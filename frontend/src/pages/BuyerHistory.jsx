import React, { useState, useEffect } from "react";
import SidebarBuyer from "../components/BuyerSidebar";
import Header from "../components/BuyerHeader";
import { ShoppingCart, Download, TrendingUp, Leaf, DollarSign, AlertCircle } from "lucide-react";
import axiosInstance from "../api/axiosInstance";

export default function BuyerHistory() {
  const [activeTab, setActiveTab] = useState("purchase");
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [stats, setStats] = useState({
    totalCredits: 0,
    totalCost: 0,
    averagePrice: 0,
    totalTransactions: 0,
    co2Offset: 0
  });

  useEffect(() => {
    loadTransactions();
  }, [page]);

  useEffect(() => {
    if (transactions.length > 0) {
      calculateStats();
    }
  }, [transactions]);

  const loadTransactions = async () => {
    setLoading(true);
    setError("");

    try {
      const response = await axiosInstance.get("/transactions/my-history", {
        params: { page, size: 10 }
      });

      console.log("📊 Transaction History Response:", response.data);

      if (response.data?.data) {
        const pageData = response.data.data;
        setTransactions(pageData.content || []);
        setTotalPages(pageData.totalPages || 0);
      }
    } catch (e) {
      console.error("❌ Error loading transaction history:", e.response?.data || e.message);
      setError(e.response?.data?.message || "Không thể tải lịch sử giao dịch.");
    } finally {
      setLoading(false);
    }
  };

  const calculateStats = () => {
    // Calculate statistics from all transactions
    let totalCredits = 0;
    let totalCost = 0;
    let transactionCount = 0;

    transactions.forEach(tx => {
      if (tx.status === "COMPLETED") {
        const creditAmount = Number(tx.carbonCreditsAmount || tx.credit?.creditAmount || 0);
        const price = Number(tx.totalPrice || tx.amount || 0);
        
        totalCredits += creditAmount;
        totalCost += price;
        transactionCount++;
      }
    });

    const averagePrice = transactionCount > 0 ? totalCost / totalCredits : 0;
    const co2Offset = totalCredits * 1000; // Convert tCO2 to kg

    setStats({
      totalCredits,
      totalCost,
      averagePrice,
      totalTransactions: transactionCount,
      co2Offset
    });
  };

  const formatCurrency = (vnd) =>
    new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(vnd || 0);

  const formatDate = (s) =>
    s ? new Date(s).toLocaleDateString("vi-VN", { year: "numeric", month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" }) : "N/A";

  const getStatusBadge = (status) => {
    const statusMap = {
      COMPLETED: { label: "Hoàn thành", class: "bg-green-600 text-white" },
      PENDING: { label: "Đang xử lý", class: "bg-yellow-500 text-white" },
      FAILED: { label: "Thất bại", class: "bg-red-600 text-white" },
      CANCELLED: { label: "Đã hủy", class: "bg-gray-500 text-white" }
    };
    const config = statusMap[status] || { label: status, class: "bg-gray-200 text-gray-700" };
    return (
      <span className={`text-xs font-semibold px-3 py-1 rounded-full ${config.class}`}>
        {config.label}
      </span>
    );
  };

  const getCompletedTransactions = () => {
    return transactions.filter(tx => tx.status === "COMPLETED");
  };

  const calculateTreesEquivalent = (co2Kg) => {
    // Average tree absorbs ~22 kg of CO2 per year
    return Math.round(co2Kg / 22);
  };

  const calculateCarbonNeutrality = () => {
    // This is a simplified calculation
    // In reality, you'd need user's total emissions data
    // Assuming average person emits 4 tons CO2/year in Vietnam
    const averageAnnualEmissions = 4; // tons
    const userOffsetTons = stats.totalCredits; // already in tons
    const neutralityPercentage = Math.min(100, (userOffsetTons / averageAnnualEmissions) * 100);
    return neutralityPercentage.toFixed(1);
  };

  if (loading && transactions.length === 0) {
    return (
      <div className="flex min-h-screen bg-[#F9FAFB]">
        <SidebarBuyer />
        <div className="flex flex-col flex-1">
          <Header />
          <main className="flex-1 flex items-center justify-center">
            <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Đang tải lịch sử...</p>
            </div>
          </main>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-[#F9FAFB]">
      <SidebarBuyer />

      <div className="flex flex-col flex-1">
        <Header />

        <main className="p-8 w-full">
          {/* Page Title */}
          <div className="mb-6">
            <h1 className="text-2xl font-semibold text-gray-800">
              Lịch sử hoạt động
            </h1>
            <p className="text-gray-500 text-sm mt-1">
              Tổng quan về tất cả hoạt động mua tín chỉ carbon
            </p>
          </div>

          {/* Error Message */}
          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2">
              <AlertCircle className="text-red-600 w-5 h-5" />
              <span className="text-sm text-red-700">{error}</span>
            </div>
          )}

          {/* Tabs */}
          <div className="bg-white rounded-2xl border border-gray-200 p-5">
            <div className="flex bg-gray-100 rounded-full text-sm font-medium text-gray-600 mb-6">
              {["purchase", "certificate", "stats"].map((tab) => (
                <button
                  key={tab}
                  onClick={() => setActiveTab(tab)}
                  className={`flex-1 text-center py-2 rounded-full transition ${
                    activeTab === tab
                      ? "bg-white text-gray-900 shadow font-semibold"
                      : "hover:text-gray-800"
                  }`}
                >
                  {tab === "purchase"
                    ? "Giao dịch mua"
                    : tab === "certificate"
                    ? "Chứng nhận"
                    : "Thống kê"}
                </button>
              ))}
            </div>

            {/* Giao dịch mua */}
            {activeTab === "purchase" && (
              <>
                {loading ? (
                  <div className="text-center py-12">
                    <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-green-600 mx-auto mb-3"></div>
                    <p className="text-gray-500 text-sm">Đang tải...</p>
                  </div>
                ) : transactions.length === 0 ? (
                  <div className="text-center py-12">
                    <ShoppingCart className="w-16 h-16 text-gray-400 mx-auto mb-3" />
                    <p className="text-gray-500">Chưa có giao dịch nào</p>
                    <p className="text-gray-400 text-sm mt-1">Bắt đầu mua tín chỉ carbon ngay!</p>
                  </div>
                ) : (
                  <>
                    <div className="space-y-3">
                      {transactions.map((tx) => {
                        const creditAmount = Number(tx.carbonCreditsAmount || tx.credit?.creditAmount || 0);
                        const totalPrice = Number(tx.totalPrice || tx.amount || 0);
                        const sellerName = tx.seller?.username || "N/A";

                        return (
                          <div
                            key={tx.id}
                            className="flex items-center justify-between border border-gray-200 rounded-xl px-4 py-3 bg-white hover:bg-gray-50 transition"
                          >
                            <div className="flex items-center gap-3">
                              <ShoppingCart size={18} className="text-blue-500" />
                              <div>
                                <p className="text-sm font-medium text-gray-800">
                                  {creditAmount.toFixed(2)} tCO₂ từ {sellerName}
                                </p>
                                <p className="text-xs text-gray-500">
                                  {formatDate(tx.createdAt)}
                                </p>
                                {tx.listing && (
                                  <p className="text-xs text-gray-400">
                                    Listing ID: {tx.listing.listingId?.toString().substring(0, 8)}...
                                  </p>
                                )}
                              </div>
                            </div>

                            <div className="flex items-center gap-3">
                              <div className="text-right">
                                <p className="text-sm font-medium text-gray-800">
                                  {formatCurrency(totalPrice)}
                                </p>
                                {tx.completedAt && (
                                  <p className="text-xs text-gray-400">
                                    Hoàn tất: {formatDate(tx.completedAt)}
                                  </p>
                                )}
                              </div>
                              {getStatusBadge(tx.status)}
                            </div>
                          </div>
                        );
                      })}
                    </div>

                    {/* Pagination */}
                    {totalPages > 1 && (
                      <div className="flex items-center justify-center gap-2 mt-6">
                        <button
                          onClick={() => setPage(Math.max(0, page - 1))}
                          disabled={page === 0}
                          className="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          Trước
                        </button>
                        <span className="text-sm text-gray-600">
                          Trang {page + 1} / {totalPages}
                        </span>
                        <button
                          onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                          disabled={page >= totalPages - 1}
                          className="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          Sau
                        </button>
                      </div>
                    )}
                  </>
                )}
              </>
            )}

            {/* Chứng nhận */}
            {activeTab === "certificate" && (
              <>
                {getCompletedTransactions().length === 0 ? (
                  <div className="text-center py-12">
                    <div className="text-6xl mb-3">🏅</div>
                    <p className="text-gray-500">Chưa có chứng nhận nào</p>
                    <p className="text-gray-400 text-sm mt-1">
                      Hoàn tất giao dịch để nhận chứng nhận
                    </p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {getCompletedTransactions().map((tx) => {
                      const creditAmount = Number(tx.carbonCreditsAmount || tx.credit?.creditAmount || 0);
                      
                      return (
                        <div
                          key={tx.id}
                          className="flex items-center justify-between border border-gray-200 rounded-xl px-4 py-3 bg-white hover:bg-gray-50 transition"
                        >
                          <div className="flex items-center gap-3">
                            <div className="text-green-600 font-semibold text-2xl">🏅</div>
                            <div>
                              <p className="text-sm font-medium text-gray-800">
                                Chứng nhận #{tx.id.toString().substring(0, 8)} – {creditAmount.toFixed(2)} tCO₂
                              </p>
                              <p className="text-xs text-gray-500">
                                Cấp ngày: {formatDate(tx.completedAt || tx.createdAt)}
                              </p>
                              <p className="text-xs text-gray-400">
                                Từ: {tx.seller?.username || "N/A"}
                              </p>
                            </div>
                          </div>

                          <button
                            onClick={() => {
                              // Implement download functionality
                              alert(`Tải xuống chứng nhận ${tx.id}\nTính năng này sẽ được phát triển.`);
                            }}
                            className="flex items-center gap-1 text-sm font-medium text-gray-700 border border-gray-300 px-3 py-1.5 rounded-lg hover:bg-gray-100"
                          >
                            <Download size={16} /> Tải xuống
                          </button>
                        </div>
                      );
                    })}
                  </div>
                )}
              </>
            )}

            {/* Thống kê */}
            {activeTab === "stats" && (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                {/* Tổng quan mua sắm */}
                <div className="border border-gray-200 rounded-xl bg-gradient-to-br from-blue-50 to-white p-5">
                  <div className="flex items-center gap-2 mb-3">
                    <DollarSign className="text-blue-600 w-5 h-5" />
                    <h3 className="font-semibold text-gray-800">Tổng quan mua sắm</h3>
                  </div>
                  <div className="space-y-2">
                    <div className="flex justify-between items-center">
                      <p className="text-sm text-gray-700">Tổng tín chỉ đã mua:</p>
                      <span className="font-semibold text-gray-900">
                        {stats.totalCredits.toFixed(2)} tCO₂
                      </span>
                    </div>
                    <div className="flex justify-between items-center">
                      <p className="text-sm text-gray-700">Tổng chi phí:</p>
                      <span className="font-semibold text-gray-900">
                        {formatCurrency(stats.totalCost)}
                      </span>
                    </div>
                    <div className="flex justify-between items-center">
                      <p className="text-sm text-gray-700">Giá trung bình:</p>
                      <span className="font-semibold text-gray-900">
                        {formatCurrency(stats.averagePrice)}/tCO₂
                      </span>
                    </div>
                    <div className="flex justify-between items-center">
                      <p className="text-sm text-gray-700">Số giao dịch:</p>
                      <span className="font-semibold text-gray-900">
                        {stats.totalTransactions}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Tác động môi trường */}
                <div className="border border-gray-200 rounded-xl bg-gradient-to-br from-green-50 to-white p-5">
                  <div className="flex items-center gap-2 mb-3">
                    <Leaf className="text-green-600 w-5 h-5" />
                    <h3 className="font-semibold text-gray-800">Tác động môi trường</h3>
                  </div>
                  <div className="space-y-2">
                    <div className="flex justify-between items-center">
                      <p className="text-sm text-gray-700">CO₂ offset:</p>
                      <span className="font-semibold text-green-600">
                        {stats.co2Offset.toLocaleString()} kg
                      </span>
                    </div>
                    <div className="flex justify-between items-center">
                      <p className="text-sm text-gray-700">Mức trung hòa carbon:</p>
                      <span className="font-semibold text-green-600">
                        {calculateCarbonNeutrality()}%
                      </span>
                    </div>
                    <div className="flex justify-between items-center">
                      <p className="text-sm text-gray-700">Tương đương cây trồng:</p>
                      <span className="font-semibold text-green-600">
                        {calculateTreesEquivalent(stats.co2Offset).toLocaleString()} cây
                      </span>
                    </div>
                    <div className="flex justify-between items-center">
                      <p className="text-sm text-gray-700">Tổng tín chỉ:</p>
                      <span className="font-semibold text-green-600">
                        {stats.totalCredits.toFixed(2)} tCO₂
                      </span>
                    </div>
                  </div>
                </div>

                {/* Visual Impact Chart */}
                <div className="col-span-full border border-gray-200 rounded-xl bg-white p-5">
                  <div className="flex items-center gap-2 mb-3">
                    <TrendingUp className="text-purple-600 w-5 h-5" />
                    <h3 className="font-semibold text-gray-800">Tác động của bạn</h3>
                  </div>
                  <div className="grid grid-cols-3 gap-4 text-center">
                    <div className="bg-blue-50 rounded-lg p-4">
                      <p className="text-3xl font-bold text-blue-600">
                        {stats.totalTransactions}
                      </p>
                      <p className="text-xs text-gray-600 mt-1">Giao dịch hoàn thành</p>
                    </div>
                    <div className="bg-green-50 rounded-lg p-4">
                      <p className="text-3xl font-bold text-green-600">
                        {(stats.co2Offset / 1000).toFixed(1)}
                      </p>
                      <p className="text-xs text-gray-600 mt-1">Tấn CO₂ giảm thiểu</p>
                    </div>
                    <div className="bg-purple-50 rounded-lg p-4">
                      <p className="text-3xl font-bold text-purple-600">
                        {calculateTreesEquivalent(stats.co2Offset).toLocaleString()}
                      </p>
                      <p className="text-xs text-gray-600 mt-1">Cây tương đương</p>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}