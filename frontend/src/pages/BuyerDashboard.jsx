import React from "react";
import SidebarBuyer from "../components/BuyerSidebar";
import Header from "../components/BuyerHeader";
import { Leaf, ShoppingCart } from "lucide-react";

export default function BuyerDashboard() {
  const recentPurchases = [
    { amount: 25, buyer: "Nguyễn Văn A", price: "625,000 VNĐ", status: "Hoàn thành" },
    { amount: 50, buyer: "Trần Thị B", price: "1,225,000 VNĐ", status: "Hoàn thành" },
    { amount: 30, buyer: "Lê Văn C", price: "780,000 VNĐ", status: "Đang xử lý" },
  ];

  const availableCredits = [
    { amount: 50, location: "Hà Nội", price: "25,500 VNĐ/tCO₂", type: "Giá cố định" },
    { amount: 75, location: "TP.HCM", price: "24,800 VNĐ/tCO₂", type: "Đấu giá" },
    { amount: 30, location: "Đà Nẵng", price: "26,200 VNĐ/tCO₂", type: "Giá cố định" },
    { amount: 100, location: "Hà Nội", price: "25,000 VNĐ/tCO₂", type: "Đấu giá" },
  ];

  return (
    <div className="flex min-h-screen w-screen bg-[#F9FAFB] overflow-hidden">
      <SidebarBuyer /> {/* ✅ sidebar riêng cho buyer */}

      <div className="flex flex-col flex-1 min-h-screen w-full">
        <Header />

        <main className="flex-1 p-8 w-full bg-[#F9FAFB] overflow-y-auto">
          {/* Header section */}
          <div className="mb-8">
            <h1 className="text-2xl font-semibold text-gray-800">
              Carbon Credit Exchange
            </h1>
            <button className="mt-2 bg-green-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-green-700">
              Người mua tín chỉ
            </button>
          </div>

          {/* 4 Stat cards */}
          <div className="grid grid-cols-4 gap-6 mb-8">
            <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
              <h2 className="text-gray-500 text-sm">Tín chỉ đã mua</h2>
              <p className="text-2xl font-semibold text-gray-800 mt-1">
                1250.5 tCO₂
              </p>
              <p className="text-sm text-green-600">+89.3 tCO₂ tháng này</p>
            </div>

            <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
              <h2 className="text-gray-500 text-sm">Tổng chi phí</h2>
              <p className="text-2xl font-semibold text-gray-800 mt-1">
                31,250,000 VNĐ
              </p>
              <p className="text-sm text-gray-500">Trung bình 24,990 VNĐ/tCO₂</p>
            </div>

            <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
              <h2 className="text-gray-500 text-sm">Mức carbon trung hòa</h2>
              <p className="text-2xl font-semibold text-gray-800 mt-1">85%</p>
              <p className="text-sm text-gray-500">
                Đạt mục tiêu trung hòa carbon năm 2024
              </p>
            </div>

            <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
              <h2 className="text-gray-500 text-sm">Chứng nhận</h2>
              <p className="text-2xl font-semibold text-gray-800 mt-1">2</p>
              <p className="text-sm text-gray-500">
                Chứng nhận carbon offset hợp lệ
              </p>
            </div>
          </div>

          {/* 2 columns layout */}
          <div className="grid grid-cols-2 gap-6">
            {/* Giao dịch gần đây */}
            <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
              <h3 className="text-lg font-semibold text-gray-800 mb-2">
                Giao dịch gần đây
              </h3>
              <p className="text-sm text-gray-500 mb-4">
                Lịch sử mua tín chỉ carbon gần nhất
              </p>

              <div className="space-y-3">
                {recentPurchases.map((item, i) => (
                  <div
                    key={i}
                    className="flex justify-between items-center border-b border-gray-200 pb-2"
                  >
                    <div className="flex items-center gap-2">
                      <ShoppingCart className="text-green-600 w-4 h-4" />
                      <p className="text-gray-700 text-sm">
                        {item.amount} tCO₂ - {item.buyer}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-sm font-medium text-gray-700">
                        {item.price}
                      </p>
                      <span
                        className={`text-xs px-2 py-0.5 rounded-full ${
                          item.status === "Hoàn thành"
                            ? "bg-green-100 text-green-700"
                            : "bg-yellow-100 text-yellow-700"
                        }`}
                      >
                        {item.status}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Tín chỉ carbon có sẵn */}
            <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
              <h3 className="text-lg font-semibold text-gray-800 mb-2">
                Tín chỉ carbon có sẵn
              </h3>
              <p className="text-sm text-gray-500 mb-4">
                Các tín chỉ mới nhất trên thị trường
              </p>

              <div className="space-y-3">
                {availableCredits.map((credit, i) => (
                  <div
                    key={i}
                    className="flex justify-between items-center border-b border-gray-200 pb-2"
                  >
                    <div className="flex items-center gap-2">
                      <Leaf className="text-green-600 w-4 h-4" />
                      <p className="text-gray-700 text-sm">
                        {credit.amount} tCO₂ • {credit.location}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-sm font-medium text-gray-700">
                        {credit.price}
                      </p>
                      <span className="text-xs bg-gray-800 text-white px-2 py-0.5 rounded-full">
                        {credit.type}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
