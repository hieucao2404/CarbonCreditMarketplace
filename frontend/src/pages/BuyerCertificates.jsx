import React from "react";
import SidebarBuyer from "../components/BuyerSidebar";
import Header from "../components/BuyerHeader";
import { FileDown, Award } from "lucide-react";

export default function BuyerCertificates() {
  const certificates = [
    {
      id: "#CERT-001",
      project: "EV Carbon Credits - Hanoi Region",
      amount: 25,
      co2Offset: "25,000 kg CO₂",
      issueDate: "2024-12-25",
      expiryDate: "2025-12-25",
    },
    {
      id: "#CERT-002",
      project: "EV Carbon Credits - Ho Chi Minh Region",
      amount: 50,
      co2Offset: "50,000 kg CO₂",
      issueDate: "2024-12-23",
      expiryDate: "2025-12-23",
    },
  ];

  return (
    <div className="flex min-h-screen bg-[#F9FAFB]">
      <SidebarBuyer />
      <div className="flex flex-col flex-1">
        <Header />

        <main className="p-8 w-full">
          {/* Header */}
          <div className="mb-6">
            <h1 className="text-2xl font-semibold text-gray-800">
              Chứng nhận tín chỉ carbon
            </h1>
            <p className="text-gray-500 text-sm mt-1">
              Chứng nhận giảm phát thải CO₂ cho báo cáo bền vững
            </p>
          </div>

          {/* Certificates list */}
          <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
            {certificates.map((cert, index) => (
              <div
                key={index}
                className="flex items-center justify-between border border-gray-200 rounded-lg bg-gray-50 p-4 hover:bg-gray-100 transition"
              >
                {/* Left content */}
                <div className="flex items-start gap-4">
                  <div className="bg-green-100 p-3 rounded-lg">
                    <Award className="text-green-600 w-6 h-6" />
                  </div>

                  <div>
                    <h2 className="text-base font-semibold text-gray-800">
                      Chứng nhận {cert.id}
                    </h2>
                    <p className="text-sm text-gray-500">{cert.project}</p>

                    <div className="mt-2 grid grid-cols-2 gap-x-8 gap-y-1 text-sm">
                      <p className="text-gray-600">
                        <span className="font-medium">Số lượng tín chỉ:</span>{" "}
                        {cert.amount} tCO₂
                      </p>
                      <p className="text-gray-600">
                        <span className="font-medium">CO₂ offset:</span>{" "}
                        {cert.co2Offset}
                      </p>
                      <p className="text-gray-600">
                        <span className="font-medium">Ngày phát hành:</span>{" "}
                        {cert.issueDate}
                      </p>
                      <p className="text-gray-600">
                        <span className="font-medium">Có hiệu lực đến:</span>{" "}
                        {cert.expiryDate}
                      </p>
                    </div>
                  </div>
                </div>

                {/* Actions */}
                <div className="flex flex-col items-end gap-2">
                  <button className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm hover:bg-gray-100 flex items-center gap-1">
                    <FileDown size={14} />
                    Tải PDF
                  </button>
                  <button className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm hover:bg-gray-100">
                    Xem chi tiết
                  </button>
                </div>
              </div>
            ))}
          </div>
        </main>
      </div>
    </div>
  );
}
