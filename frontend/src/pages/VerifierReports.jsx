import React from "react";
import VerifierSidebar from "../components/VerifierSidebar";
import VerifierHeader from "../components/VerifierHeader";
import { FileText } from "lucide-react";

export default function VerifierReports() {
  const reports = [
    {
      title: "Báo cáo kiểm toán Q4 2024",
      period: "Q4 2024",
      approved: 1247,
      rejected: 23,
      rate: "98%",
      date: "2024-12-28",
    },
    {
      title: "Báo cáo kiểm toán Q3 2024",
      period: "Q3 2024",
      approved: 1189,
      rejected: 31,
      rate: "97%",
      date: "2024-09-30",
    },
  ];

  return (
    <div className="flex min-h-screen w-screen bg-gray-50 overflow-hidden">
      {/* Sidebar */}
      <VerifierSidebar />

      {/* Main Content */}
      <div className="flex flex-col flex-1 min-h-screen w-full">
        {/* Header */}
        <VerifierHeader />

        {/* Main Section */}
        <main className="flex-1 p-8 w-full bg-gray-50 overflow-y-auto">
          <div className="max-w-5xl mx-auto space-y-6">
            {/* Title */}
            <div>
              <h2 className="text-xl font-semibold text-gray-800">
                Báo cáo kiểm toán
              </h2>
              <p className="text-gray-500 text-sm mt-1">
                Báo cáo tổng hợp hoạt động xác minh và phát hành tín chỉ carbon
              </p>
            </div>

            {/* Report Cards */}
            <div className="space-y-4">
              {reports.map((report, i) => (
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
                        Kỳ báo cáo: {report.period}
                      </p>

                      <div className="mt-3 space-y-1 text-sm">
                        <p>
                          <span className="text-gray-600">
                            Tín chỉ đã duyệt:
                          </span>{" "}
                          <span className="text-green-600 font-medium">
                            {report.approved} tCO₂
                          </span>
                        </p>
                        <p>
                          <span className="text-gray-600">
                            Tín chỉ từ chối:
                          </span>{" "}
                          <span className="text-red-500 font-medium">
                            {report.rejected} tCO₂
                          </span>
                        </p>
                        <p className="text-gray-700">
                          <span className="font-medium">Tỷ lệ duyệt:</span>{" "}
                          {report.rate}
                        </p>
                      </div>
                    </div>
                  </div>

                  {/* Right section */}
                  <div className="text-right flex flex-col items-end gap-2">
                    <span className="bg-gray-900 text-white text-xs px-3 py-1 rounded-md font-medium">
                      Hoàn thành
                    </span>
                    <button className="flex items-center gap-2 border border-gray-300 text-gray-800 text-sm font-medium rounded-lg px-4 py-2 hover:bg-gray-100 transition">
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        fill="none"
                        viewBox="0 0 24 24"
                        strokeWidth="2"
                        stroke="currentColor"
                        className="w-4 h-4"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          d="M4 4v16h16V4M4 12h16"
                        />
                      </svg>
                      Tải báo cáo
                    </button>
                    <p className="text-xs text-gray-500">{report.date}</p>
                  </div>
                </div>
              ))}
            </div>

            {/* New Report Section */}
            <div className="bg-white border border-gray-200 rounded-2xl p-6 mt-6 text-center shadow-sm">
              <h3 className="text-gray-800 font-medium mb-4">
                Tạo báo cáo mới
              </h3>
              <div className="flex justify-center gap-4">
                <button className="bg-black text-white px-5 py-3 rounded-lg font-medium hover:bg-gray-900 transition">
                  Báo cáo tháng này
                </button>
                <button className="border border-gray-300 px-5 py-3 rounded-lg font-medium hover:bg-gray-100 transition">
                  Báo cáo quý này
                </button>
                <button className="border border-gray-300 px-5 py-3 rounded-lg font-medium hover:bg-gray-100 transition">
                  Báo cáo tùy chỉnh
                </button>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
