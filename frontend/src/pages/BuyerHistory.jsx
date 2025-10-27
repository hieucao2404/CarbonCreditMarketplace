import React, { useState } from "react";
import SidebarBuyer from "../components/BuyerSidebar";
import Header from "../components/BuyerHeader";
import { ShoppingCart, Download } from "lucide-react";

export default function BuyerHistory() {
  const [activeTab, setActiveTab] = useState("purchase");

  const transactions = [
    {
      id: 1,
      buyer: "Nguyễn Văn A",
      amount: 25,
      date: "2024-12-25",
      price: "625,000 VND",
      status: "Hoàn thành",
    },
    {
      id: 2,
      buyer: "Trần Thị B",
      amount: 50,
      date: "2024-12-23",
      price: "1,225,000 VND",
      status: "Hoàn thành",
    },
    {
      id: 3,
      buyer: "Lê Văn C",
      amount: 30,
      date: "2024-12-20",
      price: "780,000 VND",
      status: "Đang xử lý",
    },
  ];

  const certificates = [
    {
      id: "CERT-001",
      amount: 25,
      date: "2024-12-25",
    },
    {
      id: "CERT-002",
      amount: 50,
      date: "2024-12-23",
    },
  ];

  return (
    <div className="flex min-h-screen bg-[#F9FAFB]">
      {/* Sidebar */}
      <SidebarBuyer />

      {/* Main Content */}
      <div className="flex flex-col flex-1">
        <Header />

        <main className="p-8 w-full">
          {/* Page Title */}
          <div className="mb-6">
            <h1 className="text-2xl font-semibold text-gray-800">
              Lịch sử hoạt động
            </h1>
            <p className="text-gray-500 text-sm mt-1">
              Tổng quan về tất cả hoạt động mua tín chỉ carbon
            </p>
          </div>

          {/* Tabs */}
          <div className="bg-white rounded-2xl border border-gray-200 p-5">
            <div className="flex bg-gray-100 rounded-full text-sm font-medium text-gray-600 mb-6">
              {["purchase", "certificate", "stats"].map((tab) => (
                <button
                  key={tab}
                  onClick={() => setActiveTab(tab)}
                  className={`flex-1 text-center py-2 rounded-full transition ${
                    activeTab === tab
                      ? "bg-white text-gray-900 shadow font-semibold"
                      : "hover:text-gray-800"
                  }`}
                >
                  {tab === "purchase"
                    ? "Giao dịch mua"
                    : tab === "certificate"
                    ? "Chứng nhận"
                    : "Thống kê"}
                </button>
              ))}
            </div>

            {/* Giao dịch mua */}
            {activeTab === "purchase" && (
              <div className="space-y-3">
                {transactions.map((tx) => (
                  <div
                    key={tx.id}
                    className="flex items-center justify-between border border-gray-200 rounded-xl px-4 py-3 bg-white hover:bg-gray-50 transition"
                  >
                    <div className="flex items-center gap-3">
                      <ShoppingCart size={18} className="text-blue-500" />
                      <div>
                        <p className="text-sm font-medium text-gray-800">
                          {tx.amount} tCO₂ từ {tx.buyer}
                        </p>
                        <p className="text-xs text-gray-500">{tx.date}</p>
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
                      <p className="text-sm font-medium text-gray-800">
                        {tx.price}
                      </p>
                      <span
                        className={`text-xs font-semibold px-3 py-1 rounded-full ${
                          tx.status === "Hoàn thành"
                            ? "bg-black text-white"
                            : "bg-gray-200 text-gray-700"
                        }`}
                      >
                        {tx.status}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Chứng nhận */}
            {activeTab === "certificate" && (
              <div className="space-y-3">
                {certificates.map((cert) => (
                  <div
                    key={cert.id}
                    className="flex items-center justify-between border border-gray-200 rounded-xl px-4 py-3 bg-white hover:bg-gray-50 transition"
                  >
                    <div className="flex items-center gap-3">
                      <div className="text-green-600 font-semibold">🏅</div>
                      <div>
                        <p className="text-sm font-medium text-gray-800">
                          Chứng nhận #{cert.id} – {cert.amount} tCO₂
                        </p>
                        <p className="text-xs text-gray-500">{cert.date}</p>
                      </div>
                    </div>

                    <button className="flex items-center gap-1 text-sm font-medium text-gray-700 border border-gray-300 px-3 py-1.5 rounded-lg hover:bg-gray-100">
                      <Download size={16} /> Tải xuống
                    </button>
                  </div>
                ))}
              </div>
            )}

            {/* Thống kê */}
            {activeTab === "stats" && (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div className="border border-gray-200 rounded-xl bg-white p-5">
                  <h3 className="font-semibold text-gray-800 mb-2">
                    Tổng quan mua sắm
                  </h3>
                  <p className="text-sm text-gray-700">
                    Tổng tín chỉ đã mua:{" "}
                    <span className="font-medium text-gray-900">1250.5 tCO₂</span>
                  </p>
                  <p className="text-sm text-gray-700">
                    Tổng chi phí:{" "}
                    <span className="font-medium text-gray-900">
                      31,250,000 VND
                    </span>
                  </p>
                  <p className="text-sm text-gray-700">
                    Giá trung bình:{" "}
                    <span className="font-medium text-gray-900">
                      24,990 VND/tCO₂
                    </span>
                  </p>
                  <p className="text-sm text-gray-700">
                    Số giao dịch:{" "}
                    <span className="font-medium text-gray-900">3</span>
                  </p>
                </div>

                <div className="border border-gray-200 rounded-xl bg-white p-5">
                  <h3 className="font-semibold text-gray-800 mb-2">
                    Tác động môi trường
                  </h3>
                  <p className="text-sm text-gray-700">
                    CO₂ offset:{" "}
                    <span className="font-medium text-green-600">
                      1,250,500 kg
                    </span>
                  </p>
                  <p className="text-sm text-gray-700">
                    Mức trung hòa carbon:{" "}
                    <span className="font-medium text-green-600">85%</span>
                  </p>
                  <p className="text-sm text-gray-700">
                    Tương đương cây được trồng:{" "}
                    <span className="font-medium text-green-600">56,273 cây</span>
                  </p>
                </div>
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}
