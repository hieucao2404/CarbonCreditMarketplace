import React, { useState } from "react";
import Sidebar from "../components/Sidebar";
import Header from "../components/Header";
import { Lightbulb } from "lucide-react";

export default function CarbonListing() {
  const [activeTab, setActiveTab] = useState("create");
  const [listingType, setListingType] = useState("fixed");

  const listings = [
    {
      amount: 50,
      price: 25.5,
      date: "2024-12-25",
      total: 1275,
      status: "Đang bán",
      type: "Giá cố định",
    },
    {
      amount: 30,
      price: 24.8,
      date: "2024-12-23",
      total: 744,
      status: "Đã bán",
      type: "Đấu giá",
    },
    {
      amount: 75,
      price: 26.2,
      date: "2024-12-28",
      total: 1965,
      status: "Chờ duyệt",
      type: "Giá cố định",
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
              Niêm yết tín chỉ carbon
            </h2>
            <p className="text-gray-500 text-sm mb-6">
              Tạo đơn bán tín chỉ carbon với giá cố định hoặc đấu giá
            </p>

            {/* Tabs */}
            <div className="flex mb-6 border-b border-gray-200">
              <button
                onClick={() => setActiveTab("create")}
                className={`flex-1 text-center py-2 rounded-t-lg font-medium transition ${
                  activeTab === "create"
                    ? "bg-gray-100 text-gray-800 border border-gray-200 border-b-transparent"
                    : "bg-white text-gray-500"
                }`}
              >
                Tạo niêm yết mới
              </button>
              <button
                onClick={() => setActiveTab("manage")}
                className={`flex-1 text-center py-2 rounded-t-lg font-medium transition ${
                  activeTab === "manage"
                    ? "bg-gray-100 text-gray-800 border border-gray-200 border-b-transparent"
                    : "bg-white text-gray-500"
                }`}
              >
                Quản lý niêm yết
              </button>
            </div>

            {/* Nội dung tab */}
            {activeTab === "create" ? (
              <div className="grid grid-cols-2 gap-8">
                {/* Form tạo niêm yết */}
                <div>
                  <div className="mb-4">
                    <label className="text-sm text-gray-700 font-medium">
                      Số lượng tín chỉ carbon (tCO₂)
                    </label>
                    <input
                      type="number"
                      placeholder="Nhập số lượng"
                      className="w-full mt-1 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:ring-1 focus:ring-green-500 outline-none"
                    />
                    <p className="text-xs text-gray-400 mt-1">
                      Số dư hiện tại: 342.5 tCO₂
                    </p>
                  </div>

                  <div className="mb-4">
                    <label className="text-sm text-gray-700 font-medium">
                      Loại niêm yết
                    </label>
                    <select
                      className="w-full mt-1 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:ring-1 focus:ring-green-500 outline-none"
                      value={listingType}
                      onChange={(e) => setListingType(e.target.value)}
                    >
                      <option value="fixed">Giá cố định</option>
                      <option value="auction">Đấu giá</option>
                    </select>
                  </div>

                  <div className="mb-4">
                    <label className="text-sm text-gray-700 font-medium">
                      {listingType === "fixed"
                        ? "Giá bán (VNĐ/tCO₂)"
                        : "Giá khởi điểm (VNĐ/tCO₂)"}
                    </label>
                    <input
                      type="number"
                      placeholder="25.5"
                      className="w-full mt-1 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:ring-1 focus:ring-green-500 outline-none"
                    />
                  </div>

                  {listingType === "auction" && (
                    <div className="mb-4">
                      <label className="text-sm text-gray-700 font-medium">
                        Thời gian đấu giá
                      </label>
                      <input
                        type="datetime-local"
                        className="w-full mt-1 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:ring-1 focus:ring-green-500 outline-none"
                      />
                    </div>
                  )}

                  <button className="w-full bg-black text-white py-2 rounded-lg mt-4 hover:bg-gray-800 transition">
                    Tạo niêm yết
                  </button>
                </div>

                {/* AI Gợi ý giá */}
                <div className="bg-gray-50 rounded-xl border border-gray-200 p-6">
                  <div className="flex items-center gap-2 mb-4">
                    <Lightbulb size={18} className="text-green-600" />
                    <h4 className="font-semibold text-gray-800">AI Gợi ý giá</h4>
                  </div>

                  <p className="text-green-600 text-3xl font-bold mb-1">
                    26.8 VNĐ/tCO₂
                  </p>
                  <p className="text-sm text-gray-500 mb-3">Giá đề xuất</p>

                  <div className="flex items-center gap-2 mb-3">
                    <span className="text-sm text-gray-600">Độ tin cậy:</span>
                    <div className="flex-1 bg-gray-200 rounded-full h-2">
                      <div
                        className="bg-green-500 h-2 rounded-full"
                        style={{ width: "85%" }}
                      />
                    </div>
                    <span className="text-sm font-medium text-gray-700">
                      85%
                    </span>
                  </div>

                  <div className="bg-blue-50 text-blue-600 text-sm p-3 rounded-lg mb-4 border border-blue-100">
                    Giá thị trường hiện tại tăng 5% so với tuần trước. Nhu cầu
                    cao trong khu vực của bạn.
                  </div>

                  <button className="w-full border border-gray-200 rounded-lg py-2 text-sm font-medium hover:bg-gray-100 transition">
                    Áp dụng giá đề xuất
                  </button>
                </div>
              </div>
            ) : (
              // Danh sách niêm yết
              <div className="space-y-4">
                {listings.map((l, i) => (
                  <div
                    key={i}
                    className="border border-gray-200 bg-gray-50 rounded-xl p-4 flex justify-between items-center hover:bg-gray-100 transition"
                  >
                    <div>
                      <p className="font-semibold text-gray-800">
                        {l.amount} tCO₂
                      </p>
                      <p className="text-sm text-gray-500">
                        {l.type} - {l.date}
                      </p>
                      <p className="text-xs text-gray-400 mt-1">
                        Tổng: {l.total.toLocaleString()} VNĐ
                      </p>
                    </div>
                    <p className="text-sm font-semibold text-gray-700">
                      {l.price} VNĐ/tCO₂
                    </p>
                    <div className="flex gap-2">
                      <span
                        className={`px-3 py-1 text-xs font-semibold rounded-full ${
                          l.status === "Đang bán"
                            ? "bg-gray-800 text-white"
                            : l.status === "Đã bán"
                            ? "bg-gray-200 text-gray-700"
                            : "bg-yellow-100 text-yellow-700"
                        }`}
                      >
                        {l.status}
                      </span>
                      {l.status === "Đang bán" && (
                        <>
                          <button className="px-3 py-1 text-xs border border-gray-200 rounded-lg hover:bg-gray-100">
                            Sửa
                          </button>
                          <button className="px-3 py-1 text-xs border border-gray-200 rounded-lg hover:bg-gray-100">
                            Hủy
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}
