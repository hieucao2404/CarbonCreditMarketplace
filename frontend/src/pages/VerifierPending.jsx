import VerifierSidebar from "../components/VerifierSidebar";
import VerifierHeader from "../components/VerifierHeader";
import { Eye, Download, MapPin, Calendar } from "lucide-react";

export default function VerifierPending() {
  return (
    <div className="flex h-screen bg-gray-50">
      <VerifierSidebar />
      <div className="flex-1 flex flex-col">
        <VerifierHeader />

        <main className="flex-1 overflow-y-auto p-8">
          <div>
            <h1 className="text-xl font-semibold text-gray-800">
              Yêu cầu xác minh tín chỉ carbon
            </h1>
            <p className="text-gray-500 mt-1">
              Kiểm tra và duyệt các yêu cầu phát hành tín chỉ carbon
            </p>
          </div>

          {/* Card 1 */}
          <div className="bg-white mt-6 p-5 rounded-xl shadow-sm border border-gray-100">
            <div className="flex justify-between">
              <div>
                <div className="flex items-center space-x-3">
                  <span className="font-semibold text-gray-800">VR001</span>
                  <span className="bg-red-100 text-red-600 text-xs font-medium px-2 py-0.5 rounded-md">
                    Ưu tiên cao
                  </span>
                  <span className="bg-yellow-100 text-yellow-600 text-xs font-medium px-2 py-0.5 rounded-md">
                    Chờ xử lý
                  </span>
                </div>

                <p className="mt-2 text-gray-700">
                  <strong>Chủ xe:</strong> Nguyễn Văn A
                </p>
                <p className="text-gray-700">
                  <strong>Xe:</strong> Tesla Model 3 - 2023
                </p>
                <p className="text-gray-700">
                  <strong>Khoảng thời gian:</strong> 2024-12-01 đến 2024-12-28
                </p>

                <div className="mt-2 text-sm">
                  <p>
                    <span className="text-gray-600">Quãng đường:</span>{" "}
                    <span className="font-semibold">1,247.5 km</span>
                  </p>
                  <p>
                    <span className="text-gray-600">CO₂ tiết kiệm:</span>{" "}
                    <span className="text-green-600 font-semibold">234.8 kg</span>
                  </p>
                  <p>
                    <span className="text-gray-600">Tín chỉ yêu cầu:</span>{" "}
                    <span className="text-blue-600 font-semibold">25.5 tCO₂</span>
                  </p>
                </div>

                <div className="mt-3 flex flex-wrap gap-2">
                  <span className="px-3 py-1 text-sm bg-gray-100 rounded-lg border">
                    trip_log.csv
                  </span>
                  <span className="px-3 py-1 text-sm bg-gray-100 rounded-lg border">
                    vehicle_registration.pdf
                  </span>
                  <span className="px-3 py-1 text-sm bg-gray-100 rounded-lg border">
                    charging_records.pdf
                  </span>
                </div>
              </div>

              <div className="flex flex-col justify-between items-end">
                <button className="flex items-center space-x-1 px-4 py-2 bg-gray-900 text-white rounded-lg hover:bg-gray-800">
                  <Eye className="w-4 h-4" />
                  <span>Xem xét</span>
                </button>
                <button className="mt-2 flex items-center space-x-1 px-4 py-2 border rounded-lg hover:bg-gray-100">
                  <Download className="w-4 h-4" />
                  <span>Tài liệu</span>
                </button>
                <div className="text-sm text-gray-500 mt-2">
                  <div className="flex items-center">
                    <MapPin className="w-4 h-4 mr-1" /> Hà Nội
                  </div>
                  <div className="flex items-center mt-1">
                    <Calendar className="w-4 h-4 mr-1" /> 2024-12-28
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Card 2 */}
          <div className="bg-white mt-6 p-5 rounded-xl shadow-sm border border-gray-100">
            <div className="flex justify-between">
              <div>
                <div className="flex items-center space-x-3">
                  <span className="font-semibold text-gray-800">VR002</span>
                  <span className="bg-blue-100 text-blue-600 text-xs font-medium px-2 py-0.5 rounded-md">
                    Trung bình
                  </span>
                  <span className="bg-yellow-100 text-yellow-600 text-xs font-medium px-2 py-0.5 rounded-md">
                    Đang xem xét
                  </span>
                </div>

                <p className="mt-2 text-gray-700">
                  <strong>Chủ xe:</strong> Trần Thị B
                </p>
                <p className="text-gray-700">
                  <strong>Xe:</strong> BYD Seal - 2024
                </p>
                <p className="text-gray-700">
                  <strong>Khoảng thời gian:</strong> 2024-12-15 đến 2024-12-28
                </p>

                <div className="mt-2 text-sm">
                  <p>
                    <span className="text-gray-600">Quãng đường:</span>{" "}
                    <span className="font-semibold">892.3 km</span>
                  </p>
                  <p>
                    <span className="text-gray-600">CO₂ tiết kiệm:</span>{" "}
                    <span className="text-green-600 font-semibold">167.4 kg</span>
                  </p>
                  <p>
                    <span className="text-gray-600">Tín chỉ yêu cầu:</span>{" "}
                    <span className="text-blue-600 font-semibold">18.7 tCO₂</span>
                  </p>
                </div>

                <div className="mt-3 flex flex-wrap gap-2">
                  <span className="px-3 py-1 text-sm bg-gray-100 rounded-lg border">
                    trip_data.json
                  </span>
                  <span className="px-3 py-1 text-sm bg-gray-100 rounded-lg border">
                    ownership_proof.pdf
                  </span>
                </div>
              </div>

              <div className="flex flex-col justify-between items-end">
                <button className="flex items-center space-x-1 px-4 py-2 bg-gray-900 text-white rounded-lg hover:bg-gray-800">
                  <Eye className="w-4 h-4" />
                  <span>Xem xét</span>
                </button>
                <button className="mt-2 flex items-center space-x-1 px-4 py-2 border rounded-lg hover:bg-gray-100">
                  <Download className="w-4 h-4" />
                  <span>Tài liệu</span>
                </button>
                <div className="text-sm text-gray-500 mt-2">
                  <div className="flex items-center">
                    <MapPin className="w-4 h-4 mr-1" /> TP.HCM
                  </div>
                  <div className="flex items-center mt-1">
                    <Calendar className="w-4 h-4 mr-1" /> 2024-12-27
                  </div>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
