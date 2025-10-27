import React from "react";
import SidebarBuyer from "../components/BuyerSidebar";
import Header from "../components/BuyerHeader";
import { Search, MapPin, Calendar, CheckCircle2, Clock } from "lucide-react";

export default function BuyerMarket() {
  const credits = [
    {
      id: 1,
      amount: 50,
      seller: "Nguyễn Văn A",
      source: "Tesla Model 3 - 2023",
      location: "Hà Nội",
      price: "25,500 VNĐ/tCO₂",
      total: "1,275,000 VNĐ",
      type: "Giá cố định",
      date: "2024-12-28",
      image: "/images/carbon1.png",
    },
    {
      id: 2,
      amount: 75,
      seller: "Trần Thị B",
      source: "BYD Seal - 2024",
      location: "TP.HCM",
      price: "24,800 VNĐ/tCO₂",
      total: "1,860,000 VNĐ",
      type: "Đấu giá",
      endDate: "2024-12-30",
      date: "2024-12-27",
      image: "/images/carbon2.png",
    },
    {
      id: 3,
      amount: 30,
      seller: "Lê Văn C",
      source: "Tesla Model Y - 2023",
      location: "Đà Nẵng",
      price: "26,200 VNĐ/tCO₂",
      total: "786,000 VNĐ",
      type: "Giá cố định",
      date: "2024-12-26",
      image: "/images/carbon3.png",
    },
    {
      id: 4,
      amount: 100,
      seller: "Phạm Thị D",
      source: "VinFast VF8 - 2024",
      location: "Hà Nội",
      price: "25,000 VNĐ/tCO₂",
      total: "2,500,000 VNĐ",
      type: "Đấu giá",
      endDate: "2024-12-29",
      date: "2024-12-25",
      image: "/images/carbon4.png",
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
              Thị trường tín chỉ carbon
            </h1>
            <p className="text-gray-500 text-sm mt-1">
              Tìm kiếm và mua tín chỉ carbon từ chủ sở hữu xe điện
            </p>
          </div>

          {/* Search & Filters */}
          <div className="flex items-center gap-3 mb-6">
            <div className="flex items-center bg-gray-100 rounded-lg px-3 py-2 w-full">
              <Search size={18} className="text-gray-400" />
              <input
                type="text"
                placeholder="Tìm theo người bán, loại xe..."
                className="bg-transparent flex-1 px-2 text-sm outline-none"
              />
            </div>
            <select className="border border-gray-300 rounded-lg px-3 py-2 text-sm text-gray-700">
              <option>Tất cả khu vực</option>
              <option>Hà Nội</option>
              <option>TP.HCM</option>
              <option>Đà Nẵng</option>
            </select>
            <select className="border border-gray-300 rounded-lg px-3 py-2 text-sm text-gray-700">
              <option>Tất cả mức giá</option>
              <option>Dưới 25,000 VNĐ</option>
              <option>25,000–26,000 VNĐ</option>
              <option>Trên 26,000 VNĐ</option>
            </select>
          </div>

          {/* List */}
          <div className="space-y-4">
            {credits.map((credit) => (
              <div
                key={credit.id}
                className="flex items-center justify-between bg-white border border-gray-200 rounded-xl p-4 hover:shadow-sm transition-all"
              >
                <div className="flex items-center gap-4">
                  <img
                    src={credit.image}
                    alt="carbon"
                    className="w-16 h-16 rounded-lg object-cover"
                  />
                  <div>
                    <h2 className="text-lg font-semibold text-gray-800 flex items-center gap-2">
                      {credit.amount} tCO₂
                      <CheckCircle2 className="text-green-600 w-4 h-4" />
                      <span
                        className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                          credit.type === "Giá cố định"
                            ? "bg-gray-800 text-white"
                            : "bg-blue-100 text-blue-700"
                        }`}
                      >
                        {credit.type}
                      </span>
                    </h2>
                    <p className="text-sm text-gray-600">
                      Người bán: {credit.seller}
                    </p>
                    <p className="text-sm text-gray-600">Nguồn: {credit.source}</p>
                    <div className="flex items-center text-sm text-gray-500 gap-4 mt-1">
                      <span className="flex items-center gap-1">
                        <MapPin size={14} />
                        {credit.location}
                      </span>
                      <span className="flex items-center gap-1">
                        <Calendar size={14} /> {credit.date}
                      </span>
                      {credit.endDate && (
                        <span className="flex items-center gap-1 text-red-500">
                          <Clock size={14} /> Kết thúc: {credit.endDate}
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Right Side */}
                <div className="text-right">
                  <p className="text-gray-800 font-semibold text-sm">
                    {credit.price}
                  </p>
                  <p className="text-xs text-gray-500 mb-3">
                    Tổng: {credit.total}
                  </p>
                  <div className="flex gap-2 justify-end">
                    <button className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm hover:bg-gray-100">
                      Chi tiết
                    </button>
                    <button
                      className={`px-3 py-1.5 rounded-lg text-sm text-white ${
                        credit.type === "Giá cố định"
                          ? "bg-gray-900 hover:bg-gray-800"
                          : "bg-blue-600 hover:bg-blue-700"
                      }`}
                    >
                      {credit.type === "Giá cố định" ? "Mua ngay" : "Đấu giá"}
                    </button>
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
