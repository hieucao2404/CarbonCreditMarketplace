import React from "react";
import VerifierSidebar from "../components/VerifierSidebar";
import VerifierHeader from "../components/VerifierHeader";

export default function VerifierDashboard() {
  return (
    <div className="flex h-screen bg-gray-50">
      {/* Sidebar */}
      <VerifierSidebar />

      {/* Main content */}
      <div className="flex-1 flex flex-col">
        {/* Header */}
        <VerifierHeader />

        {/* Page content */}
        <main className="flex-1 overflow-y-auto p-8">
          {/* Title */}
          <div className="mb-6">
            <h1 className="text-2xl font-semibold text-gray-800">
              Tổng quan hoạt động kiểm toán
            </h1>
            <p className="text-gray-500 text-sm mt-1">
              Theo dõi và quản lý các yêu cầu xác minh tín chỉ carbon
            </p>
          </div>

          {/* Summary cards */}
          <div className="grid grid-cols-4 gap-6 mb-8">
            <div className="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm">
              <p className="text-gray-500 text-sm">Chờ xác minh</p>
              <h2 className="text-3xl font-bold text-gray-800 mt-2">12</h2>
              <p className="text-xs text-gray-400">Yêu cầu cần xử lý</p>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm">
              <p className="text-gray-500 text-sm">Đã duyệt tháng này</p>
              <h2 className="text-3xl font-bold text-gray-800 mt-2">89</h2>
              <p className="text-xs text-gray-400">+3 yêu cầu bị từ chối</p>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm">
              <p className="text-gray-500 text-sm">Tổng đã xác minh</p>
              <h2 className="text-3xl font-bold text-gray-800 mt-2">2,547</h2>
              <p className="text-xs text-gray-400">Tín chỉ carbon tổng cộng</p>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm">
              <p className="text-gray-500 text-sm">Tỷ lệ duyệt</p>
              <h2 className="text-3xl font-bold text-gray-800 mt-2">97%</h2>
              <p className="text-xs text-gray-400">Trong tháng này</p>
            </div>
          </div>

          {/* Lower grid */}
          <div className="grid grid-cols-2 gap-6">
            {/* High Priority Requests */}
            <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm">
              <h3 className="text-lg font-semibold text-gray-800 mb-2">
                Yêu cầu ưu tiên cao
              </h3>
              <p className="text-sm text-gray-500 mb-4">
                Các yêu cầu cần xử lý gấp
              </p>

              <div className="space-y-3">
                <div className="flex items-center justify-between bg-red-50 border border-red-200 p-3 rounded-xl">
                  <div className="flex items-center gap-3">
                    <span className="text-red-500 text-lg">⚠️</span>
                    <div>
                      <p className="font-medium text-gray-800">Nguyễn Văn A</p>
                      <p className="text-xs text-gray-500">25.5 tCO₂</p>
                    </div>
                  </div>
                  <button className="bg-black text-white text-sm px-4 py-1.5 rounded-lg hover:bg-gray-800 transition">
                    Xem chi tiết
                  </button>
                </div>

                <div className="flex items-center justify-between bg-red-50 border border-red-200 p-3 rounded-xl">
                  <div className="flex items-center gap-3">
                    <span className="text-red-500 text-lg">⚠️</span>
                    <div>
                      <p className="font-medium text-gray-800">Trần Thị B</p>
                      <p className="text-xs text-gray-500">18.7 tCO₂</p>
                    </div>
                  </div>
                  <button className="bg-black text-white text-sm px-4 py-1.5 rounded-lg hover:bg-gray-800 transition">
                    Xem chi tiết
                  </button>
                </div>
              </div>
            </div>

            {/* Recent Activities */}
            <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm">
              <h3 className="text-lg font-semibold text-gray-800 mb-2">
                Hoạt động gần đây
              </h3>
              <p className="text-sm text-gray-500 mb-4">
                Lịch sử xác minh mới nhất
              </p>

              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="text-green-500">✔</span>
                    <div>
                      <p className="font-medium text-gray-800">Phạm Thị D</p>
                      <p className="text-xs text-gray-500">45.2 tCO₂</p>
                    </div>
                  </div>
                  <span className="bg-gray-900 text-white text-xs px-3 py-1 rounded-md">
                    Đã duyệt
                  </span>
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="text-green-500">✔</span>
                    <div>
                      <p className="font-medium text-gray-800">Hoàng Văn E</p>
                      <p className="text-xs text-gray-500">28.9 tCO₂</p>
                    </div>
                  </div>
                  <span className="bg-gray-900 text-white text-xs px-3 py-1 rounded-md">
                    Đã duyệt
                  </span>
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="text-red-500">✖</span>
                    <div>
                      <p className="font-medium text-gray-800">Vũ Thị F</p>
                      <p className="text-xs text-gray-500">15.6 tCO₂</p>
                    </div>
                  </div>
                  <span className="bg-red-600 text-white text-xs px-3 py-1 rounded-md">
                    Từ chối
                  </span>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
