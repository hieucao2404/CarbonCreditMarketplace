import React, { useState, useEffect } from "react";
import Sidebar from "../components/Sidebar";
import Header from "../components/Header";
import StatCard from "../components/StatCard";
import axios from "axios";

export default function HomePage() {
  const [vehicles, setVehicles] = useState([]);

  useEffect(() => {
    // Ví dụ: load danh sách xe điện (bạn có thể sửa URL cho phù hợp)
    axios
      .get("http://localhost:8080/api/vehicles")
      .then((res) => setVehicles(res.data))
      .catch((err) => console.error("Lỗi khi tải danh sách xe:", err));
  }, []);

  return (
    <div className="flex min-h-screen w-screen bg-[#F9FAFB] overflow-hidden">
      {/* Sidebar */}
      <Sidebar />

      {/* Main content */}
      <div className="flex flex-col flex-1 min-h-screen w-full">
        {/* Header */}
        <Header />

        {/* Main section */}
        <main className="flex-1 p-8 w-full bg-[#F9FAFB] overflow-y-auto">
          {/* Thẻ thống kê */}
          <div className="grid grid-cols-4 gap-6 mb-8 w-full">
            <StatCard
              title="Số dư tín chỉ carbon"
              value="342.5 tCO₂"
              sub="+89.2 tCO₂ đang chờ xác minh"
            />
            <StatCard
              title="Thu nhập tháng này"
              value="2,340 VNĐ"
              sub="+12% so với tháng trước"
            />
            <StatCard
              title="CO₂ tiết kiệm tuần này"
              value="89.4 kg"
              sub="Tương đương 23.5 tín chỉ carbon"
            />
            <StatCard
              title="Tổng doanh thu"
              value="15,680 VNĐ"
              sub="Từ 0 giao dịch"
            />
          </div>

          {/* Xe điện & chuyến đi */}
          <div className="grid grid-cols-2 gap-6 w-full">
            {/* Xe điện của bạn */}
            <div className="bg-white rounded-xl p-6 shadow-sm border border-gray-100 w-full">
              <h2 className="font-semibold text-lg mb-1">Xe điện của bạn</h2>
              <p className="text-gray-500 text-sm mb-4">
                Trạng thái kết nối và dữ liệu hành trình
              </p>

              <div className="space-y-4 w-full">
                {vehicles.length > 0 ? (
                  vehicles.map((car, i) => (
                    <div
                      key={i}
                      className="flex justify-between items-center border border-gray-200 p-4 rounded-lg hover:shadow transition w-full"
                    >
                      <div>
                        <p className="font-medium text-gray-800">{car.model}</p>
                        <p className="text-sm text-gray-500">
                          VIN: {car.vin} • Ngày đăng ký: {car.registrationDate}
                        </p>
                      </div>
                      <button className="text-sm bg-green-600 text-white px-4 py-1.5 rounded-md shadow hover:bg-green-700 transition">
                        Đã kết nối
                      </button>
                    </div>
                  ))
                ) : (
                  <p className="text-gray-500 text-sm">
                    Không có xe nào được đăng ký
                  </p>
                )}
              </div>
            </div>

            {/* Chuyến đi gần đây */}
            <div className="bg-white rounded-xl p-6 shadow-sm border border-gray-100 w-full">
              <h2 className="font-semibold text-lg mb-1">Chuyến đi gần đây</h2>
              <p className="text-gray-500 text-sm mb-4">
                Tín chỉ carbon được tạo từ các chuyến đi
              </p>

              <div className="space-y-4 w-full">
                {[
                  {
                    km: "45.2 km",
                    date: "2024-12-28",
                    co2: "2.1 tCO₂",
                    sub: "0.8 kg CO₂",
                  },
                  {
                    km: "32.1 km",
                    date: "2024-12-27",
                    co2: "1.5 tCO₂",
                    sub: "0.6 kg CO₂",
                  },
                  {
                    km: "67.8 km",
                    date: "2024-12-26",
                    co2: "3.2 tCO₂",
                    sub: "1.2 kg CO₂",
                  },
                  {
                    km: "23.4 km",
                    date: "2024-12-25",
                    co2: "1 tCO₂",
                    sub: "0.4 kg CO₂",
                  },
                ].map((trip, i) => (
                  <div
                    key={i}
                    className="flex justify-between items-center border-b border-gray-200 pb-3 w-full"
                  >
                    <div>
                      <p className="font-medium text-emerald-600">{trip.km}</p>
                      <p className="text-gray-500 text-sm">{trip.date}</p>
                    </div>
                    <div className="text-right">
                      <p className="font-semibold text-gray-800">{trip.co2}</p>
                      <p className="text-gray-500 text-sm">{trip.sub}</p>
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
