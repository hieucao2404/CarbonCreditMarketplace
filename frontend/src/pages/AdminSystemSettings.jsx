import React, { useState } from "react";
import AdminSidebar from "../components/AdminSidebar";
import AdminHeader from "../components/AdminHeader";
import {
  Settings,
  Save,
  ShieldCheck,
  Lock,
  DollarSign,
} from "lucide-react";

export default function AdminSystemSettings() {
  const [activeTab, setActiveTab] = useState("general");

  const [settings, setSettings] = useState({
    minCredit: 1,
    maxCredit: 1000,
    maintenanceMode: false,
    platformFee: 5,
    estimatedRevenue: 6273350,
    requireVerification: true,
    autoApprove: false,
    securityLogs: [
      {
        title: "Đăng nhập Admin",
        email: "admin@system.vn",
        status: "Thành công",
        color: "bg-black text-white",
        time: "2024-12-28 10:30",
      },
      {
        title: "Thay đổi cài đặt phí",
        email: "admin@system.vn",
        status: "Thành công",
        color: "bg-black text-white",
        time: "2024-12-28 09:15",
      },
      {
        title: "Thử đăng nhập thất bại",
        email: "unknown@email.com",
        status: "Thất bại",
        color: "bg-red-600 text-white",
        time: "2024-12-27 22:45",
      },
    ],
  });

  const handleChange = (e) => {
    const { name, type, checked, value } = e.target;
    setSettings({
      ...settings,
      [name]: type === "checkbox" ? checked : value,
    });
  };

  const handleSave = () => {
    alert("✅ Cài đặt hệ thống đã được lưu thành công!");
  };

  const tabClass = (tab) =>
    `px-6 py-3 font-medium cursor-pointer transition ${
      activeTab === tab
        ? "border-b-2 border-black text-black"
        : "text-gray-600 hover:text-black"
    }`;

  return (
    <div className="flex min-h-screen bg-gray-50">
      <AdminSidebar />

      <div className="flex flex-col flex-1">
        <AdminHeader />

        <main className="flex-1 p-8">
          <div className="max-w-6xl mx-auto bg-white rounded-xl shadow-sm p-6">
            <h2 className="text-xl font-semibold mb-1">Cài đặt hệ thống</h2>
            <p className="text-gray-500 text-sm mb-6">
              Quản lý cấu hình và tham số của nền tảng
            </p>

            {/* Tabs */}
            <div className="flex border-b border-gray-200 mb-6">
              <button onClick={() => setActiveTab("general")} className={tabClass("general")}>
                Chung
              </button>
              <button onClick={() => setActiveTab("fees")} className={tabClass("fees")}>
                Phí dịch vụ
              </button>
              <button onClick={() => setActiveTab("verification")} className={tabClass("verification")}>
                Xác minh
              </button>
              <button onClick={() => setActiveTab("security")} className={tabClass("security")}>
                Bảo mật
              </button>
            </div>

            {/* === TAB 1: CHUNG === */}
            {activeTab === "general" && (
              <div className="space-y-6">
                <div className="grid grid-cols-2 gap-6">
                  <div>
                    <label className="block text-sm text-gray-600 mb-1">
                      Số lượng tín chỉ tối thiểu (tCO₂)
                    </label>
                    <input
                      type="number"
                      name="minCredit"
                      value={settings.minCredit}
                      onChange={handleChange}
                      className="w-full border border-gray-300 rounded-lg p-2 focus:ring-1 focus:ring-black"
                    />
                  </div>
                  <div>
                    <label className="block text-sm text-gray-600 mb-1">
                      Số lượng tín chỉ tối đa (tCO₂)
                    </label>
                    <input
                      type="number"
                      name="maxCredit"
                      value={settings.maxCredit}
                      onChange={handleChange}
                      className="w-full border border-gray-300 rounded-lg p-2 focus:ring-1 focus:ring-black"
                    />
                  </div>
                </div>

                <div className="flex items-center justify-between border-b border-gray-200 pb-3">
                  <div>
                    <p className="text-sm font-medium text-gray-700">
                      Chế độ bảo trì
                    </p>
                    <p className="text-sm text-gray-500">
                      Tạm dừng tất cả hoạt động trên nền tảng
                    </p>
                  </div>
                  <input
                    type="checkbox"
                    name="maintenanceMode"
                    checked={settings.maintenanceMode}
                    onChange={handleChange}
                    className="w-5 h-5 accent-black"
                  />
                </div>
              </div>
            )}

            {/* === TAB 2: PHÍ DỊCH VỤ === */}
            {activeTab === "fees" && (
              <div className="space-y-6">
                <div>
                  <label className="block text-sm text-gray-600 mb-1">
                    Phí nền tảng (%)
                  </label>
                  <input
                    type="number"
                    name="platformFee"
                    value={settings.platformFee}
                    onChange={handleChange}
                    className="w-32 border border-gray-300 rounded-lg p-2 focus:ring-1 focus:ring-black"
                  />
                </div>
                <p className="text-gray-500 text-sm">
                  Phí thu từ mỗi giao dịch thành công
                </p>

                <div className="bg-blue-50 p-4 rounded-lg">
                  <p className="font-medium text-gray-800 mb-1">
                    Dự báo doanh thu
                  </p>
                  <p className="text-sm text-gray-600">
                    Với mức phí {settings.platformFee}%, dự kiến thu được{" "}
                    <span className="font-semibold text-gray-800">
                      {settings.estimatedRevenue.toLocaleString("vi-VN")} VNĐ/tháng
                    </span>
                  </p>
                </div>
              </div>
            )}

            {/* === TAB 3: XÁC MINH === */}
            {activeTab === "verification" && (
              <div className="space-y-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-700">
                      Yêu cầu xác minh
                    </p>
                    <p className="text-sm text-gray-500">
                      Tất cả tín chỉ carbon phải được xác minh trước khi phát hành
                    </p>
                  </div>
                  <input
                    type="checkbox"
                    name="requireVerification"
                    checked={settings.requireVerification}
                    onChange={handleChange}
                    className="w-5 h-5 accent-black"
                  />
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-700">
                      Tự động duyệt
                    </p>
                    <p className="text-sm text-gray-500">
                      Tự động duyệt các yêu cầu từ người dùng đáng tin cậy
                    </p>
                  </div>
                  <input
                    type="checkbox"
                    name="autoApprove"
                    checked={settings.autoApprove}
                    onChange={handleChange}
                    className="w-5 h-5 accent-black"
                  />
                </div>
              </div>
            )}

            {/* === TAB 4: BẢO MẬT === */}
            {activeTab === "security" && (
              <div className="space-y-6">
                <h4 className="text-base font-semibold text-gray-800">
                  Nhật ký bảo mật
                </h4>

                <div className="space-y-3">
                  {settings.securityLogs.map((log, idx) => (
                    <div
                      key={idx}
                      className="flex items-center justify-between border-b border-gray-200 pb-3"
                    >
                      <div>
                        <p className="text-sm font-medium text-gray-700">
                          {log.title}
                        </p>
                        <p className="text-sm text-gray-500">{log.email}</p>
                      </div>
                      <div className="text-right">
                        <span
                          className={`px-2 py-1 rounded-md text-xs font-medium ${log.color}`}
                        >
                          {log.status}
                        </span>
                        <p className="text-xs text-gray-500 mt-1">{log.time}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* === Save Button === */}
            <div className="pt-8">
              <button
                onClick={handleSave}
                className="bg-black text-white px-6 py-3 rounded-lg hover:bg-gray-800 transition"
              >
                <Save className="inline-block w-5 h-5 mr-2" />
                Lưu cài đặt
              </button>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
