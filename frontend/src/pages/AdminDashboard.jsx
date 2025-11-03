import React, { useState, useEffect } from "react";
import AdminSidebar from "../components/AdminSidebar";
import AdminHeader from "../components/AdminHeader";
import { Users, TrendingUp, DollarSign, Activity, AlertCircle, RefreshCw, ShoppingCart, Leaf } from "lucide-react";
import axiosInstance from "../api/axiosInstance";

export default function AdminDashboard() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [stats, setStats] = useState({
    totalUsers: 0,
    activeUsers: 0,
    totalTransactions: 0,
    totalTransactionValue: 0,
    totalCredits: 0,
    totalListings: 0,
    averagePrice: 0,
    usersByRole: {
      EV_OWNER: 0,
      BUYER: 0,
      CVA: 0,
      ADMIN: 0
    }
  });
  const [recentActivity, setRecentActivity] = useState([]);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    setLoading(true);
    setError("");

    try {
      // Fetch users data
      const usersRes = await axiosInstance.get("/users");
      console.log("👥 Users:", usersRes.data);

      // Process users data
      const users = usersRes.data?.data || [];
      const usersByRole = users.reduce((acc, user) => {
        acc[user.role] = (acc[user.role] || 0) + 1;
        return acc;
      }, { EV_OWNER: 0, BUYER: 0, CVA: 0, ADMIN: 0 });

      // Fetch transactions
      let transactions = [];
      let completedTransactions = [];
      let totalTransactionValue = 0;
      
      try {
        const transactionsRes = await axiosInstance.get("/transactions/admin/by-status", { 
          params: { status: "COMPLETED", page: 0, size: 100 } 
        });
        console.log("💰 Completed Transactions:", transactionsRes.data);
        
        if (transactionsRes.data?.data?.content) {
          completedTransactions = transactionsRes.data.data.content;
          totalTransactionValue = completedTransactions.reduce((sum, tx) => 
            sum + Number(tx.totalPrice || tx.amount || 0), 0
          );
        }
      } catch (txError) {
        console.warn("⚠️ Could not fetch completed transactions:", txError.message);
      }

      // Get transaction counts by status (exclude FAILED as it returns 400)
      let allTransactionsCount = 0;
      const statuses = ['COMPLETED', 'PENDING', 'CANCELLED'];
      
      for (const status of statuses) {
        try {
          const statusRes = await axiosInstance.get("/transactions/admin/by-status", {
            params: { status, page: 0, size: 1 }
          });
          if (statusRes.data?.data?.totalElements) {
            allTransactionsCount += statusRes.data.data.totalElements;
            
            // Add recent transactions to activity
            if (statusRes.data.data.content) {
              transactions = [...transactions, ...statusRes.data.data.content.slice(0, 3)];
            }
          }
        } catch (e) {
          console.warn(`⚠️ Could not fetch ${status} transactions:`, e.message);
        }
      }

      // Calculate total credits from all users' credits (admin doesn't own credits)
      // We'll estimate from listings instead since admin can't access credit endpoints
      let totalCreditAmount = 0;

      // Fetch listings stats
      let listingsStats = {};
      try {
        const listingsStatsRes = await axiosInstance.get("/listings/stats");
        console.log("📊 Listings Stats:", listingsStatsRes.data);
        listingsStats = listingsStatsRes.data?.data || {};
        
        // Estimate total credits from active listings (this is an approximation)
        // In production, you'd want a dedicated admin endpoint that aggregates all credits
        const listingsRes = await axiosInstance.get("/listings", {
          params: { page: 0, size: 100 }
        });
        
        if (listingsRes.data?.data?.content) {
          const listings = listingsRes.data.data.content;
          totalCreditAmount = listings.reduce((sum, listing) => 
            sum + Number(listing.creditAmount || 0), 0
          );
        }
      } catch (e) {
        console.warn("⚠️ Could not fetch listings stats:", e.message);
      }

      // Generate recent activity from transactions
      const activity = transactions
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
        .slice(0, 10)
        .map(tx => ({
          label: `Giao dịch ${tx.id.toString().substring(0, 8)}... - ${getStatusLabel(tx.status)}`,
          time: getTimeAgo(tx.createdAt),
          color: getActivityColor(tx.status),
          type: 'transaction',
          data: tx
        }));

      // Add user registration activity
      const recentUsers = users
        .filter(u => u.createdAt)
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
        .slice(0, 5)
        .map(user => ({
          label: `Người dùng mới: ${user.username} (${user.role})`,
          time: getTimeAgo(user.createdAt),
          color: 'bg-blue-500',
          type: 'user',
          data: user
        }));

      const combinedActivity = [...activity, ...recentUsers]
        .sort((a, b) => {
          const dateA = a.data.createdAt ? new Date(a.data.createdAt) : new Date(0);
          const dateB = b.data.createdAt ? new Date(b.data.createdAt) : new Date(0);
          return dateB - dateA;
        })
        .slice(0, 10);

      setStats({
        totalUsers: users.length,
        activeUsers: users.filter(u => u.status === "ACTIVE" || !u.status).length,
        totalTransactions: allTransactionsCount || completedTransactions.length,
        completedTransactions: completedTransactions.length,
        totalTransactionValue,
        totalCredits: totalCreditAmount,
        totalListings: listingsStats.totalActiveListings || 0,
        averagePrice: Number(listingsStats.averagePrice || 0),
        usersByRole
      });

      setRecentActivity(combinedActivity);

    } catch (e) {
      console.error("❌ Error loading dashboard data:", e.response?.data || e.message);
      setError(e.response?.data?.message || "Không thể tải dữ liệu dashboard.");
    } finally {
      setLoading(false);
    }
  };

  const getTimeAgo = (dateString) => {
    if (!dateString) return "N/A";
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return "Vừa xong";
    if (diffMins < 60) return `${diffMins} phút trước`;
    if (diffHours < 24) return `${diffHours} giờ trước`;
    if (diffDays < 30) return `${diffDays} ngày trước`;
    return date.toLocaleDateString('vi-VN');
  };

  const getActivityColor = (status) => {
    const colorMap = {
      COMPLETED: "bg-green-500",
      PENDING: "bg-yellow-500",
      FAILED: "bg-red-500",
      CANCELLED: "bg-gray-500"
    };
    return colorMap[status] || "bg-blue-500";
  };

  const getStatusLabel = (status) => {
    const labelMap = {
      COMPLETED: "Hoàn thành",
      PENDING: "Đang xử lý",
      FAILED: "Thất bại",
      CANCELLED: "Đã hủy"
    };
    return labelMap[status] || status;
  };

  const formatCurrency = (vnd) =>
    new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(vnd || 0);

  const formatNumber = (num) =>
    new Intl.NumberFormat("vi-VN").format(num || 0);

  if (loading) {
    return (
      <div className="flex min-h-screen w-screen bg-gray-50 overflow-hidden">
        <AdminSidebar />
        <div className="flex flex-col flex-1 min-h-screen w-full">
          <AdminHeader />
          <main className="flex-1 flex items-center justify-center">
            <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Đang tải dữ liệu dashboard...</p>
            </div>
          </main>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen w-screen bg-gray-50 overflow-hidden">
      <AdminSidebar />

      <div className="flex flex-col flex-1 min-h-screen w-full">
        <AdminHeader />

        <main className="flex-1 p-8 w-full bg-gray-50 overflow-y-auto">
          {/* Error Message */}
          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2">
              <AlertCircle className="text-red-600 w-5 h-5" />
              <span className="text-sm text-red-700">{error}</span>
              <button
                onClick={loadDashboardData}
                className="ml-auto text-sm text-red-600 hover:text-red-700 underline"
              >
                Thử lại
              </button>
            </div>
          )}

          <div className="space-y-8 w-full">
            {/* Header with Refresh */}
            <div className="flex items-center justify-between">
              <h1 className="text-2xl font-semibold text-gray-800">Dashboard Quản trị</h1>
              <button
                onClick={loadDashboardData}
                className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition"
              >
                <RefreshCw className="w-4 h-4" />
                <span className="text-sm">Tải lại</span>
              </button>
            </div>

            {/* Top Statistic Cards */}
            <div className="grid grid-cols-4 gap-6">
              <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm hover:shadow-md transition">
                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-sm font-medium text-gray-500">Tổng người dùng</h3>
                  <Users className="w-5 h-5 text-blue-500" />
                </div>
                <p className="text-3xl font-semibold text-gray-800">{formatNumber(stats.totalUsers)}</p>
                <p className="text-sm text-gray-400 mt-1">
                  {formatNumber(stats.activeUsers)} đang hoạt động
                </p>
              </div>

              <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm hover:shadow-md transition">
                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-sm font-medium text-gray-500">Giao dịch hoàn thành</h3>
                  <ShoppingCart className="w-5 h-5 text-green-500" />
                </div>
                <p className="text-3xl font-semibold text-gray-800">{formatNumber(stats.completedTransactions)}</p>
                <p className="text-sm text-gray-400 mt-1">
                  {stats.totalTransactions > stats.completedTransactions 
                    ? `${formatNumber(stats.totalTransactions)} tổng cộng` 
                    : 'Đã hoàn tất'}
                </p>
              </div>

              <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm hover:shadow-md transition">
                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-sm font-medium text-gray-500">Giá trị giao dịch</h3>
                  <DollarSign className="w-5 h-5 text-orange-500" />
                </div>
                <p className="text-3xl font-semibold text-gray-800">
                  {stats.totalTransactionValue > 1000000 
                    ? `${(stats.totalTransactionValue / 1000000).toFixed(1)}M`
                    : formatNumber(stats.totalTransactionValue)}
                </p>
                <p className="text-sm text-gray-400 mt-1">
                  {formatCurrency(stats.totalTransactionValue)}
                </p>
              </div>

              <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm hover:shadow-md transition">
                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-sm font-medium text-gray-500">Listings hoạt động</h3>
                  <Leaf className="w-5 h-5 text-green-600" />
                </div>
                <p className="text-3xl font-semibold text-gray-800">
                  {formatNumber(stats.totalListings)}
                </p>
                <p className="text-sm text-gray-400 mt-1">
                  {stats.totalCredits.toFixed(1)} tCO₂ đang giao dịch
                </p>
              </div>
            </div>

            {/* Activity & Stats */}
            <div className="grid grid-cols-2 gap-6">
              {/* Recent Activity */}
              <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
                <div className="flex items-center gap-2 mb-1">
                  <Activity className="w-5 h-5 text-blue-600" />
                  <h2 className="font-semibold text-lg text-gray-800">Hoạt động gần đây</h2>
                </div>
                <p className="text-gray-500 text-sm mb-4">Các sự kiện quan trọng trên hệ thống</p>

                {recentActivity.length === 0 ? (
                  <div className="text-center py-8">
                    <Activity className="w-12 h-12 text-gray-400 mx-auto mb-3" />
                    <p className="text-gray-500 text-sm">Chưa có hoạt động</p>
                  </div>
                ) : (
                  <div className="space-y-4 max-h-96 overflow-y-auto">
                    {recentActivity.map((item, i) => (
                      <div key={i} className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 transition">
                        <div className={`w-3 h-3 rounded-full ${item.color} flex-shrink-0`}></div>
                        <div className="flex-1 min-w-0">
                          <p className="font-medium text-gray-700 text-sm truncate">{item.label}</p>
                          <p className="text-xs text-gray-500">{item.time}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Quick Stats */}
              <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
                <div className="flex items-center gap-2 mb-1">
                  <TrendingUp className="w-5 h-5 text-purple-600" />
                  <h2 className="font-semibold text-lg text-gray-800">Thống kê người dùng</h2>
                </div>
                <p className="text-gray-500 text-sm mb-4">Phân bổ theo vai trò</p>

                <div className="space-y-3 text-sm">
                  <div className="flex justify-between items-center p-2 bg-blue-50 rounded-lg">
                    <span className="text-gray-700">Chủ xe điện (EV Owner):</span>
                    <span className="text-blue-600 font-semibold">{formatNumber(stats.usersByRole.EV_OWNER)}</span>
                  </div>
                  <div className="flex justify-between items-center p-2 bg-green-50 rounded-lg">
                    <span className="text-gray-700">Người mua (Buyer):</span>
                    <span className="text-green-600 font-semibold">{formatNumber(stats.usersByRole.BUYER)}</span>
                  </div>
                  <div className="flex justify-between items-center p-2 bg-purple-50 rounded-lg">
                    <span className="text-gray-700">Người xác minh (CVA):</span>
                    <span className="text-purple-600 font-semibold">{formatNumber(stats.usersByRole.CVA)}</span>
                  </div>
                  <div className="flex justify-between items-center p-2 bg-orange-50 rounded-lg">
                    <span className="text-gray-700">Quản trị viên (Admin):</span>
                    <span className="text-orange-600 font-semibold">{formatNumber(stats.usersByRole.ADMIN)}</span>
                  </div>
                  <div className="flex justify-between items-center p-2 bg-gray-50 rounded-lg border-t-2 border-gray-200">
                    <span className="text-gray-700 font-medium">Listings đang hoạt động:</span>
                    <span className="text-gray-800 font-semibold">{formatNumber(stats.totalListings)}</span>
                  </div>
                  <div className="flex justify-between items-center p-2 bg-gray-50 rounded-lg">
                    <span className="text-gray-700 font-medium">Giá trung bình:</span>
                    <span className="text-gray-800 font-semibold">{formatCurrency(stats.averagePrice)}/tCO₂</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Additional Insights */}
            <div className="grid grid-cols-3 gap-6">
              <div className="bg-gradient-to-br from-blue-50 to-white border border-blue-100 rounded-xl p-6 shadow-sm">
                <h3 className="text-sm font-medium text-blue-700 mb-2">Tỷ lệ hoàn thành</h3>
                <p className="text-3xl font-semibold text-blue-600">
                  {stats.totalTransactions > 0 
                    ? ((stats.completedTransactions / stats.totalTransactions) * 100).toFixed(1)
                    : 0}%
                </p>
                <p className="text-sm text-gray-600 mt-1">
                  {formatNumber(stats.completedTransactions)} / {formatNumber(stats.totalTransactions)} giao dịch
                </p>
              </div>

              <div className="bg-gradient-to-br from-green-50 to-white border border-green-100 rounded-xl p-6 shadow-sm">
                <h3 className="text-sm font-medium text-green-700 mb-2">Tín chỉ đang giao dịch</h3>
                <p className="text-3xl font-semibold text-green-600">
                  {stats.totalCredits.toFixed(1)}
                </p>
                <p className="text-sm text-gray-600 mt-1">
                  tCO₂ trên marketplace
                </p>
              </div>

              <div className="bg-gradient-to-br from-purple-50 to-white border border-purple-100 rounded-xl p-6 shadow-sm">
                <h3 className="text-sm font-medium text-purple-700 mb-2">Giá trị trung bình</h3>
                <p className="text-3xl font-semibold text-purple-600">
                  {stats.completedTransactions > 0
                    ? formatCurrency(stats.totalTransactionValue / stats.completedTransactions)
                    : formatCurrency(0)}
                </p>
                <p className="text-sm text-gray-600 mt-1">
                  Mỗi giao dịch
                </p>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}