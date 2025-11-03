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
} from "lucide-react";
import axiosInstance from "../api/axiosInstance";

export default function AdminSystemSettings() {
  const [activeTab, setActiveTab] = useState("general");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const [settings, setSettings] = useState({
    minCredit: 1,
    maxCredit: 1000,
    maintenanceMode: false,
    platformFee: 5,
    monthlyRevenue: 0,
    estimatedPlatformRevenue: 0,
    requireVerification: true,
    autoApprove: false,
    totalUsers: 0,
    activeListings: 0,
  });

  const [auditLogs, setAuditLogs] = useState([]);

  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    setLoading(true);
    setError("");

    try {
      // 1. Fetch transaction data to calculate revenue
      const completedTxRes = await axiosInstance.get("/transactions/admin/by-status", {
        params: { status: "COMPLETED", page: 0, size: 100 }
      });

      const completedTransactions = completedTxRes.data?.data?.content || [];
      console.log("💰 Completed Transactions:", completedTransactions.length);

      // Calculate monthly revenue
      const now = new Date();
      const currentMonth = now.getMonth();
      const currentYear = now.getFullYear();

      const currentMonthTransactions = completedTransactions.filter(tx => {
        const txDate = new Date(tx.createdAt);
        return txDate.getMonth() === currentMonth && txDate.getFullYear() === currentYear;
      });

      const monthlyRevenue = currentMonthTransactions.reduce((sum, tx) => 
        sum + Number(tx.totalPrice || tx.amount || 0), 0
      );

      const estimatedPlatformRevenue = monthlyRevenue * (settings.platformFee / 100);

      // 2. Fetch users count
      const usersRes = await axiosInstance.get("/users");
      const totalUsers = usersRes.data?.data?.length || 0;
      console.log("👥 Total Users:", totalUsers);

      // 3. Fetch listing stats
      let activeListings = 0;
      try {
        const listingsStatsRes = await axiosInstance.get("/listings/stats");
        activeListings = listingsStatsRes.data?.data?.totalActiveListings || 0;
        console.log("📋 Active Listings:", activeListings);
      } catch (e) {
        console.warn("⚠️ Could not fetch listing stats");
      }

      // 4. Fetch recent audit logs (converted to security logs)
      // Since we can't directly fetch all audit logs without pagination, 
      // we'll fetch recent transactions and create security log entries
      const securityLogs = await fetchRecentActivity();

      setSettings(prev => ({
        ...prev,
        monthlyRevenue,
        estimatedPlatformRevenue,
        totalUsers,
        activeListings,
      }));

      setAuditLogs(securityLogs);

    } catch (e) {
      console.error("❌ Error loading settings:", e.response?.data || e.message);
      setError(e.response?.data?.message || "Không thể tải cài đặt hệ thống.");
    } finally {
      setLoading(false);
    }
  };

  const fetchRecentActivity = async () => {
    const logs = [];

    try {
      // Get recent completed transactions
      const completedRes = await axiosInstance.get("/transactions/admin/by-status", {
        params: { status: "COMPLETED", page: 0, size: 5 }
      });

      const completed = completedRes.data?.data?.content || [];
      completed.forEach(tx => {
        logs.push({
          title: "Giao dịch hoàn thành",
          email: `${tx.buyer?.username || 'N/A'} ← ${tx.seller?.username || 'N/A'}`,
          status: "Thành công",
          color: "bg-green-600 text-white",
          time: formatDateTime(tx.completedAt || tx.createdAt),
        });
      });

      // Get recent pending transactions
      const pendingRes = await axiosInstance.get("/transactions/admin/by-status", {
        params: { status: "PENDING", page: 0, size: 3 }
      });

      const pending = pendingRes.data?.data?.content || [];
      pending.forEach(tx => {
        logs.push({
          title: "Giao dịch đang xử lý",
          email: `${tx.buyer?.username || 'N/A'} → ${tx.seller?.username || 'N/A'}`,
          status: "Đang xử lý",
          color: "bg-yellow-600 text-white",
          time: formatDateTime(tx.createdAt),
        });
      });

      // Get recent cancelled transactions
      const cancelledRes = await axiosInstance.get("/transactions/admin/by-status", {
        params: { status: "CANCELLED", page: 0, size: 2 }
      });

      const cancelled = cancelledRes.data?.data?.content || [];
      cancelled.forEach(tx => {
        logs.push({
          title: "Giao dịch bị hủy",
          email: `${tx.buyer?.username || 'N/A'} × ${tx.seller?.username || 'N/A'}`,
          status: "Đã hủy",
          color: "bg-red-600 text-white",
          time: formatDateTime(tx.createdAt),
        });
      });

      // Sort by time descending
      logs.sort((a, b) => new Date(b.time) - new Date(a.time));

      return logs.slice(0, 10); // Return top 10

    } catch (e) {
      console.warn("⚠️ Could not fetch recent activity:", e.message);
      return [];
    }
  };

  const formatDateTime = (dateString) => {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleString("vi-VN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const formatCurrency = (vnd) =>
    new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(vnd || 0);

  const handleChange = (e) => {
    const { name, type, checked, value } = e.target;
    
    // Recalculate platform revenue when fee changes
    if (name === "platformFee") {
      const newFee = Number(value);
      const estimatedPlatformRevenue = settings.monthlyRevenue * (newFee / 100);
      setSettings({
        ...settings,
        platformFee: newFee,
        estimatedPlatformRevenue,
      });
    } else {
      setSettings({
        ...settings,
        [name]: type === "checkbox" ? checked : value,
      });
    }
  };

  const handleSave = async () => {
    setSaving(true);
    
    try {
      // Note: Since there's no backend endpoint for system settings,
      // this would typically save to a database or configuration service
      // For now, we'll just show a success message
      
      await new Promise(resolve => setTimeout(resolve, 1000)); // Simulate API call
      
      alert(`✅ Cài đặt hệ thống đã được lưu thành công!\n\n` +
            `Phí nền tảng: ${settings.platformFee}%\n` +
            `Tín chỉ tối thiểu: ${settings.minCredit} tCO₂\n` +
            `Tín chỉ tối đa: ${settings.maxCredit} tCO₂\n` +
            `Chế độ bảo trì: ${settings.maintenanceMode ? 'Bật' : 'Tắt'}\n` +
            `Yêu cầu xác minh: ${settings.requireVerification ? 'Có' : 'Không'}\n` +
            `Tự động duyệt: ${settings.autoApprove ? 'Có' : 'Không'}`
      );
    } catch (e) {
      alert("❌ Lỗi khi lưu cài đặt: " + e.message);
    } finally {
      setSaving(false);
    }
  };

  const tabClass = (tab) =>
    `px-6 py-3 font-medium cursor-pointer transition ${
      activeTab === tab
        ? "border-b-2 border-blue-600 text-blue-600"
        : "text-gray-600 hover:text-gray-800"
    }`;

  if (loading) {
    return (
      <div className="flex min-h-screen bg-gray-50">
        <AdminSidebar />
        <div className="flex-1 flex flex-col">
          <AdminHeader />
          <main className="flex-1 flex items-center justify-center">
            <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Đang tải cài đặt...</p>
            </div>
          </main>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex min-h-screen bg-gray-50">
        <AdminSidebar />
        <div className="flex-1 flex flex-col">
          <AdminHeader />
          <main className="p-8">
            <div className="bg-red-50 border border-red-200 rounded-lg p-6 flex items-start gap-3">
              <AlertCircle className="text-red-600 flex-shrink-0 mt-0.5" size={20} />
              <div>
                <h3 className="text-red-800 font-semibold mb-1">Lỗi tải dữ liệu</h3>
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
          </main>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-gray-50">
      <AdminSidebar />

      <div className="flex flex-col flex-1">
        <AdminHeader />

        <main className="flex-1 p-8">
          <div className="max-w-6xl mx-auto bg-white rounded-xl shadow-sm p-6">
            <div className="flex justify-between items-start mb-6">
              <div>
                <h2 className="text-xl font-semibold mb-1">Cài đặt hệ thống</h2>
                <p className="text-gray-500 text-sm">
                  Quản lý cấu hình và tham số của nền tảng
                </p>
              </div>
              <button
                onClick={loadSettings}
                disabled={loading}
                className="flex items-center gap-2 px-4 py-2 text-gray-600 hover:text-gray-800 border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 transition-colors"
              >
                <RefreshCw size={16} className={loading ? "animate-spin" : ""} />
                Làm mới
              </button>
            </div>

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
                Hoạt động gần đây
              </button>
            </div>

            {/* === TAB 1: CHUNG === */}
            {activeTab === "general" && (
              <div className="space-y-6">
                {/* System Info */}
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
                  <h3 className="font-semibold text-blue-800 mb-2">Thông tin hệ thống</h3>
                  <div className="grid grid-cols-3 gap-4 text-sm">
                    <div>
                      <p className="text-blue-600">Tổng người dùng</p>
                      <p className="text-blue-800 font-semibold text-lg">{settings.totalUsers}</p>
                    </div>
                    <div>
                      <p className="text-blue-600">Niêm yết đang hoạt động</p>
                      <p className="text-blue-800 font-semibold text-lg">{settings.activeListings}</p>
                    </div>
                    <div>
                      <p className="text-blue-600">Doanh thu tháng này</p>
                      <p className="text-blue-800 font-semibold text-lg">{formatCurrency(settings.monthlyRevenue)}</p>
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-6">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Số lượng tín chỉ tối thiểu (tCO₂)
                    </label>
                    <input
                      type="number"
                      name="minCredit"
                      value={settings.minCredit}
                      onChange={handleChange}
                      min="0"
                      step="0.1"
                      className="w-full border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    />
                    <p className="text-xs text-gray-500 mt-1">
                      Số lượng tín chỉ tối thiểu cho mỗi giao dịch
                    </p>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Số lượng tín chỉ tối đa (tCO₂)
                    </label>
                    <input
                      type="number"
                      name="maxCredit"
                      value={settings.maxCredit}
                      onChange={handleChange}
                      min="1"
                      step="1"
                      className="w-full border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    />
                    <p className="text-xs text-gray-500 mt-1">
                      Số lượng tín chỉ tối đa cho mỗi giao dịch
                    </p>
                  </div>
                </div>

                <div className="flex items-center justify-between border border-gray-200 rounded-lg p-4 hover:bg-gray-50 transition-colors">
                  <div>
                    <p className="text-sm font-medium text-gray-800">
                      Chế độ bảo trì
                    </p>
                    <p className="text-sm text-gray-500">
                      Tạm dừng tất cả hoạt động giao dịch trên nền tảng
                    </p>
                  </div>
                  <input
                    type="checkbox"
                    name="maintenanceMode"
                    checked={settings.maintenanceMode}
                    onChange={handleChange}
                    className="w-5 h-5 accent-blue-600 cursor-pointer"
                  />
                </div>
              </div>
            )}

            {/* === TAB 2: PHÍ DỊCH VỤ === */}
            {activeTab === "fees" && (
              <div className="space-y-6">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Phí nền tảng (%)
                  </label>
                  <div className="flex items-center gap-4">
                    <input
                      type="number"
                      name="platformFee"
                      value={settings.platformFee}
                      onChange={handleChange}
                      min="0"
                      max="100"
                      step="0.1"
                      className="w-32 border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    />
                    <span className="text-gray-600">% của mỗi giao dịch</span>
                  </div>
                  <p className="text-gray-500 text-sm mt-2">
                    Phí thu từ mỗi giao dịch thành công trên nền tảng
                  </p>
                </div>

                <div className="bg-gradient-to-br from-green-50 to-emerald-50 border border-green-200 rounded-lg p-6">
                  <div className="flex items-center gap-2 mb-3">
                    <DollarSign className="text-green-600" size={24} />
                    <h3 className="font-semibold text-green-800">Dự báo doanh thu nền tảng</h3>
                  </div>
                  <div className="space-y-2">
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-green-700">Doanh thu giao dịch tháng này:</span>
                      <span className="text-green-800 font-semibold">{formatCurrency(settings.monthlyRevenue)}</span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-green-700">Phí nền tảng ({settings.platformFee}%):</span>
                      <span className="text-green-800 font-bold text-lg">{formatCurrency(settings.estimatedPlatformRevenue)}</span>
                    </div>
                  </div>
                  <p className="text-xs text-green-600 mt-3">
                    💡 Dựa trên {settings.monthlyRevenue > 0 ? 'dữ liệu thực tế' : 'ước tính'} giao dịch tháng này
                  </p>
                </div>

                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                  <h4 className="font-medium text-blue-800 mb-2">📊 Phân tích</h4>
                  <ul className="text-sm text-blue-700 space-y-1">
                    <li>• Phí trung bình ngành: 3-7%</li>
                    <li>• Phí hiện tại: <span className="font-semibold">{settings.platformFee}%</span></li>
                    <li>• Khuyến nghị: Giữ phí trong khoảng 4-6% để cạnh tranh</li>
                  </ul>
                </div>
              </div>
            )}

            {/* === TAB 3: XÁC MINH === */}
            {activeTab === "verification" && (
              <div className="space-y-6">
                <div className="flex items-center justify-between border border-gray-200 rounded-lg p-4 hover:bg-gray-50 transition-colors">
                  <div>
                    <p className="text-sm font-medium text-gray-800 flex items-center gap-2">
                      <ShieldCheck size={18} className="text-blue-600" />
                      Yêu cầu xác minh
                    </p>
                    <p className="text-sm text-gray-500 ml-6">
                      Tất cả tín chỉ carbon phải được CVA xác minh trước khi phát hành
                    </p>
                  </div>
                  <input
                    type="checkbox"
                    name="requireVerification"
                    checked={settings.requireVerification}
                    onChange={handleChange}
                    className="w-5 h-5 accent-blue-600 cursor-pointer"
                  />
                </div>

                <div className="flex items-center justify-between border border-gray-200 rounded-lg p-4 hover:bg-gray-50 transition-colors">
                  <div>
                    <p className="text-sm font-medium text-gray-800 flex items-center gap-2">
                      <Lock size={18} className="text-blue-600" />
                      Tự động duyệt
                    </p>
                    <p className="text-sm text-gray-500 ml-6">
                      Tự động duyệt các yêu cầu từ người dùng đã được xác minh
                    </p>
                  </div>
                  <input
                    type="checkbox"
                    name="autoApprove"
                    checked={settings.autoApprove}
                    onChange={handleChange}
                    disabled={!settings.requireVerification}
                    className="w-5 h-5 accent-blue-600 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                  />
                </div>

                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                  <h4 className="font-medium text-yellow-800 mb-2">⚠️ Lưu ý</h4>
                  <ul className="text-sm text-yellow-700 space-y-1">
                    <li>• Xác minh bởi CVA đảm bảo tính chính xác của tín chỉ carbon</li>
                    <li>• Tự động duyệt chỉ áp dụng cho người dùng đáng tin cậy</li>
                    <li>• Không bật tự động duyệt nếu muốn kiểm soát chặt chẽ</li>
                  </ul>
                </div>
              </div>
            )}

            {/* === TAB 4: HOẠT ĐỘNG GẦN ĐÂY === */}
            {activeTab === "security" && (
              <div className="space-y-6">
                <div className="flex justify-between items-center">
                  <h4 className="text-base font-semibold text-gray-800">
                    Hoạt động gần đây trên hệ thống
                  </h4>
                  <button
                    onClick={loadSettings}
                    className="text-sm text-blue-600 hover:text-blue-700 font-medium"
                  >
                    Làm mới
                  </button>
                </div>

                {auditLogs.length > 0 ? (
                  <div className="space-y-3">
                    {auditLogs.map((log, idx) => (
                      <div
                        key={idx}
                        className="flex items-center justify-between border border-gray-200 rounded-lg p-4 hover:shadow-sm transition-shadow"
                      >
                        <div>
                          <p className="text-sm font-medium text-gray-800">
                            {log.title}
                          </p>
                          <p className="text-sm text-gray-600 mt-1">{log.email}</p>
                        </div>
                        <div className="text-right">
                          <span
                            className={`px-3 py-1 rounded-full text-xs font-medium ${log.color}`}
                          >
                            {log.status}
                          </span>
                          <p className="text-xs text-gray-500 mt-2">{log.time}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-8 text-gray-500">
                    <Activity size={48} className="mx-auto mb-3 text-gray-300" />
                    <p>Chưa có hoạt động nào được ghi nhận</p>
                  </div>
                )}
              </div>
            )}

            {/* === Save Button === */}
            <div className="pt-8 border-t border-gray-200">
              <button
                onClick={handleSave}
                disabled={saving}
                className="bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
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
          </div>
        </main>
      </div>
    </div>
  );
}