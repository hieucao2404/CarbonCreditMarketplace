import React, { useState } from "react";
import AdminSidebar from "../components/AdminSidebar";
import AdminHeader from "../components/AdminHeader";
import { MapPin } from "lucide-react";

export default function AdminStatistics() {
  const [activeTab, setActiveTab] = useState("seller");

  const regions = [
    { name: "Hà Nội", value: "1247 tCO₂", percent: "35%" },
    { name: "TP.HCM", value: "1089 tCO₂", percent: "31%" },
    { name: "Đà Nẵng", value: "567 tCO₂", percent: "16%" },
    { name: "Hải Phòng", value: "345 tCO₂", percent: "10%" },
    { name: "Khác", value: "289 tCO₂", percent: "8%" },
  ];

  const topSellers = [
    { name: "Công ty Green Tech", value: "31,250,000 VND", co2: "1250 tCO₂" },
    { name: "EcoViet Corp", value: "24,687,500 VND", co2: "987.5 tCO₂" },
    { name: "Clean Energy Ltd", value: "18,907,500 VND", co2: "756.3 tCO₂" },
  ];

  const topBuyers = [
    { name: "Công ty Xanh Việt", value: "29,345,000 VND", co2: "1180 tCO₂" },
    { name: "Future Carbon Co.", value: "22,670,000 VND", co2: "920 tCO₂" },
    { name: "Green Planet Inc.", value: "17,200,000 VND", co2: "698 tCO₂" },
  ];

  const currentUsers = activeTab === "seller" ? topSellers : topBuyers;

  return (
    <div className="flex min-h-screen bg-gray-50">
      {/* Sidebar */}
      <AdminSidebar />

      {/* Main content */}
      <div className="flex-1 flex flex-col">
        <AdminHeader />

        <main className="p-8">
          {/* === Top summary cards === */}
          <div className="grid grid-cols-4 gap-6 mb-8">
            <div className="bg-white border border-gray-200 rounded-xl p-5">
              <p className="text-gray-500 text-sm mb-1">Doanh thu tháng</p>
              <h2 className="text-2xl font-semibold text-gray-800">125.4M VND</h2>
              <p className="text-green-600 text-xs mt-1">+15% so với tháng trước</p>
            </div>

            <div className="bg-white border border-gray-200 rounded-xl p-5">
              <p className="text-gray-500 text-sm mb-1">Tín chỉ đã giao dịch</p>
              <h2 className="text-2xl font-semibold text-gray-800">3,456 tCO₂</h2>
              <p className="text-green-600 text-xs mt-1">+8% so với tháng trước</p>
            </div>

            <div className="bg-white border border-gray-200 rounded-xl p-5">
              <p className="text-gray-500 text-sm mb-1">Người dùng hoạt động</p>
              <h2 className="text-2xl font-semibold text-gray-800">1,923</h2>
              <p className="text-green-600 text-xs mt-1">+12% so với tháng trước</p>
            </div>

            <div className="bg-white border border-gray-200 rounded-xl p-5">
              <p className="text-gray-500 text-sm mb-1">Tỷ lệ hoàn thành</p>
              <h2 className="text-2xl font-semibold text-gray-800">97.2%</h2>
              <p className="text-green-600 text-xs mt-1">+0.5% so với tháng trước</p>
            </div>
          </div>

          {/* === Bottom section === */}
          <div className="grid grid-cols-2 gap-6">
            {/* Phân tích theo khu vực */}
            <div className="bg-white border border-gray-200 rounded-xl p-6">
              <h3 className="text-gray-800 font-semibold mb-4">
                Phân tích theo khu vực
              </h3>
              <ul className="space-y-3">
                {regions.map((r, i) => (
                  <li key={i} className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <MapPin size={16} className="text-gray-500" />
                      <span className="text-gray-700">{r.name}</span>
                    </div>
                    <div className="text-gray-700 text-sm">
                      {r.value}{" "}
                      <span className="text-gray-400">({r.percent})</span>
                    </div>
                  </li>
                ))}
              </ul>
            </div>

            {/* Top người dùng */}
            <div className="bg-white border border-gray-200 rounded-xl p-6">
              <h3 className="text-gray-800 font-semibold mb-4">Top người dùng</h3>

              {/* Tabs */}
              <div className="flex mb-3 border border-gray-200 rounded-full overflow-hidden w-fit">
                <button
                  onClick={() => setActiveTab("seller")}
                  className={`px-6 py-1.5 text-sm font-medium rounded-full transition-all ${
                    activeTab === "seller"
                      ? "bg-gray-100 text-gray-800"
                      : "text-gray-500 hover:text-gray-700"
                  }`}
                >
                  Người bán
                </button>
                <button
                  onClick={() => setActiveTab("buyer")}
                  className={`px-6 py-1.5 text-sm font-medium rounded-full transition-all ${
                    activeTab === "buyer"
                      ? "bg-gray-100 text-gray-800"
                      : "text-gray-500 hover:text-gray-700"
                  }`}
                >
                  Người mua
                </button>
              </div>

              {/* Danh sách */}
              <ul className="divide-y divide-gray-100">
                {currentUsers.map((u, i) => (
                  <li
                    key={i}
                    className="flex items-center justify-between py-3"
                  >
                    <div>
                      <p className="text-gray-800 font-medium">{u.name}</p>
                      <p className="text-xs text-gray-400">{u.co2}</p>
                    </div>
                    <p className="text-gray-800 text-sm font-medium">
                      {u.value}
                    </p>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
