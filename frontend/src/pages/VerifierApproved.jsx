import VerifierSidebar from "../components/VerifierSidebar";
import VerifierHeader from "../components/VerifierHeader";
import { CheckCircle2, XCircle } from "lucide-react";

export default function VerifierApproved() {
  return (
    <div className="flex h-screen bg-gray-50">
      <VerifierSidebar />
      <div className="flex-1 flex flex-col">
        <VerifierHeader />

        <main className="flex-1 overflow-y-auto p-8">
          <div>
            <h1 className="text-xl font-semibold text-gray-800">
              Tín chỉ carbon đã xác minh
            </h1>
            <p className="text-gray-500 mt-1">
              Danh sách các tín chỉ đã được duyệt và phát hành
            </p>
          </div>

          <div className="mt-6 bg-white p-6 rounded-2xl border border-gray-100 shadow-sm">
            {/* ITEM 1 */}
            <div className="flex justify-between items-center p-4 border border-gray-200 rounded-xl mb-4">
              <div className="flex items-center space-x-4">
                <CheckCircle2 className="w-6 h-6 text-green-500" />
                <div>
                  <p className="font-medium text-gray-800">Phạm Thị D</p>
                  <p className="text-sm text-gray-500">
                    Tesla Model Y - 2023
                  </p>
                </div>
              </div>

              <div className="text-right">
                <p className="font-semibold text-gray-800">45.2 tCO₂</p>
                <p className="text-sm text-gray-500">Hà Nội</p>
              </div>

              <div className="text-right">
                <p className="text-sm text-gray-500">2024-12-25</p>
                <p className="text-xs text-gray-400">CC-2024-001245</p>
              </div>

              <div className="flex items-center space-x-3">
                <span className="bg-gray-900 text-white text-sm font-medium px-3 py-1 rounded-lg">
                  Đã duyệt
                </span>
                <button className="px-4 py-1.5 border border-gray-300 rounded-lg text-sm hover:bg-gray-100">
                  Chi tiết
                </button>
              </div>
            </div>

            {/* ITEM 2 */}
            <div className="flex justify-between items-center p-4 border border-gray-200 rounded-xl mb-4">
              <div className="flex items-center space-x-4">
                <CheckCircle2 className="w-6 h-6 text-green-500" />
                <div>
                  <p className="font-medium text-gray-800">Hoàng Văn E</p>
                  <p className="text-sm text-gray-500">
                    BYD Tang - 2024
                  </p>
                </div>
              </div>

              <div className="text-right">
                <p className="font-semibold text-gray-800">28.9 tCO₂</p>
                <p className="text-sm text-gray-500">TP.HCM</p>
              </div>

              <div className="text-right">
                <p className="text-sm text-gray-500">2024-12-24</p>
                <p className="text-xs text-gray-400">CC-2024-001244</p>
              </div>

              <div className="flex items-center space-x-3">
                <span className="bg-gray-900 text-white text-sm font-medium px-3 py-1 rounded-lg">
                  Đã duyệt
                </span>
                <button className="px-4 py-1.5 border border-gray-300 rounded-lg text-sm hover:bg-gray-100">
                  Chi tiết
                </button>
              </div>
            </div>

            {/* ITEM 3 */}
            <div className="flex justify-between items-center p-4 border border-gray-200 rounded-xl">
              <div className="flex items-center space-x-4">
                <XCircle className="w-6 h-6 text-red-500" />
                <div>
                  <p className="font-medium text-gray-800">Vũ Thị F</p>
                  <p className="text-sm text-gray-500">
                    Tesla Model 3 - 2022
                  </p>
                </div>
              </div>

              <div className="text-right">
                <p className="font-semibold text-gray-800">15.6 tCO₂</p>
                <p className="text-sm text-gray-500">Hải Phòng</p>
              </div>

              <div className="text-right">
                <p className="text-sm text-gray-500">2024-12-23</p>
              </div>

              <div className="flex items-center space-x-3">
                <span className="bg-red-500 text-white text-sm font-medium px-3 py-1 rounded-lg">
                  Từ chối
                </span>
                <button className="px-4 py-1.5 border border-gray-300 rounded-lg text-sm hover:bg-gray-100">
                  Chi tiết
                </button>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
