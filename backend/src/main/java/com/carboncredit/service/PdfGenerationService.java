package com.carboncredit.service;

import com.carboncredit.dto.MonthlyReportDTO;
import com.carboncredit.entity.Certificate;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter; // Correct import

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.awt.Color; // Import AWT Color

@Service
public class PdfGenerationService {
    private static final Logger log = LoggerFactory.getLogger(PdfGenerationService.class);

    // --- DEFINE COLORS ---
    private static final Color COLOR_PRIMARY_GREEN = new Color(0, 102, 102);
    private static final Color COLOR_SECONDARY_BLUE = new Color(0, 51, 102);
    private static final Color COLOR_BORDER_GRAY = new Color(200, 200, 200);

    // --- DEFINE FONTS ---
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, COLOR_PRIMARY_GREEN);
    private static final Font FONT_HEADER = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.GRAY);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, COLOR_SECONDARY_BLUE);
    private static final Font FONT_BUYER_NAME = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 32,
            COLOR_SECONDARY_BLUE);
    private static final Font FONT_AMOUNT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 36, COLOR_PRIMARY_GREEN);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY);
    private static final Font FONT_BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
    private static final Font FONT_FOOTER = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);

    public byte[] generateCertificatePdf(Certificate certificate) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate()); // Landscape mode
            PdfWriter.getInstance(document, baos);

            document.open();

            // --- 1. CREATE MAIN TABLE (for border) ---
            PdfPTable mainTable = new PdfPTable(1);
            mainTable.setWidthPercentage(95); // 95% of page width

            PdfPCell mainCell = new PdfPCell();
            mainCell.setBorderColor(COLOR_BORDER_GRAY);
            mainCell.setBorderWidth(2);
            mainCell.setPadding(20);

            // --- 2. ADD LOGO (as text) ---
            Paragraph logo = new Paragraph("Carbon Credit Marketplace (CCM)", FONT_HEADER);
            logo.setAlignment(Element.ALIGN_LEFT);
            mainCell.addElement(logo);

            // --- 3. ADD TITLE ---
            Paragraph title = new Paragraph("Certificate of Carbon Credit Retirement", FONT_TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(15);
            mainCell.addElement(title);

            // --- 4. ADD "ISSUED TO" ---
            Paragraph pIssuedTo = new Paragraph("This certificate is issued to:", FONT_BODY);
            pIssuedTo.setAlignment(Element.ALIGN_CENTER);
            pIssuedTo.setSpacingBefore(30);
            mainCell.addElement(pIssuedTo);

            // --- 5. ADD BUYER'S NAME (THE FIX) ---
            // We now use getUsername() instead of getFullName()
            String buyerName = (certificate.getBuyer() != null)
                    ? certificate.getBuyer().getUsername() // <-- THIS IS THE FIX
                    : "N/A";
            Paragraph pBuyerName = new Paragraph(buyerName, FONT_BUYER_NAME);
            pBuyerName.setAlignment(Element.ALIGN_CENTER);
            mainCell.addElement(pBuyerName);

            // --- 6. ADD "FOR THE RETIREMENT OF" ---
            Paragraph pRetirement = new Paragraph("For the successful retirement of:", FONT_BODY);
            pRetirement.setAlignment(Element.ALIGN_CENTER);
            pRetirement.setSpacingBefore(30);
            mainCell.addElement(pRetirement);

            // --- 7. ADD AMOUNT ---
            Paragraph pAmount = new Paragraph(certificate.getCo2ReducedKg() + " tCO₂e", FONT_AMOUNT);
            pAmount.setAlignment(Element.ALIGN_CENTER);
            mainCell.addElement(pAmount);

            Paragraph pReason = new Paragraph(
                    "This retirement corresponds to verified carbon reductions from project " +
                            certificate.getCredit().getId().toString().substring(0, 8),
                    FONT_BODY);
            pReason.setAlignment(Element.ALIGN_CENTER);
            pReason.setSpacingBefore(10);
            mainCell.addElement(pReason);

            // --- 8. ADD FOOTER (using a nested table for layout) ---
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);
            footerTable.setSpacingBefore(40);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

            // Issue Date Cell
            Paragraph pDate = new Paragraph(
                    "Issue Date:\n" + certificate.getIssueDate().format(formatter),
                    FONT_FOOTER);
            PdfPCell dateCell = new PdfPCell(pDate);
            dateCell.setBorder(Rectangle.NO_BORDER);
            dateCell.setHorizontalAlignment(Element.ALIGN_LEFT);

            // Certificate ID Cell
            Paragraph pCode = new Paragraph(
                    "Certificate ID:\n" + certificate.getCertificateCode(),
                    FONT_FOOTER);
            PdfPCell codeCell = new PdfPCell(pCode);
            codeCell.setBorder(Rectangle.NO_BORDER);
            codeCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            footerTable.addCell(dateCell);
            footerTable.addCell(codeCell);
            mainCell.addElement(footerTable);

            // --- Add the main cell to the table ---
            mainTable.addCell(mainCell);

            // --- Add the table to the document ---
            document.add(mainTable);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating PDF certificate: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    public byte[] generateMonthlyReportPdf(MonthlyReportDTO report) {
        ByteArrayOutputStream baos = null;
        Document document = null;
        
        try {
            // Null check for report
            if (report == null) {
                log.error("❌ Report DTO is null");
                throw new RuntimeException("Report data cannot be null");
            }

            log.info("📄 Starting PDF generation for report: {}", report.getPeriod());

            baos = new ByteArrayOutputStream();
            document = new Document(PageSize.A4); // Portrait mode
            PdfWriter.getInstance(document, baos);
            document.open();

            log.debug("✓ Document opened successfully");

            // --- 1. Header ---
            Paragraph logo = new Paragraph("Carbon Credit Marketplace (CCM)", FONT_HEADER);
            logo.setAlignment(Element.ALIGN_LEFT);
            document.add(logo);

            Paragraph title = new Paragraph("Carbon Verification Report", FONT_TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(15);
            document.add(title);

            log.debug("✓ Title section added");

            // --- 2. Report Title ---
            String reportTitle = report.getTitle() != null && !report.getTitle().isEmpty() 
                ? report.getTitle() 
                : "Monthly Report";
            Paragraph pSubtitle = new Paragraph(reportTitle, FONT_SUBTITLE);
            pSubtitle.setAlignment(Element.ALIGN_CENTER);
            pSubtitle.setSpacingAfter(10);
            document.add(pSubtitle);

            // --- 3. Summary Info ---
            String period = report.getPeriod() != null && !report.getPeriod().isEmpty() 
                ? report.getPeriod() 
                : "N/A";
            Paragraph pPeriod = new Paragraph("Report Period: " + period, FONT_BODY);
            document.add(pPeriod);

            Paragraph pGenerated = new Paragraph(
                    "Generated On: " + java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                    FONT_BODY);
            pGenerated.setSpacingAfter(25);
            document.add(pGenerated);

            log.debug("✓ Summary section added");

            // --- 4. Statistics Table ---
            PdfPTable table = new PdfPTable(2); // 2 columns
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 1, 2 }); // Col 1 is 1/3, Col 2 is 2/3

            try {
                // Safe format approved
                String approvedStr = (report.getApprovedCount() > 0 ? report.getApprovedCount() : 0) + " (" +
                        (report.getApproved() != null && !report.getApproved().isEmpty() 
                            ? report.getApproved() 
                            : "0.00") + 
                        " tCO₂)";
                addStatRow(table, "Total Approved:", approvedStr);

                // Safe format rejected
                String rejectedStr = (report.getRejectedCount() > 0 ? report.getRejectedCount() : 0) + " (" +
                        (report.getRejected() != null && !report.getRejected().isEmpty() 
                            ? report.getRejected() 
                            : "0.00") + 
                        " tCO₂)";
                addStatRow(table, "Total Rejected:", rejectedStr);

                // Safe format rate
                String rateStr = (report.getRate() != null && !report.getRate().isEmpty() 
                    ? report.getRate() 
                    : "0.00") + "%";
                addStatRow(table, "Approval Rate:", rateStr);

                log.debug("✓ Statistics table created");
            } catch (Exception e) {
                log.error("❌ Error creating statistics table: {}", e.getMessage(), e);
                throw e;
            }

            document.add(table);

            // --- 5. Footer ---
            Paragraph pFooter = new Paragraph("End of Report", FONT_FOOTER);
            pFooter.setAlignment(Element.ALIGN_CENTER);
            pFooter.setSpacingBefore(50);
            document.add(pFooter);

            document.close();
            document = null;

            log.info("✅ PDF report generated successfully for period: {}", period);
            return baos.toByteArray();

        } catch (DocumentException de) {
            log.error("❌ DocumentException during PDF generation: {}", de.getMessage(), de);
            throw new RuntimeException("Error generating PDF document: " + de.getMessage(), de);
        } catch (Exception e) {
            log.error("❌ Unexpected error generating PDF report: {}", e.getMessage(), e);
            e.printStackTrace();
            throw new RuntimeException("Error generating PDF report: " + e.getMessage(), e);
        } finally {
            // Ensure document is closed
            if (document != null) {
                document.close();
            }
            // Close stream
            if (baos != null) {
                try {
                    baos.close();
                } catch (Exception e) {
                    log.warn("⚠ Error closing output stream: {}", e.getMessage());
                }
            }
        }
    }

    //
    // --- HELPER METHOD MOVED HERE (AND CORRECTED) ---
    //
    private void addStatRow(PdfPTable table, String label, String value) {
        // Label Cell (bold)
        PdfPCell labelCell = new PdfPCell(new Paragraph(label, FONT_BODY_BOLD));
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setBorderColor(COLOR_BORDER_GRAY);
        labelCell.setPadding(10);
        table.addCell(labelCell);

        // Value Cell (normal)
        PdfPCell valueCell = new PdfPCell(new Paragraph(value, FONT_BODY));
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setBorderColor(COLOR_BORDER_GRAY);
        valueCell.setPadding(10);
        table.addCell(valueCell);
    }
}