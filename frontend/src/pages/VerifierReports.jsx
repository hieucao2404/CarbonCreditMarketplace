import React, { useState, useEffect } from "react";
import VerifierSidebar from "../components/VerifierSidebar";
import VerifierHeader from "../components/VerifierHeader";
import { FileText, Download, TrendingUp, Calendar, CheckCircle2, XCircle } from "lucide-react";
import { cvaService } from "../services/cvaService";

export default function VerifierReports() {
  const [credits, setCredits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    loadReportData();
  }, []);

  const loadReportData = async () => {
    setLoading(true);
    setError("");

    try {
      const response = await cvaService.getApprovedCredits();

      if (response.success) {
        setCredits(response.data || []);
      }
    } catch (err) {
      console.error("Error loading report data:", err);
      setError("Failed to load report data");
    } finally {
      setLoading(false);
    }
  };

  // Calculate statistics from credits data
  const calculateStatistics = () => {
    if (!credits || credits.length === 0) {
      return { totalProcessed: 0, approved: 0, rejected: 0, pending: 0 };
    }

    const stats = {
      totalProcessed: credits.length,
      approved: 0,
      rejected: 0,
      pending: 0
    };

    credits.forEach(credit => {
      const status = credit.verificationStatus || credit.status;
      
      if (["VERIFIED", "LISTED", "SOLD"].includes(status)) {
        stats.approved++;
      } else if (status === "REJECTED") {
        stats.rejected++;
      } else if (status === "PENDING" || status === "PENDING_APPROVAL") {
        stats.pending++;
      }
    });

    return stats;
  };

  const calculatedStats = calculateStatistics();

  // Generate monthly reports from credits data
  const generateMonthlyReports = () => {
    if (!credits || credits.length === 0) return [];

    const monthlyData = {};

    credits.forEach(credit => {
      const date = new Date(credit.verifiedAt || credit.createdAt);
      const monthKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
      
      if (!monthlyData[monthKey]) {
        monthlyData[monthKey] = {
          month: monthKey,
          date: date,
          approved: 0,
          rejected: 0,
          totalApproved: 0,
          totalRejected: 0
        };
      }

      const status = credit.verificationStatus || credit.status;
      const amount = credit.creditAmount || 0;

      if (["VERIFIED", "LISTED", "SOLD"].includes(status)) {
        monthlyData[monthKey].approved += 1;
        monthlyData[monthKey].totalApproved += amount;
      } else if (status === "REJECTED") {
        monthlyData[monthKey].rejected += 1;
        monthlyData[monthKey].totalRejected += amount;
      }
    });

    return Object.values(monthlyData)
      .sort((a, b) => b.date - a.date)
      .map(data => {
        const total = data.approved + data.rejected;
        const rate = total > 0 ? ((data.approved / total) * 100).toFixed(1) : "0";
        
        return {
          title: `Monthly Report - ${data.date.toLocaleDateString('en-US', { year: 'numeric', month: 'long' })}`,
          period: data.date.toLocaleDateString('en-US', { year: 'numeric', month: 'short' }),
          approved: data.totalApproved.toFixed(2),
          rejected: data.totalRejected.toFixed(2),
          approvedCount: data.approved,
          rejectedCount: data.rejected,
          rate: `${rate}%`,
          date: data.date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' }),
          rawDate: data.date
        };
      });
  };

  const reports = generateMonthlyReports();

  const downloadReport = async (report) => {
    try {
      // Prepare the report data for PDF generation
      const reportData = {
        title: report.title,
        period: report.period,
        approvedCount: report.approvedCount,
        approved: parseFloat(report.approved),
        rejectedCount: report.rejectedCount,
        rejected: parseFloat(report.rejected),
        rate: parseFloat(report.rate)
      };

      // Call the backend API to generate PDF
      const pdfBlob = await cvaService.downloadReportPdf(reportData);

      // Create download link
      const url = URL.createObjectURL(pdfBlob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `CVA_Report_${report.period.replace(/\s+/g, '_')}.pdf`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);

    } catch (err) {
      console.error("Error downloading PDF report:", err);
      alert("Failed to download PDF report. Please try again.");
    }
  };

  const generateCustomReport = (type) => {
    alert(`Generating ${type} report... (Feature coming soon)`);
  };

  if (loading) {
    return (
      <div className="flex min-h-screen w-screen bg-gray-50">
        <VerifierSidebar />
        <div className="flex flex-col flex-1">
          <VerifierHeader />
          <main className="flex-1 flex items-center justify-center">
            <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Loading reports...</p>
            </div>
          </main>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen w-screen bg-gray-50 overflow-hidden">
      <VerifierSidebar />

      <div className="flex flex-col flex-1 min-h-screen w-full">
        <VerifierHeader />

        <main className="flex-1 p-8 w-full bg-gray-50 overflow-y-auto">
          <div className="max-w-5xl mx-auto space-y-6">
            {/* Header */}
            <div>
              <h2 className="text-2xl font-bold text-gray-800">
                Verification Reports
              </h2>
              <p className="text-gray-500 text-sm mt-1">
                Comprehensive reports of carbon credit verification activities
              </p>
            </div>

            {/* Error Alert */}
            {error && (
              <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
                <p className="text-red-600 text-sm">{error}</p>
              </div>
            )}

            {/* Overall Statistics Card - CORRECTED */}
            <div className="bg-gradient-to-r from-green-50 to-blue-50 border border-green-200 rounded-2xl p-6 shadow-sm">
              <h3 className="font-semibold text-gray-800 text-lg mb-4 flex items-center gap-2">
                <TrendingUp className="w-5 h-5 text-green-600" />
                Overall Statistics
              </h3>
              <div className="grid grid-cols-4 gap-4">
                <div className="bg-white rounded-lg p-4 border border-gray-200">
                  <p className="text-sm text-gray-600">Total Processed</p>
                  <p className="text-2xl font-bold text-gray-800">{calculatedStats.totalProcessed}</p>
                </div>
                <div className="bg-white rounded-lg p-4 border border-gray-200">
                  <p className="text-sm text-gray-600">Approved</p>
                  <p className="text-2xl font-bold text-green-600">{calculatedStats.approved}</p>
                </div>
                <div className="bg-white rounded-lg p-4 border border-gray-200">
                  <p className="text-sm text-gray-600">Rejected</p>
                  <p className="text-2xl font-bold text-red-600">{calculatedStats.rejected}</p>
                </div>
                <div className="bg-white rounded-lg p-4 border border-gray-200">
                  <p className="text-sm text-gray-600">Pending</p>
                  <p className="text-2xl font-bold text-orange-600">{calculatedStats.pending}</p>
                </div>
              </div>
            </div>

            {/* Monthly Reports */}
            <div className="space-y-4">
              <h3 className="font-semibold text-gray-700 flex items-center gap-2">
                <Calendar className="w-5 h-5" />
                Monthly Reports
              </h3>

              {reports.length === 0 ? (
                <div className="bg-white border border-gray-200 rounded-2xl p-12 text-center">
                  <FileText className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                  <p className="text-gray-500">No reports available yet</p>
                  <p className="text-sm text-gray-400 mt-2">Reports will be generated as you process credits</p>
                </div>
              ) : (
                reports.map((report, i) => (
                  <div
                    key={i}
                    className="bg-white border border-gray-200 rounded-2xl p-6 flex justify-between items-center shadow-sm hover:shadow-md transition"
                  >
                    {/* Left section */}
                    <div className="flex items-start gap-4">
                      <div className="bg-blue-100 p-3 rounded-xl">
                        <FileText className="w-6 h-6 text-blue-600" />
                      </div>
                      <div>
                        <h3 className="font-semibold text-gray-800 text-base">
                          {report.title}
                        </h3>
                        <p className="text-sm text-gray-500">
                          Report Period: {report.period}
                        </p>

                        <div className="mt-3 space-y-1 text-sm">
                          <p className="flex items-center gap-2">
                            <CheckCircle2 className="w-4 h-4 text-green-600" />
                            <span className="text-gray-600">Credits Approved:</span>
                            <span className="text-green-600 font-medium">
                              {report.approvedCount} ({report.approved} tCO₂)
                            </span>
                          </p>
                          <p className="flex items-center gap-2">
                            <XCircle className="w-4 h-4 text-red-500" />
                            <span className="text-gray-600">Credits Rejected:</span>
                            <span className="text-red-500 font-medium">
                              {report.rejectedCount} ({report.rejected} tCO₂)
                            </span>
                          </p>
                          <p className="text-gray-700 font-medium">
                            Approval Rate: {report.rate}
                          </p>
                        </div>
                      </div>
                    </div>

                    {/* Right section */}
                    <div className="text-right flex flex-col items-end gap-2">
                      <span className="bg-green-600 text-white text-xs px-3 py-1 rounded-md font-medium">
                        Completed
                      </span>
                      <button
                        onClick={() => downloadReport(report)}
                        className="flex items-center gap-2 border border-gray-300 text-gray-800 text-sm font-medium rounded-lg px-4 py-2 hover:bg-gray-100 transition"
                      >
                        <Download className="w-4 h-4" />
                        Download PDF
                      </button>
                      <p className="text-xs text-gray-500">{report.date}</p>
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Generate New Report Section */}
            <div className="bg-white border border-gray-200 rounded-2xl p-6 mt-6 text-center shadow-sm">
              <h3 className="text-gray-800 font-semibold mb-4 text-lg">
                Generate New Report
              </h3>
              <div className="flex justify-center gap-4">
                <button
                  onClick={() => generateCustomReport("Current Month")}
                  className="bg-green-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-green-700 transition shadow-sm"
                >
                  Current Month Report
                </button>
                <button
                  onClick={() => generateCustomReport("Current Quarter")}
                  className="border border-gray-300 px-6 py-3 rounded-lg font-medium hover:bg-gray-50 transition"
                >
                  Quarterly Report
                </button>
                <button
                  onClick={() => generateCustomReport("Custom Range")}
                  className="border border-gray-300 px-6 py-3 rounded-lg font-medium hover:bg-gray-50 transition"
                >
                  Custom Range
                </button>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
