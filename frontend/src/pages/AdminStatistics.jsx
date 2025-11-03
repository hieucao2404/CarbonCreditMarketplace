import React, { useState, useEffect } from "react";
import AdminSidebar from "../components/AdminSidebar";
import AdminHeader from "../components/AdminHeader";
import { TrendingUp, DollarSign, Users, Package, RefreshCw, AlertCircle, Activity, CheckCircle, Clock, XCircle, Leaf, FileText } from "lucide-react";
import axiosInstance from "../api/axiosInstance";

export default function AdminStatistics() {
  const [activeTab, setActiveTab] = useState("seller");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [stats, setStats] = useState({
    monthlyRevenue: 0,
    creditsTraded: 0,
    activeUsers: 0,
    completionRate: 0,
    previousMonthRevenue: 0,
    previousMonthCredits: 0,
    previousMonthUsers: 0,
    totalCO2Reduced: 0,
    platformRevenue: 0,
    totalUsers: 0
  });
  const [transactionBreakdown, setTransactionBreakdown] = useState({
    completed: 0,
    pending: 0,
    cancelled: 0
  });
  const [listingStats, setListingStats] = useState({
    active: 0,
    totalListings: 0,
    averagePrice: 0
  });
  const [topSellers, setTopSellers] = useState([]);
  const [topBuyers, setTopBuyers] = useState([]);

  useEffect(() => {
    loadStatistics();
  }, []);

  const loadStatistics = async () => {
    setLoading(true);
    setError("");

    try {
      // 1. Fetch completed transactions in smaller batches to avoid 500 error
      let allCompletedTransactions = [];
      let currentPage = 0;
      let hasMore = true;
      const pageSize = 100;

      console.log("📊 Fetching completed transactions...");
      
      while (hasMore && currentPage < 10) { // Limit to 10 pages (1000 transactions max)
        try {
          const transactionsRes = await axiosInstance.get("/transactions/admin/by-status", {
            params: { status: "COMPLETED", page: currentPage, size: pageSize }
          });

          const pageData = transactionsRes.data?.data;
          const transactions = pageData?.content || [];
          
          if (transactions.length > 0) {
            allCompletedTransactions = [...allCompletedTransactions, ...transactions];
            console.log(`📄 Page ${currentPage}: ${transactions.length} transactions`);
          }

          // Check if there are more pages
          hasMore = transactions.length === pageSize && currentPage < (pageData?.totalPages - 1 || 0);
          currentPage++;
        } catch (pageError) {
          console.warn(`⚠️ Error fetching page ${currentPage}:`, pageError.message);
          hasMore = false;
        }
      }

      console.log(`💰 Total Completed Transactions: ${allCompletedTransactions.length}`);

      // 2. Fetch all users
      const usersRes = await axiosInstance.get("/users");
      const allUsers = usersRes.data?.data || [];
      console.log("👥 Total Users:", allUsers.length);

      // 3. Fetch listing statistics
      let listingStatsData = { active: 0, totalListings: 0, averagePrice: 0 };
      try {
        const listingsStatsRes = await axiosInstance.get("/listings/stats");
        const statsData = listingsStatsRes.data?.data;
        if (statsData) {
          listingStatsData = {
            active: statsData.totalActiveListings || 0,
            totalListings: statsData.totalActiveListings || 0,
            averagePrice: Number(statsData.averagePrice || 0)
          };
        }
        console.log("📋 Listing Stats:", listingStatsData);
      } catch (listingError) {
        console.warn("⚠️ Could not fetch listing stats:", listingError.message);
      }

      // 4. Calculate statistics
      const now = new Date();
      const currentMonth = now.getMonth();
      const currentYear = now.getFullYear();
      const previousMonth = currentMonth === 0 ? 11 : currentMonth - 1;
      const previousYear = currentMonth === 0 ? currentYear - 1 : currentYear;

      // Filter transactions by month
      const currentMonthTransactions = allCompletedTransactions.filter(tx => {
        const txDate = new Date(tx.createdAt);
        return txDate.getMonth() === currentMonth && txDate.getFullYear() === currentYear;
      });

      const previousMonthTransactions = allCompletedTransactions.filter(tx => {
        const txDate = new Date(tx.createdAt);
        return txDate.getMonth() === previousMonth && txDate.getFullYear() === previousYear;
      });

      console.log(`📅 Current Month (${currentMonth + 1}/${currentYear}): ${currentMonthTransactions.length} transactions`);
      console.log(`📅 Previous Month (${previousMonth + 1}/${previousYear}): ${previousMonthTransactions.length} transactions`);

      // Calculate monthly revenue
      const monthlyRevenue = currentMonthTransactions.reduce((sum, tx) => 
        sum + Number(tx.totalPrice || tx.amount || 0), 0
      );

      const previousMonthRevenue = previousMonthTransactions.reduce((sum, tx) => 
        sum + Number(tx.totalPrice || tx.amount || 0), 0
      );

      // Calculate credits traded this month
      const creditsTraded = currentMonthTransactions.reduce((sum, tx) => 
        sum + Number(tx.carbonCreditsAmount || 0), 0
      );

      const previousMonthCredits = previousMonthTransactions.reduce((sum, tx) => 
        sum + Number(tx.carbonCreditsAmount || 0), 0
      );

      // Calculate total CO2 reduced (all time)
      const totalCO2Reduced = allCompletedTransactions.reduce((sum, tx) => 
        sum + Number(tx.carbonCreditsAmount || 0), 0
      );

      // Calculate platform revenue (assume 5% fee on all transactions)
      const platformRevenue = monthlyRevenue * 0.05;

      // Calculate active users (users who made transactions this month)
      const activeUserIds = new Set();
      currentMonthTransactions.forEach(tx => {
        if (tx.buyer?.id) activeUserIds.add(tx.buyer.id);
        if (tx.seller?.id) activeUserIds.add(tx.seller.id);
      });
      const activeUsers = activeUserIds.size;

      // Calculate previous month active users
      const previousActiveUserIds = new Set();
      previousMonthTransactions.forEach(tx => {
        if (tx.buyer?.id) previousActiveUserIds.add(tx.buyer.id);
        if (tx.seller?.id) previousActiveUserIds.add(tx.seller.id);
      });
      const previousMonthUsers = previousActiveUserIds.size;

      // Calculate completion rate and transaction breakdown
      const allTransactionsRes = await Promise.all([
        axiosInstance.get("/transactions/admin/by-status", { params: { status: "COMPLETED", page: 0, size: 1 }}),
        axiosInstance.get("/transactions/admin/by-status", { params: { status: "PENDING", page: 0, size: 1 }}),
        axiosInstance.get("/transactions/admin/by-status", { params: { status: "CANCELLED", page: 0, size: 1 }})
      ]);

      const totalCompleted = allTransactionsRes[0].data?.data?.totalElements || 0;
      const totalPending = allTransactionsRes[1].data?.data?.totalElements || 0;
      const totalCancelled = allTransactionsRes[2].data?.data?.totalElements || 0;
      const totalAll = totalCompleted + totalPending + totalCancelled;
      const completionRate = totalAll > 0 ? (totalCompleted / totalAll) * 100 : 0;

      console.log(`✅ Completion Rate: ${completionRate.toFixed(1)}% (${totalCompleted}/${totalAll})`);

      // Set transaction breakdown
      setTransactionBreakdown({
        completed: totalCompleted,
        pending: totalPending,
        cancelled: totalCancelled
      });

      // Set listing stats
      setListingStats(listingStatsData);

      // 5. Calculate top sellers and buyers from all transactions
      const sellerStats = {};
      const buyerStats = {};

      allCompletedTransactions.forEach(tx => {
        // Seller stats
        if (tx.seller?.id) {
          if (!sellerStats[tx.seller.id]) {
            sellerStats[tx.seller.id] = {
              name: tx.seller.fullName || tx.seller.username,
              totalValue: 0,
              totalCO2: 0
            };
          }
          sellerStats[tx.seller.id].totalValue += Number(tx.totalPrice || tx.amount || 0);
          sellerStats[tx.seller.id].totalCO2 += Number(tx.carbonCreditsAmount || 0);
        }

        // Buyer stats
        if (tx.buyer?.id) {
          if (!buyerStats[tx.buyer.id]) {
            buyerStats[tx.buyer.id] = {
              name: tx.buyer.fullName || tx.buyer.username,
              totalValue: 0,
              totalCO2: 0
            };
          }
          buyerStats[tx.buyer.id].totalValue += Number(tx.totalPrice || tx.amount || 0);
          buyerStats[tx.buyer.id].totalCO2 += Number(tx.carbonCreditsAmount || 0);
        }
      });

      // Sort and get top 3 sellers
      const topSellersArray = Object.values(sellerStats)
        .sort((a, b) => b.totalValue - a.totalValue)
        .slice(0, 3)
        .map(s => ({
          name: s.name,
          value: formatCurrency(s.totalValue),
          co2: `${s.totalCO2.toFixed(2)} tCO₂`
        }));

      // Sort and get top 3 buyers
      const topBuyersArray = Object.values(buyerStats)
        .sort((a, b) => b.totalValue - a.totalValue)
        .slice(0, 3)
        .map(b => ({
          name: b.name,
          value: formatCurrency(b.totalValue),
          co2: `${b.totalCO2.toFixed(2)} tCO₂`
        }));

      console.log(`🏆 Top Sellers:`, topSellersArray);
      console.log(`🏆 Top Buyers:`, topBuyersArray);

      // Update state
      setStats({
        monthlyRevenue,
        creditsTraded,
        activeUsers,
        completionRate,
        previousMonthRevenue,
        previousMonthCredits,
        previousMonthUsers,
        totalCO2Reduced,
        platformRevenue,
        totalUsers: allUsers.length
      });

      setTopSellers(topSellersArray);
      setTopBuyers(topBuyersArray);

    } catch (e) {
      console.error("❌ Error loading statistics:", e.response?.data || e.message);
      setError(e.response?.data?.message || "Không thể tải thống kê. Vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (vnd) =>
    new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(vnd || 0);

  const calculatePercentChange = (current, previous) => {
    if (previous === 0) return current > 0 ? 100 : 0;
    return ((current - previous) / previous) * 100;
  };

  const revenueChange = calculatePercentChange(stats.monthlyRevenue, stats.previousMonthRevenue);
  const creditsChange = calculatePercentChange(stats.creditsTraded, stats.previousMonthCredits);
  const usersChange = calculatePercentChange(stats.activeUsers, stats.previousMonthUsers);

  const currentUsers = activeTab === "seller" ? topSellers : topBuyers;

  if (loading) {
    return (
      <div className="flex min-h-screen bg-gray-50">
        <AdminSidebar />
        <div className="flex-1 flex flex-col">
          <AdminHeader />
          <main className="flex-1 flex items-center justify-center">
            <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Đang tải thống kê...</p>
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
                  onClick={loadStatistics}
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
      {/* Sidebar */}
      <AdminSidebar />

      {/* Main content */}
      <div className="flex-1 flex flex-col">
        <AdminHeader />

        <main className="p-8">
          {/* Header with refresh button */}
          <div className="flex justify-between items-center mb-6">
            <div>
              <h1 className="text-2xl font-bold text-gray-800">Báo cáo tổng hợp</h1>
              <p className="text-gray-600 text-sm mt-1">Thống kê giao dịch tín chỉ carbon trên nền tảng</p>
            </div>
            <button
              onClick={loadStatistics}
              disabled={loading}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              <RefreshCw size={18} className={loading ? "animate-spin" : ""} />
              Làm mới
            </button>
          </div>

          {/* === Top summary cards === */}
          <div className="grid grid-cols-4 gap-6 mb-8">
            <div className="bg-white border border-gray-200 rounded-xl p-5">
              <div className="flex items-center gap-2 mb-1">
                <DollarSign size={18} className="text-gray-500" />
                <p className="text-gray-500 text-sm">Doanh thu tháng</p>
              </div>
              <h2 className="text-2xl font-semibold text-gray-800">
                {formatCurrency(stats.monthlyRevenue)}
              </h2>
              <p className={`text-xs mt-1 ${revenueChange >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                {revenueChange >= 0 ? '+' : ''}{revenueChange.toFixed(1)}% so với tháng trước
              </p>
            </div>

            <div className="bg-white border border-gray-200 rounded-xl p-5">
              <div className="flex items-center gap-2 mb-1">
                <Package size={18} className="text-gray-500" />
                <p className="text-gray-500 text-sm">Tín chỉ đã giao dịch</p>
              </div>
              <h2 className="text-2xl font-semibold text-gray-800">
                {stats.creditsTraded.toFixed(2)} tCO₂
              </h2>
              <p className={`text-xs mt-1 ${creditsChange >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                {creditsChange >= 0 ? '+' : ''}{creditsChange.toFixed(1)}% so với tháng trước
              </p>
            </div>

            <div className="bg-white border border-gray-200 rounded-xl p-5">
              <div className="flex items-center gap-2 mb-1">
                <Users size={18} className="text-gray-500" />
                <p className="text-gray-500 text-sm">Người dùng hoạt động</p>
              </div>
              <h2 className="text-2xl font-semibold text-gray-800">
                {stats.activeUsers}
              </h2>
              <p className={`text-xs mt-1 ${usersChange >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                {usersChange >= 0 ? '+' : ''}{usersChange.toFixed(1)}% so với tháng trước
              </p>
            </div>

            <div className="bg-white border border-gray-200 rounded-xl p-5">
              <div className="flex items-center gap-2 mb-1">
                <TrendingUp size={18} className="text-gray-500" />
                <p className="text-gray-500 text-sm">Tỷ lệ hoàn thành</p>
              </div>
              <h2 className="text-2xl font-semibold text-gray-800">
                {stats.completionRate.toFixed(1)}%
              </h2>
              <p className="text-green-600 text-xs mt-1">
                Giao dịch thành công
              </p>
            </div>
          </div>

          {/* === Additional metrics === */}
          <div className="grid grid-cols-3 gap-6 mb-8">
            <div className="bg-gradient-to-br from-green-50 to-emerald-50 border border-green-200 rounded-xl p-5">
              <div className="flex items-center gap-2 mb-1">
                <Leaf size={18} className="text-green-600" />
                <p className="text-green-700 text-sm font-medium">Tổng CO₂ giảm phát thải</p>
              </div>
              <h2 className="text-2xl font-semibold text-green-800">
                {stats.totalCO2Reduced.toFixed(2)} tCO₂
              </h2>
              <p className="text-green-600 text-xs mt-1">
                Tương đương {(stats.totalCO2Reduced * 48).toFixed(0)} cây xanh
              </p>
            </div>

            <div className="bg-gradient-to-br from-blue-50 to-indigo-50 border border-blue-200 rounded-xl p-5">
              <div className="flex items-center gap-2 mb-1">
                <DollarSign size={18} className="text-blue-600" />
                <p className="text-blue-700 text-sm font-medium">Doanh thu nền tảng</p>
              </div>
              <h2 className="text-2xl font-semibold text-blue-800">
                {formatCurrency(stats.platformRevenue)}
              </h2>
              <p className="text-blue-600 text-xs mt-1">
                Phí giao dịch 5% tháng này
              </p>
            </div>

            <div className="bg-gradient-to-br from-purple-50 to-pink-50 border border-purple-200 rounded-xl p-5">
              <div className="flex items-center gap-2 mb-1">
                <FileText size={18} className="text-purple-600" />
                <p className="text-purple-700 text-sm font-medium">Niêm yết hoạt động</p>
              </div>
              <h2 className="text-2xl font-semibold text-purple-800">
                {listingStats.active}
              </h2>
              <p className="text-purple-600 text-xs mt-1">
                Giá TB: {formatCurrency(listingStats.averagePrice)}
              </p>
            </div>
          </div>

          {/* === Bottom section === */}
          <div className="grid grid-cols-2 gap-6">
            {/* Phân bố trạng thái giao dịch */}
            <div className="bg-white border border-gray-200 rounded-xl p-6">
              <h3 className="text-gray-800 font-semibold mb-4 flex items-center gap-2">
                <Activity size={18} />
                Phân bố trạng thái giao dịch
              </h3>
              <div className="space-y-4">
                {/* Completed */}
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <CheckCircle size={18} className="text-green-600" />
                      <span className="text-gray-700 font-medium">Hoàn thành</span>
                    </div>
                    <span className="text-gray-800 font-semibold">{transactionBreakdown.completed}</span>
                  </div>
                  <div className="w-full bg-gray-100 rounded-full h-2">
                    <div 
                      className="bg-green-500 h-2 rounded-full transition-all duration-500"
                      style={{ 
                        width: `${(transactionBreakdown.completed / (transactionBreakdown.completed + transactionBreakdown.pending + transactionBreakdown.cancelled || 1)) * 100}%` 
                      }}
                    ></div>
                  </div>
                </div>

                {/* Pending */}
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <Clock size={18} className="text-yellow-600" />
                      <span className="text-gray-700 font-medium">Đang xử lý</span>
                    </div>
                    <span className="text-gray-800 font-semibold">{transactionBreakdown.pending}</span>
                  </div>
                  <div className="w-full bg-gray-100 rounded-full h-2">
                    <div 
                      className="bg-yellow-500 h-2 rounded-full transition-all duration-500"
                      style={{ 
                        width: `${(transactionBreakdown.pending / (transactionBreakdown.completed + transactionBreakdown.pending + transactionBreakdown.cancelled || 1)) * 100}%` 
                      }}
                    ></div>
                  </div>
                </div>

                {/* Cancelled */}
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <XCircle size={18} className="text-gray-600" />
                      <span className="text-gray-700 font-medium">Đã hủy</span>
                    </div>
                    <span className="text-gray-800 font-semibold">{transactionBreakdown.cancelled}</span>
                  </div>
                  <div className="w-full bg-gray-100 rounded-full h-2">
                    <div 
                      className="bg-gray-400 h-2 rounded-full transition-all duration-500"
                      style={{ 
                        width: `${(transactionBreakdown.cancelled / (transactionBreakdown.completed + transactionBreakdown.pending + transactionBreakdown.cancelled || 1)) * 100}%` 
                      }}
                    ></div>
                  </div>
                </div>

                {/* Total */}
                <div className="pt-3 border-t border-gray-100">
                  <div className="flex items-center justify-between">
                    <span className="text-gray-600 text-sm">Tổng giao dịch</span>
                    <span className="text-gray-800 font-bold">
                      {transactionBreakdown.completed + transactionBreakdown.pending + transactionBreakdown.cancelled}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Top người dùng */}
            <div className="bg-white border border-gray-200 rounded-xl p-6">
              <h3 className="text-gray-800 font-semibold mb-4">Top người dùng</h3>

              {/* Tabs */}
              <div className="flex mb-3 border border-gray-200 rounded-full overflow-hidden w-fit">
                <button
                  onClick={() => setActiveTab("seller")}
                  className={`px-6 py-1.5 text-sm font-medium rounded-full transition-all ${
                    activeTab === "seller"
                      ? "bg-gray-100 text-gray-800"
                      : "text-gray-500 hover:text-gray-700"
                  }`}
                >
                  Người bán
                </button>
                <button
                  onClick={() => setActiveTab("buyer")}
                  className={`px-6 py-1.5 text-sm font-medium rounded-full transition-all ${
                    activeTab === "buyer"
                      ? "bg-gray-100 text-gray-800"
                      : "text-gray-500 hover:text-gray-700"
                  }`}
                >
                  Người mua
                </button>
              </div>

              {/* Danh sách */}
              {currentUsers.length > 0 ? (
                <ul className="divide-y divide-gray-100">
                  {currentUsers.map((u, i) => (
                    <li
                      key={i}
                      className="flex items-center justify-between py-3"
                    >
                      <div className="flex items-center gap-3">
                        <div className="flex items-center justify-center w-8 h-8 rounded-full bg-blue-100 text-blue-600 font-semibold text-sm">
                          {i + 1}
                        </div>
                        <div>
                          <p className="text-gray-800 font-medium">{u.name}</p>
                          <p className="text-xs text-gray-400">{u.co2}</p>
                        </div>
                      </div>
                      <p className="text-gray-800 text-sm font-medium">
                        {u.value}
                      </p>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="text-gray-500 text-sm text-center py-4">
                  Chưa có dữ liệu {activeTab === "seller" ? "người bán" : "người mua"}
                </p>
              )}
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}