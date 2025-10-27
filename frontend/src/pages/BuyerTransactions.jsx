import React from "react";
import SidebarBuyer from "../components/BuyerSidebar";
import Header from "../components/BuyerHeader";
import { Calendar, Download, CheckCircle2, Clock } from "lucide-react";

export default function BuyerTransactions() {
  const purchases = [
    {
      id: "PH001",
      amount: 25,
      price: 25000,
      seller: "Nguyễn Văn A",
      date: "2024-12-25",
      total: "625,000 VNĐ",
      status: "Hoàn thành",
      downloadable: true,
    },
    {
      id: "PH002",
      amount: 50,
      price: 24500,
      seller: "Trần Thị B",
      date: "2024-12-23",
      total: "1,225,000 VNĐ",
      status: "Hoàn thành",
      downloadable: true,
    },
    {
      id: "PH003",
      amount: 30,
      price: 26000,
      seller: "Lê Văn C",
      date: "2024-12-20",
      total: "780,000 VNĐ",
      status: "Đang xử lý",
      downloadable: false,
    },
  ];

  return (
    <div className="flex min-h-screen bg-[#F9FAFB]">
      <SidebarBuyer />
      <div className="flex flex-col flex-1">
        <Header />

        <main className="p-8 w-full">
          {/* Title */}
          <div className="mb-6">
            <h1 className="text-2xl font-semibold text-gray-800">
              Lịch sử mua tín chỉ carbon
            </h1>
            <p className="text-gray-500 text-sm mt-1">
              Quản lý và theo dõi các giao dịch mua của bạn
            </p>
          </div>

          {/* List of purchases */}
          <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
            {purchases.map((item) => (
              <div
                key={item.id}
                className="flex items-center justify-between bg-gray-50 border border-gray-200 rounded-lg p-4 hover:bg-gray-100 transition"
              >
                {/* Left Section */}
                <div>
                  <h2 className="text-base font-semibold text-gray-800">
                    {item.id}
                  </h2>
                  <p className="text-sm text-gray-600">
                    {item.amount} tCO₂ × {item.price.toLocaleString()} VNĐ
                  </p>
                  <p className="text-sm text-gray-500">
                    Người bán: {item.seller}
                  </p>
                  <div className="flex items-center gap-1 text-sm text-gray-400 mt-1">
                    <Calendar size={14} /> {item.date}
                  </div>
                </div>

                {/* Right Section */}
                <div className="text-right">
                  <p className="text-sm font-medium text-gray-800">
                    {item.total}
                  </p>
                  <div className="flex items-center justify-end gap-2 mt-2">
                    <span
                      className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                        item.status === "Hoàn thành"
                          ? "bg-gray-900 text-white"
                          : "bg-yellow-100 text-yellow-700"
                      }`}
                    >
                      {item.status}
                    </span>

                    <button className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm hover:bg-gray-100">
                      Chi tiết
                    </button>

                    {item.downloadable && (
                      <button className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm hover:bg-gray-100 flex items-center gap-1">
                        <Download size={14} />
                        Chứng nhận
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </main>
      </div>
    </div>
  );
}
