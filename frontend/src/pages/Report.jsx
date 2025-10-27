import React from "react";
import Sidebar from "../components/Sidebar";
import Header from "../components/Header";
import { Leaf, DollarSign, BarChart3 } from "lucide-react";

export default function Report() {
  return (
    <div className="flex min-h-screen w-screen bg-[#F9FAFB] overflow-hidden">
      <Sidebar />

      <div className="flex flex-col flex-1 min-h-screen w-full">
        <Header />

        <main className="flex-1 p-8 w-full bg-[#F9FAFB] overflow-y-auto">
          <div className="w-full max-w-7xl mx-auto space-y-8">
            {/* --- Header Section --- */}
            <div>
              <h2 className="text-xl font-semibold text-gray-800">Báo cáo</h2>
              <p className="text-gray-500 text-sm">
                Tổng hợp hiệu quả tiết kiệm CO₂ và doanh thu tín chỉ carbon của bạn
              </p>
            </div>

            {/* --- Row 1: CO₂ + Revenue --- */}
            <div className="grid grid-cols-2 gap-6 w-full">
              {/* Card 1: Báo cáo CO₂ tiết kiệm */}
              <div className="bg-white border border-gray-300 rounded-2xl p-6 shadow-sm">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <Leaf className="text-green-600" size={22} />
                    <h3 className="font-semibold text-gray-700">
                      Báo cáo CO₂ tiết kiệm
                    </h3>
                  </div>
                </div>

                <div className="text-center space-y-1 mb-6">
                  <p className="text-3xl font-semibold text-green-600">
                    1,247.8 kg
                  </p>
                  <p className="text-sm text-gray-500">
                    Tổng CO₂ tiết kiệm năm 2024
                  </p>
                </div>

                <div className="flex justify-between text-center border-t border-gray-200 pt-4">
                  <div className="flex-1">
                    <p className="text-xl font-semibold text-gray-800">78.9 kg</p>
                    <p className="text-sm text-gray-500">Tháng này</p>
                  </div>
                  <div className="w-px bg-gray-200 mx-6" />
                  <div className="flex-1">
                    <p className="text-xl font-semibold text-gray-800">21.4 kg</p>
                    <p className="text-sm text-gray-500">Tuần này</p>
                  </div>
                </div>
              </div>

              {/* Card 2: Doanh thu từ tín chỉ carbon */}
              <div className="bg-white border border-gray-300 rounded-2xl p-6 shadow-sm">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <DollarSign className="text-blue-600" size={22} />
                    <h3 className="font-semibold text-gray-700">
                      Doanh thu từ tín chỉ carbon
                    </h3>
                  </div>
                </div>

                <div className="text-center space-y-1 mb-6">
                  <p className="text-3xl font-semibold text-blue-600">
                    15,680 VNĐ
                  </p>
                  <p className="text-sm text-gray-500">
                    Tổng doanh thu năm 2024
                  </p>
                </div>

                <div className="flex justify-between text-center border-t border-gray-200 pt-4">
                  <div className="flex-1">
                    <p className="text-xl font-semibold text-gray-800">2,340 VNĐ</p>
                    <p className="text-sm text-gray-500">Tháng này</p>
                  </div>
                  <div className="w-px bg-gray-200 mx-6" />
                  <div className="flex-1">
                    <p className="text-xl font-semibold text-gray-800">586,000 VNĐ</p>
                    <p className="text-sm text-gray-500">Tuần này</p>
                  </div>
                </div>
              </div>
            </div>

            {/* --- Row 2: Tổng quan hoạt động --- */}
            <div className="bg-white border border-gray-300 rounded-2xl p-6 shadow-sm">
              <div className="flex items-center gap-2 mb-6">
                <BarChart3 className="text-green-600" size={22} />
                <h3 className="font-semibold text-gray-700">Tổng quan hoạt động</h3>
              </div>

              <div className="grid grid-cols-2 md:grid-cols-4 gap-6 text-center border-t border-gray-200 pt-4">
                <div>
                  <p className="text-2xl font-semibold text-gray-800">2,847 km</p>
                  <p className="text-sm text-gray-500">Tổng quãng đường</p>
                </div>
                <div className="w-px bg-gray-200 hidden md:block" />
                <div>
                  <p className="text-2xl font-semibold text-gray-800">342.5 tCO₂</p>
                  <p className="text-sm text-gray-500">Tín chỉ đã tạo</p>
                </div>
                <div className="w-px bg-gray-200 hidden md:block" />
                <div>
                  <p className="text-2xl font-semibold text-gray-800">267.3 tCO₂</p>
                  <p className="text-sm text-gray-500">Tín chỉ đã bán</p>
                </div>
                <div className="w-px bg-gray-200 hidden md:block" />
                <div>
                  <p className="text-2xl font-semibold text-gray-800">28</p>
                  <p className="text-sm text-gray-500">Giao dịch hoàn thành</p>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
