package com.carboncredit.service;

import com.carboncredit.entity.Certificate;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

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
    private static final Font FONT_BUYER_NAME = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 32, COLOR_SECONDARY_BLUE);
    private static final Font FONT_AMOUNT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 36, COLOR_PRIMARY_GREEN);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY);
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
                FONT_BODY
            );
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
                FONT_FOOTER
            );
            PdfPCell dateCell = new PdfPCell(pDate);
            dateCell.setBorder(Rectangle.NO_BORDER);
            dateCell.setHorizontalAlignment(Element.ALIGN_LEFT);

            // Certificate ID Cell
            Paragraph pCode = new Paragraph(
                "Certificate ID:\n" + certificate.getCertificateCode(), 
                FONT_FOOTER
            );
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
}