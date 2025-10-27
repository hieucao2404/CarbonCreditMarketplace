import React from "react";
import Sidebar from "../components/Sidebar";
import Header from "../components/Header";

export default function Transactions() {
  const transactions = [
    {
      id: "TX001",
      company: "Công ty ABC Ltd",
      amount: 25,
      date: "2024-12-27",
      price: 24800,
      total: 620000,
      status: "Hoàn thành",
    },
    {
      id: "TX002",
      company: "Green Corp",
      amount: 50,
      date: "2024-12-25",
      price: 25500,
      total: 1275000,
      status: "Hoàn thành",
    },
    {
      id: "TX003",
      company: "EcoViet Co.",
      amount: 30,
      date: "2024-12-23",
      price: 25000,
      total: 750000,
      status: "Đang xử lý",
    },
    {
      id: "TX004",
      company: "CleanTech Inc",
      amount: 75,
      date: "2024-12-20",
      price: 26200,
      total: 1965000,
      status: "Hoàn thành",
    },
  ];

  return (
    <div className="flex min-h-screen w-screen bg-[#F9FAFB] overflow-hidden">
      <Sidebar />

      <div className="flex flex-col flex-1 min-h-screen w-full">
        <Header />

        <main className="flex-1 p-8 bg-[#F9FAFB] overflow-y-auto">
          <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6">
            <h2 className="text-xl font-semibold text-gray-800 mb-2">
              Lịch sử giao dịch
            </h2>
            <p className="text-gray-500 text-sm mb-6">
              Theo dõi và quản lý các giao dịch bán tín chỉ carbon
            </p>

            {/* Danh sách giao dịch */}
            <div className="space-y-4">
              {transactions.map((t) => (
                <div
                  key={t.id}
                  className="flex items-center justify-between border border-gray-200 rounded-xl px-5 py-4 bg-white hover:bg-gray-50 transition"
                >
                  {/* Bên trái */}
                  <div className="flex flex-col">
                    <span className="font-semibold text-gray-800">{t.id}</span>
                    <span className="text-sm text-gray-500">{t.company}</span>
                  </div>

                  {/* Giữa */}
                  <div className="text-right">
                    <p className="text-gray-800 font-medium">
                      {t.amount} tCO₂
                    </p>
                    <p className="text-sm text-gray-400">{t.date}</p>
                  </div>

                  {/* Giá và tổng */}
                  <div className="text-right">
                    <p className="text-sm text-gray-700">
                      {t.price.toLocaleString()} VNĐ/tCO₂
                    </p>
                    <p className="text-xs text-gray-400">
                      Tổng: {t.total.toLocaleString()} VNĐ
                    </p>
                  </div>

                  {/* Trạng thái + nút */}
                  <div className="flex items-center gap-3">
                    <span
                      className={`px-3 py-1 text-xs font-medium rounded-full ${
                        t.status === "Hoàn thành"
                          ? "bg-gray-900 text-white"
                          : "bg-yellow-100 text-yellow-700 border border-gray-200"
                      }`}
                    >
                      {t.status}
                    </span>
                    <button className="px-3 py-1 text-xs font-medium border border-gray-200 rounded-lg hover:bg-gray-100 transition">
                      Chi tiết
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
