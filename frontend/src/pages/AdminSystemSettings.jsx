import React, { useState, useEffect } from "react";
import AdminSidebar from "../components/AdminSidebar";
import AdminHeader from "../components/AdminHeader";
import {
  Settings,
  Save,
  ShieldCheck,
  Lock,
  DollarSign,
  RefreshCw,
  AlertCircle,
  Info,
} from "lucide-react";
import axiosInstance from "../api/axiosInstance";

/**
 * Admin System Settings Page (Simplified)
 * 
 * PURPOSE:
 * - Edit platform-wide configuration
 * - Control platform fee percentage
 * - Toggle maintenance mode
 * 
 * BACKEND INTEGRATION:
 * - GET /api/system-settings → Fetch all settings
 * - PUT /api/system-settings/bulk → Update multiple settings
 * - PUT /api/system-settings/{key} → Update individual setting
 * 
 * FEATURES:
 * ✅ Platform Fee Management (0-100%)
 * ✅ Maintenance Mode Toggle
 * ✅ Real-time validation
 * ✅ Success/Error feedback
 */
export default function AdminSystemSettings() {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  // System settings from backend
  const [settings, setSettings] = useState({
    platformFee: 5.0, // PLATFORM_FEE_PERCENT
    maintenanceMode: false, // MAINTENANCE_MODE
  });

  // Track if settings have changed
  const [hasChanges, setHasChanges] = useState(false);

  // ============================================
  // COMPONENT LIFECYCLE
  // ============================================

  useEffect(() => {
    loadSettings();
  }, []);

  /**
   * Load system settings from backend
   * 
   * ENDPOINT: GET /api/system-settings
   * RESPONSE: [
   *   { settingKey: "PLATFORM_FEE_PERCENT", settingValue: "5.0" },
   *   { settingKey: "MAINTENANCE_MODE", settingValue: "false" }
   * ]
   */
  const loadSettings = async () => {
    setLoading(true);
    setError("");

    try {
      console.log("📥 Loading system settings...");

      const response = await axiosInstance.get("/system-settings");
      const settingsData = response.data?.data || [];

      // Extract platform fee
      const platformFeeObj = settingsData.find(
        (s) => s.settingKey === "PLATFORM_FEE_PERCENT"
      );
      const platformFee = platformFeeObj
        ? parseFloat(platformFeeObj.settingValue)
        : 5.0;

      // Extract maintenance mode
      const maintenanceModeObj = settingsData.find(
        (s) => s.settingKey === "MAINTENANCE_MODE"
      );
      const maintenanceMode = maintenanceModeObj
        ? maintenanceModeObj.settingValue === "true"
        : false;

      console.log("✅ Settings loaded:");
      console.log("   • Platform Fee:", platformFee + "%");
      console.log("   • Maintenance Mode:", maintenanceMode);

      setSettings({ platformFee, maintenanceMode });
      setHasChanges(false);
    } catch (e) {
      console.error("❌ Error loading settings:", e.response?.data || e.message);
      setError(
        e.response?.data?.message ||
          "Không thể tải cài đặt. Vui lòng thử lại sau."
      );
    } finally {
      setLoading(false);
    }
  };

  // ============================================
  // SAVE FUNCTIONS
  // ============================================

  /**
   * Save all settings to backend
   * 
   * ENDPOINT: PUT /api/system-settings/bulk
   * BODY: {
   *   "PLATFORM_FEE_PERCENT": "5.5",
   *   "MAINTENANCE_MODE": "false"
   * }
   */
  const handleSaveSettings = async () => {
    // Validation
    if (settings.platformFee < 0 || settings.platformFee > 100) {
      setError("Phí nền tảng phải trong khoảng 0-100%");
      return;
    }

    setSaving(true);
    setError("");
    setSuccessMessage("");

    try {
      console.log("💾 Saving settings...");
      console.log("   • Platform Fee:", settings.platformFee + "%");
      console.log("   • Maintenance Mode:", settings.maintenanceMode);

      // Prepare bulk update request
      const updates = {
        PLATFORM_FEE_PERCENT: settings.platformFee.toString(),
        MAINTENANCE_MODE: settings.maintenanceMode.toString(),
      };

      const response = await axiosInstance.put("/system-settings/bulk", updates);

      console.log("✅ Settings saved successfully:", response.data);

      setSuccessMessage(
        "✅ Cài đặt đã được lưu thành công! " +
        `Phí nền tảng: ${settings.platformFee}%, ` +
        `Bảo trì: ${settings.maintenanceMode ? "Bật" : "Tắt"}`
      );

      setHasChanges(false);

      // Clear success message after 3 seconds
      setTimeout(() => {
        setSuccessMessage("");
      }, 3000);
    } catch (e) {
      console.error("❌ Error saving settings:", e.response?.data || e.message);
      setError(
        e.response?.data?.message ||
          "Không thể lưu cài đặt. Vui lòng kiểm tra lại."
      );
    } finally {
      setSaving(false);
    }
  };

  /**
   * Quick toggle maintenance mode
   * 
   * ENDPOINT: PUT /api/system-settings/MAINTENANCE_MODE
   * BODY: { "value": "true" | "false" }
   */
  const handleQuickToggleMaintenanceMode = async () => {
    const newValue = !settings.maintenanceMode;

    try {
      console.log("🚧 Quick toggling maintenance mode to:", newValue);

      await axiosInstance.put("/system-settings/MAINTENANCE_MODE", {
        value: newValue.toString(),
      });

      setSettings({ ...settings, maintenanceMode: newValue });

      setSuccessMessage(
        newValue
          ? "🚧 Chế độ bảo trì đã BẬT. Người dùng không thể truy cập hệ thống."
          : "✅ Chế độ bảo trì đã TẮT. Hệ thống hoạt động bình thường."
      );

      setTimeout(() => setSuccessMessage(""), 3000);
    } catch (e) {
      console.error("❌ Error toggling maintenance mode:", e);
      setError("Không thể thay đổi chế độ bảo trì.");
    }
  };

  // ============================================
  // EVENT HANDLERS
  // ============================================

  const handlePlatformFeeChange = (e) => {
    const newFee = parseFloat(e.target.value) || 0;
    setSettings({ ...settings, platformFee: newFee });
    setHasChanges(true);
  };

  const handleMaintenanceModeToggle = (e) => {
    setSettings({ ...settings, maintenanceMode: e.target.checked });
    setHasChanges(true);
  };

  // ============================================
  // LOADING & ERROR STATES
  // ============================================

  if (loading) {
    return (
      <div className="flex min-h-screen bg-gray-50">
        <AdminSidebar />
        <div className="flex-1 flex flex-col">
          <AdminHeader />
          <main className="flex-1 flex items-center justify-center">
            <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Đang tải cài đặt hệ thống...</p>
            </div>
          </main>
        </div>
      </div>
    );
  }

  if (error && !successMessage) {
    return (
      <div className="flex min-h-screen bg-gray-50">
        <AdminSidebar />
        <div className="flex-1 flex flex-col">
          <AdminHeader />
          <main className="p-8">
            <div className="max-w-4xl mx-auto">
              <div className="bg-red-50 border border-red-200 rounded-lg p-6 flex items-start gap-3">
                <AlertCircle
                  className="text-red-600 flex-shrink-0 mt-0.5"
                  size={20}
                />
                <div className="flex-1">
                  <h3 className="text-red-800 font-semibold mb-1">
                    Lỗi tải dữ liệu
                  </h3>
                  <p className="text-red-600 text-sm">{error}</p>
                  <button
                    onClick={loadSettings}
                    className="mt-3 flex items-center gap-2 text-red-700 hover:text-red-800 text-sm font-medium"
                  >
                    <RefreshCw size={16} />
                    Thử lại
                  </button>
                </div>
              </div>
            </div>
          </main>
        </div>
      </div>
    );
  }

  // ============================================
  // MAIN RENDER
  // ============================================

  return (
    <div className="flex min-h-screen bg-gray-50">
      <AdminSidebar />

      <div className="flex flex-col flex-1">
        <AdminHeader />

        <main className="flex-1 p-8">
          <div className="max-w-4xl mx-auto">
            {/* Page Header */}
            <div className="mb-6">
              <div className="flex items-center justify-between">
                <div>
                  <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-3">
                    <Settings className="text-blue-600" size={32} />
                    Cài đặt hệ thống
                  </h1>
                  <p className="text-gray-600 mt-2">
                    Quản lý cấu hình và tham số của nền tảng Carbon Credit
                  </p>
                </div>
                <button
                  onClick={loadSettings}
                  disabled={loading}
                  className="flex items-center gap-2 px-4 py-2 text-gray-600 hover:text-gray-800 border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 transition-colors"
                >
                  <RefreshCw
                    size={16}
                    className={loading ? "animate-spin" : ""}
                  />
                  Làm mới
                </button>
              </div>
            </div>

            {/* Success Message */}
            {successMessage && (
              <div className="mb-6 bg-green-50 border border-green-200 rounded-lg p-4 flex items-start gap-3">
                <ShieldCheck
                  className="text-green-600 flex-shrink-0 mt-0.5"
                  size={20}
                />
                <p className="text-green-800 font-medium">{successMessage}</p>
              </div>
            )}

            {/* System Status Banner */}
            <div
              className={`mb-6 rounded-lg p-5 flex items-center justify-between ${
                settings.maintenanceMode
                  ? "bg-red-50 border-2 border-red-300"
                  : "bg-green-50 border-2 border-green-300"
              }`}
            >
              <div className="flex items-center gap-3">
                <div
                  className={`w-4 h-4 rounded-full ${
                    settings.maintenanceMode
                      ? "bg-red-500 animate-pulse"
                      : "bg-green-500"
                  }`}
                ></div>
                <div>
                  <p
                    className={`font-bold text-lg ${
                      settings.maintenanceMode ? "text-red-800" : "text-green-800"
                    }`}
                  >
                    {settings.maintenanceMode
                      ? "🚧 Chế độ bảo trì đang BẬT"
                      : "✅ Hệ thống hoạt động bình thường"}
                  </p>
                  <p
                    className={`text-sm ${
                      settings.maintenanceMode ? "text-red-600" : "text-green-600"
                    }`}
                  >
                    {settings.maintenanceMode
                      ? "Người dùng không thể truy cập hệ thống"
                      : "Tất cả dịch vụ đang hoạt động"}
                  </p>
                </div>
              </div>
              <button
                onClick={handleQuickToggleMaintenanceMode}
                className={`px-5 py-2.5 rounded-lg font-semibold transition-colors ${
                  settings.maintenanceMode
                    ? "bg-red-600 text-white hover:bg-red-700"
                    : "bg-green-600 text-white hover:bg-green-700"
                }`}
              >
                {settings.maintenanceMode ? "Tắt bảo trì" : "Bật bảo trì"}
              </button>
            </div>

            {/* Settings Panel */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-200">
              {/* Section 1: Platform Fee */}
              <div className="p-6 border-b border-gray-200">
                <div className="flex items-start gap-3 mb-4">
                  <DollarSign className="text-blue-600 mt-1" size={24} />
                  <div className="flex-1">
                    <h3 className="text-lg font-semibold text-gray-900 mb-1">
                      Phí nền tảng
                    </h3>
                    <p className="text-sm text-gray-600">
                      Phần trăm phí thu từ mỗi giao dịch thành công. Phí sẽ được
                      chuyển vào ví <code className="bg-gray-100 px-2 py-0.5 rounded">system_admin</code>.
                    </p>
                  </div>
                </div>

                <div className="ml-9">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Tỷ lệ phí (%)
                  </label>
                  <div className="flex items-center gap-4">
                    <input
                      type="number"
                      value={settings.platformFee}
                      onChange={handlePlatformFeeChange}
                      min="0"
                      max="100"
                      step="0.1"
                      className="w-40 border border-gray-300 rounded-lg px-4 py-2.5 text-lg font-semibold focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    />
                    <span className="text-gray-700 font-medium">
                      % của mỗi giao dịch
                    </span>
                  </div>

                  {/* Fee Examples */}
                  <div className="mt-4 bg-blue-50 border border-blue-200 rounded-lg p-4">
                    <h4 className="font-medium text-blue-800 mb-2 text-sm">
                      📊 Ví dụ tính phí:
                    </h4>
                    <div className="space-y-2 text-sm text-blue-700">
                      <div className="flex justify-between">
                        <span>Giao dịch 100,000 VND:</span>
                        <span className="font-semibold">
                          Phí = {(100000 * settings.platformFee / 100).toLocaleString("vi-VN")} VND
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span>Giao dịch 1,000,000 VND:</span>
                        <span className="font-semibold">
                          Phí = {(1000000 * settings.platformFee / 100).toLocaleString("vi-VN")} VND
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span>Giao dịch 10,000,000 VND:</span>
                        <span className="font-semibold">
                          Phí = {(10000000 * settings.platformFee / 100).toLocaleString("vi-VN")} VND
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Industry Benchmark */}
                  <div className="mt-3 flex items-start gap-2 text-sm text-gray-600">
                    <Info size={16} className="mt-0.5 flex-shrink-0" />
                    <span>
                      <strong>Khuyến nghị:</strong> Phí trung bình ngành: 3-7%. 
                      Phí hiện tại của bạn: <strong>{settings.platformFee}%</strong>
                      {settings.platformFee < 3 && " (Thấp)"}
                      {settings.platformFee >= 3 && settings.platformFee <= 7 && " (Hợp lý)"}
                      {settings.platformFee > 7 && " (Cao)"}
                    </span>
                  </div>
                </div>
              </div>

              {/* Section 2: Maintenance Mode */}
              <div className="p-6">
                <div className="flex items-start gap-3 mb-4">
                  <Lock className="text-blue-600 mt-1" size={24} />
                  <div className="flex-1">
                    <h3 className="text-lg font-semibold text-gray-900 mb-1">
                      Chế độ bảo trì
                    </h3>
                    <p className="text-sm text-gray-600">
                      Tạm dừng tất cả hoạt động giao dịch trên nền tảng. Chỉ admin
                      mới có thể truy cập hệ thống.
                    </p>
                  </div>
                </div>

                <div className="ml-9">
                  <label className="flex items-center gap-3 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={settings.maintenanceMode}
                      onChange={handleMaintenanceModeToggle}
                      className="w-6 h-6 accent-blue-600 cursor-pointer"
                    />
                    <span className="text-gray-700 font-medium">
                      {settings.maintenanceMode ? "Đang bật" : "Đang tắt"}
                    </span>
                  </label>

                  {/* Warning when enabled */}
                  {settings.maintenanceMode && (
                    <div className="mt-4 bg-red-50 border border-red-200 rounded-lg p-4">
                      <h4 className="font-medium text-red-800 mb-2 text-sm flex items-center gap-2">
                        <AlertCircle size={16} />
                        ⚠️ Cảnh báo
                      </h4>
                      <ul className="text-sm text-red-700 space-y-1 ml-5">
                        <li>• Người dùng không thể truy cập hệ thống</li>
                        <li>• Tất cả giao dịch đang xử lý sẽ bị tạm dừng</li>
                        <li>• Chỉ admin có quyền truy cập</li>
                      </ul>
                    </div>
                  )}
                </div>
              </div>

              {/* Save Button */}
              <div className="px-6 py-4 bg-gray-50 border-t border-gray-200 rounded-b-xl">
                <div className="flex items-center justify-between">
                  <div className="text-sm text-gray-600">
                    {hasChanges ? (
                      <span className="text-orange-600 font-medium">
                        ⚠️ Bạn có thay đổi chưa lưu
                      </span>
                    ) : (
                      <span>Tất cả thay đổi đã được lưu</span>
                    )}
                  </div>
                  <button
                    onClick={handleSaveSettings}
                    disabled={saving || !hasChanges}
                    className="bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2 font-medium"
                  >
                    {saving ? (
                      <>
                        <RefreshCw className="w-5 h-5 animate-spin" />
                        Đang lưu...
                      </>
                    ) : (
                      <>
                        <Save className="w-5 h-5" />
                        Lưu cài đặt
                      </>
                    )}
                  </button>
                </div>
                <p className="text-xs text-gray-500 mt-3">
                  💡 <strong>Lưu ý:</strong> Thay đổi phí nền tảng sẽ áp dụng cho
                  tất cả giao dịch mới sau khi lưu.
                </p>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}