import React from "react";
import AdminSidebar from "../components/AdminSidebar";
import AdminHeader from "../components/AdminHeader";

export default function AdminDashboard() {
  return (
    <div className="flex min-h-screen w-screen bg-gray-50 overflow-hidden">
      {/* Sidebar */}
      <AdminSidebar />

      {/* Main Content */}
      <div className="flex flex-col flex-1 min-h-screen w-full">
        {/* Header */}
        <AdminHeader />

        {/* Main Section */}
        <main className="flex-1 p-8 w-full bg-gray-50 overflow-y-auto">
          <div className="space-y-8 w-full">
            {/* Top Statistic Cards */}
            <div className="grid grid-cols-4 gap-6">
              <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm">
                <h3 className="text-sm font-medium text-gray-500 mb-2">
                  Tổng người dùng
                </h3>
                <p className="text-3xl font-semibold text-gray-800">2,847</p>
                <p className="text-sm text-gray-400 mt-1">
                  1,923 đang hoạt động
                </p>
              </div>

              <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm">
                <h3 className="text-sm font-medium text-gray-500 mb-2">
                  Tổng giao dịch
                </h3>
                <p className="text-3xl font-semibold text-gray-800">15,623</p>
                <p className="text-sm text-gray-400 mt-1">
                  Giá trị: 125,467,000 VNĐ
                </p>
              </div>

              <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm">
                <h3 className="text-sm font-medium text-gray-500 mb-2">
                  Tăng trưởng tháng
                </h3>
                <p className="text-3xl font-semibold text-orange-600">+12.5%</p>
                <p className="text-sm text-gray-400 mt-1">
                  So với tháng trước
                </p>
              </div>

              <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm">
                <h3 className="text-sm font-medium text-gray-500 mb-2">
                  Phí nền tảng
                </h3>
                <p className="text-3xl font-semibold text-gray-800">1,567,800</p>
                <p className="text-sm text-gray-400 mt-1">
                  VNĐ thu được tháng này
                </p>
              </div>
            </div>

            {/* Activity & Stats */}
            <div className="grid grid-cols-2 gap-6">
              {/* Recent Activity */}
              <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
                <h2 className="font-semibold text-lg mb-1 text-gray-800">
                  Hoạt động gần đây
                </h2>
                <p className="text-gray-500 text-sm mb-4">
                  Các sự kiện quan trọng trên hệ thống
                </p>

                <div className="space-y-4">
                  {[
                    { label: "Công ty XYZ", time: "5 phút trước", color: "bg-green-500" },
                    { label: "Giao dịch TX001 hoàn thành", time: "12 phút trước", color: "bg-blue-500" },
                    { label: "Tranh chấp TX003", time: "1 giờ trước", color: "bg-red-500" },
                    { label: "15 tín chỉ được xác minh", time: "2 giờ trước", color: "bg-orange-500" },
                  ].map((item, i) => (
                    <div key={i} className="flex items-center gap-3">
                      <div className={`w-3 h-3 rounded-full ${item.color}`}></div>
                      <div>
                        <p className="font-medium text-gray-700">{item.label}</p>
                        <p className="text-xs text-gray-500">{item.time}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Quick Stats */}
              <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
                <h2 className="font-semibold text-lg mb-1 text-gray-800">
                  Thống kê nhanh
                </h2>
                <p className="text-gray-500 text-sm mb-4">
                  Tổng quan hệ thống trong tháng
                </p>

                <div className="space-y-3 text-sm">
                  <p className="flex justify-between">
                    <span>Người dùng mới:</span>
                    <span className="text-green-600 font-medium">+347</span>
                  </p>
                  <p className="flex justify-between">
                    <span>Giao dịch thành công:</span>
                    <span className="text-green-600 font-medium">1,247</span>
                  </p>
                  <p className="flex justify-between">
                    <span>Tranh chấp:</span>
                    <span className="text-orange-500 font-medium">12</span>
                  </p>
                  <p className="flex justify-between">
                    <span>Tín chỉ được tạo:</span>
                    <span className="text-blue-600 font-medium">3,456 tCO₂</span>
                  </p>
                  <p className="flex justify-between">
                    <span>Tín chỉ được bán:</span>
                    <span className="text-purple-600 font-medium">2,891 tCO₂</span>
                  </p>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
