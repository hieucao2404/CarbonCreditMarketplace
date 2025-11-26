import React, { useState, useEffect } from "react";
import { Search, RefreshCw, AlertCircle, Eye, FileText, DollarSign } from "lucide-react";
import AdminSidebar from "../components/AdminSidebar";
import AdminHeader from "../components/AdminHeader";
import axiosInstance from "../api/axiosInstance";

const TransactionMoneyBreakdown = ({ transaction, formatCurrency, platformFeePercent = 5 }) => {
  const totalPrice = Number(transaction.totalPrice || transaction.amount || 0);
  const platformFee = totalPrice * (platformFeePercent / 100);
  const sellerReceives = totalPrice - platformFee;

  return (
    <div className="space-y-3">
      {/* Buyer Pays */}
      <div className="flex items-center justify-between bg-red-50 border border-red-200 rounded-lg p-3">
        <div className="flex items-center gap-2">
          <DollarSign className="w-5 h-5 text-red-600" />
          <div>
            <p className="text-xs text-red-600 font-medium">Người mua phải trả</p>
            <p className="text-xs text-red-500">{transaction.buyer?.username || "N/A"}</p>
          </div>
        </div>
        <p className="text-lg font-bold text-red-600">{formatCurrency(totalPrice)}</p>
      </div>

      {/* Seller Receives */}
      <div className="flex items-center justify-between bg-green-50 border border-green-200 rounded-lg p-3">
        <div className="flex items-center gap-2">
          <DollarSign className="w-5 h-5 text-green-600" />
          <div>
            <p className="text-xs text-green-600 font-medium">Người bán nhận được</p>
            <p className="text-xs text-green-500">{transaction.seller?.username || "N/A"}</p>
          </div>
        </div>
        <p className="text-lg font-bold text-green-600">{formatCurrency(sellerReceives)}</p>
      </div>

      {/* Platform Fee */}
      <div className="flex items-center justify-between bg-blue-50 border border-blue-200 rounded-lg p-3">
        <div className="flex items-center gap-2">
          <DollarSign className="w-5 h-5 text-blue-600" />
          <div>
            <p className="text-xs text-blue-600 font-medium">Phí nền tảng ({platformFeePercent}%)</p>
            <p className="text-xs text-blue-500">Carbon Credit Marketplace</p>
          </div>
        </div>
        <p className="text-lg font-bold text-blue-600">{formatCurrency(platformFee)}</p>
      </div>
    </div>
  );
};

export default function AdminTransactionManagement() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selectedTransaction, setSelectedTransaction] = useState(null);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [platformFeePercent, setPlatformFeePercent] = useState(5); // ✅ NEW: State for dynamic fee

  useEffect(() => {
    // ✅ NEW: Fetch platform fee from system settings on component mount
    const fetchPlatformFee = async () => {
      try {
        const response = await axiosInstance.get("/system-settings/public/PLATFORM_FEE_PERCENT");
        if (response.data?.data?.settingValue) {
          const feeValue = parseFloat(response.data.data.settingValue);
          setPlatformFeePercent(feeValue);
          console.log("✅ Platform fee fetched:", feeValue + "%");
        }
      } catch (err) {
        console.warn("⚠️ Could not fetch platform fee, using default 5%", err.message);
        setPlatformFeePercent(5); // Fallback to 5%
      }
    };
    
    fetchPlatformFee();
    loadTransactions();
  }, [statusFilter, page]);

  const loadTransactions = async () => {
    setLoading(true);
    setError("");

    try {
      let response;
      
      if (statusFilter === "ALL") {
        // Fetch all transactions by combining different statuses
        const statuses = ["COMPLETED", "PENDING", "CANCELLED"];
        const allTransactions = [];
        
        for (const status of statuses) {
          try {
            const res = await axiosInstance.get("/transactions/admin/by-status", {
              params: { status, page: 0, size: 100 }
            });
            
            if (res.data?.data?.content) {
              allTransactions.push(...res.data.data.content);
            }
          } catch (e) {
            console.warn(`⚠️ Could not fetch ${status} transactions`);
          }
        }
        
        // Sort by creation date
        allTransactions.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        setTransactions(allTransactions);
        setTotalPages(1);
      } else {
        // Fetch specific status
        response = await axiosInstance.get("/transactions/admin/by-status", {
          params: { status: statusFilter, page, size: 20 }
        });

        console.log("💰 Transactions Response:", response.data);

        if (response.data?.data) {
          const pageData = response.data.data;
          setTransactions(pageData.content || []);
          setTotalPages(pageData.totalPages || 0);
        }
      }
    } catch (e) {
      console.error("❌ Error loading transactions:", e.response?.data || e.message);
      setError(e.response?.data?.message || "Không thể tải danh sách giao dịch.");
    } finally {
      setLoading(false);
    }
  };

  const loadTransactionDetail = async (transactionId) => {
    try {
      const response = await axiosInstance.get(`/transactions/${transactionId}`);
      
      if (response.data?.data) {
        setSelectedTransaction(response.data.data);
        setShowDetailModal(true);
      }
    } catch (e) {
      console.error("❌ Error loading transaction detail:", e.response?.data || e.message);
      alert("Không thể tải chi tiết giao dịch.");
    }
  };

  const getStatusLabel = (status) => {
    const statusMap = {
      COMPLETED: "Hoàn thành",
      PENDING: "Đang xử lý",
      CANCELLED: "Đã hủy",
      FAILED: "Thất bại"
    };
    return statusMap[status] || status;
  };

  const getStatusColor = (status) => {
    const colorMap = {
      COMPLETED: "bg-green-100 text-green-700",
      PENDING: "bg-yellow-100 text-yellow-700",
      CANCELLED: "bg-gray-100 text-gray-700",
      FAILED: "bg-red-100 text-red-700"
    };
    return colorMap[status] || "bg-gray-100 text-gray-600";
  };

  const formatCurrency = (vnd) =>
    new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(vnd || 0);

  const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleDateString("vi-VN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit"
    });
  };

  // Filter transactions by search
  const filteredTransactions = transactions.filter((tx) => {
    if (!search) return true;
    const searchLower = search.toLowerCase();
    return (
      tx.id?.toString().toLowerCase().includes(searchLower) ||
      tx.buyer?.username?.toLowerCase().includes(searchLower) ||
      tx.seller?.username?.toLowerCase().includes(searchLower)
    );
  });

  if (loading && transactions.length === 0) {
    return (
      <div className="flex min-h-screen bg-gray-50">
        <AdminSidebar />
        <div className="flex-1 flex flex-col">
          <AdminHeader />
          <main className="flex-1 flex items-center justify-center">
            <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Đang tải giao dịch...</p>
            </div>
          </main>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-gray-50">
      <AdminSidebar />

      <div className="flex-1 flex flex-col">
        <AdminHeader />

        <main className="p-8">
          <div className="bg-white rounded-2xl shadow-sm p-6">
            {/* Header */}
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-lg font-semibold text-gray-800">Quản lý giao dịch</h2>
                <p className="text-gray-500 text-sm">
                  Theo dõi và xử lý tất cả giao dịch trên nền tảng ({filteredTransactions.length} giao dịch)
                  {" · "}
                  <span className="text-blue-600 font-medium">Phí: {platformFeePercent}%</span>
                </p>
              </div>
              <button
                onClick={loadTransactions}
                className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition"
              >
                <RefreshCw className="w-4 h-4" />
                <span className="text-sm">Tải lại</span>
              </button>
            </div>

            {/* Error Message */}
            {error && (
              <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2">
                <AlertCircle className="text-red-600 w-5 h-5" />
                <span className="text-sm text-red-700">{error}</span>
                <button
                  onClick={loadTransactions}
                  className="ml-auto text-sm text-red-600 hover:text-red-700 underline"
                >
                  Thử lại
                </button>
              </div>
            )}

            {/* Search & Filter */}
            <div className="flex items-center gap-3 mb-6">
              <div className="flex items-center bg-gray-100 rounded-xl px-3 py-2 flex-1">
                <Search className="text-gray-400" size={18} />
                <input
                  type="text"
                  placeholder="Tìm kiếm giao dịch theo ID, người mua, người bán..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="bg-transparent flex-1 ml-2 text-sm outline-none text-gray-700"
                />
              </div>

              <select
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPage(0);
                }}
                className="border border-gray-200 rounded-lg px-3 py-2 text-sm text-gray-700 focus:ring-2 focus:ring-green-200"
              >
                <option value="ALL">Tất cả trạng thái</option>
                <option value="COMPLETED">Hoàn thành</option>
                <option value="PENDING">Đang xử lý</option>
                <option value="CANCELLED">Đã hủy</option>
              </select>
            </div>

            {/* Transaction list */}
            {loading ? (
              <div className="text-center py-12">
                <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600 mx-auto mb-3"></div>
                <p className="text-gray-500 text-sm">Đang tải...</p>
              </div>
            ) : filteredTransactions.length === 0 ? (
              <div className="text-center py-12">
                <FileText className="w-16 h-16 text-gray-400 mx-auto mb-3" />
                <p className="text-gray-500">Không tìm thấy giao dịch</p>
                <p className="text-gray-400 text-sm mt-1">Thử thay đổi bộ lọc của bạn</p>
              </div>
            ) : (
              <>
                <div className="space-y-4">
                  {filteredTransactions.map((tx) => {
                    const creditAmount = Number(tx.carbonCreditsAmount || tx.credit?.creditAmount || 0);
                    const totalPrice = Number(tx.totalPrice || tx.amount || 0);
                    const pricePerUnit = creditAmount > 0 ? totalPrice / creditAmount : 0;
                    const estimatedFee = totalPrice * (platformFeePercent / 100); // ✅ Use dynamic fee

                    return (
                      <div
                        key={tx.id}
                        className="flex items-center justify-between border border-gray-200 rounded-xl p-4 hover:shadow-sm transition bg-white"
                      >
                        {/* Left: Transaction Info */}
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-1">
                            <p className="font-semibold text-gray-800">
                              #{tx.id.toString().substring(0, 8)}...
                            </p>
                            <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${getStatusColor(tx.status)}`}>
                              {getStatusLabel(tx.status)}
                            </span>
                          </div>
                          <p className="text-gray-600 text-sm">
                            <span className="font-medium">Người mua:</span> {tx.buyer?.username || "N/A"}
                          </p>
                          <p className="text-gray-600 text-sm">
                            <span className="font-medium">Người bán:</span> {tx.seller?.username || "N/A"}
                          </p>
                          <p className="text-sm mt-1">
                            <span className="font-medium text-green-600">{creditAmount.toFixed(2)} tCO₂</span>
                            {" · "}
                            <span className="text-gray-600">{formatCurrency(pricePerUnit)}/tCO₂</span>
                          </p>
                        </div>

                        {/* Middle: Price Breakdown Preview */}
                        <div className="text-right mr-6 space-y-1">
                          <div className="flex flex-col items-end">
                            <p className="text-xs text-red-600 font-medium">
                              Người mua: {formatCurrency(totalPrice)}
                            </p>
                            <p className="text-xs text-green-600 font-medium">
                              Người bán: {formatCurrency(totalPrice - estimatedFee)}
                            </p>
                            <p className="text-xs text-blue-600 font-medium">
                              Phí: {formatCurrency(estimatedFee)}
                            </p>
                          </div>
                          <p className="text-xs text-gray-500 mt-1">
                            {formatDate(tx.createdAt)}
                          </p>
                        </div>

                        {/* Right: Actions */}
                        <div className="flex gap-2">
                          <button
                            onClick={() => loadTransactionDetail(tx.id)}
                            className="flex items-center gap-1 border border-gray-300 text-gray-700 px-3 py-1.5 rounded-lg text-sm hover:bg-gray-100 transition"
                          >
                            <Eye size={14} />
                            Chi tiết
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                  <div className="flex items-center justify-center gap-2 mt-6 pt-6 border-t border-gray-200">
                    <button
                      onClick={() => setPage(Math.max(0, page - 1))}
                      disabled={page === 0}
                      className="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition"
                    >
                      Trước
                    </button>
                    <span className="text-sm text-gray-600">
                      Trang {page + 1} / {totalPages}
                    </span>
                    <button
                      onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                      disabled={page >= totalPages - 1}
                      className="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition"
                    >
                      Sau
                    </button>
                  </div>
                )}
              </>
            )}
          </div>
        </main>
      </div>

      {/* Transaction Detail Modal */}
      {showDetailModal && selectedTransaction && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-3xl w-full max-h-[90vh] overflow-y-auto">
            <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
              <div>
                <h2 className="text-xl font-semibold text-gray-800">Chi tiết giao dịch</h2>
                <p className="text-sm text-gray-500">
                  ID: {selectedTransaction.id}
                </p>
              </div>
              <button
                onClick={() => setShowDetailModal(false)}
                className="text-gray-400 hover:text-gray-600 transition"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div className="p-6 space-y-6">
              {/* Status */}
              <div className="flex items-center justify-between bg-gray-50 rounded-lg p-4">
                <span className="text-sm text-gray-600">Trạng thái</span>
                <span className={`px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(selectedTransaction.status)}`}>
                  {getStatusLabel(selectedTransaction.status)}
                </span>
              </div>

              {/* Money Breakdown - NEW PROMINENT SECTION */}
              <div className="bg-gradient-to-r from-gray-50 to-gray-100 rounded-xl p-6 border border-gray-200">
                <h3 className="font-bold text-gray-800 mb-4 flex items-center gap-2">
                  <DollarSign className="w-5 h-5 text-green-600" />
                  Chi tiết thanh toán
                </h3>
                {/* ✅ Pass dynamic platformFeePercent to the component */}
                <TransactionMoneyBreakdown 
                  transaction={selectedTransaction} 
                  formatCurrency={formatCurrency}
                  platformFeePercent={platformFeePercent}
                />
              </div>

              {/* Participants */}
              <div className="grid grid-cols-2 gap-4">
                <div className="border border-gray-200 rounded-lg p-4">
                  <p className="text-xs text-gray-500 mb-1">Người mua</p>
                  <p className="font-medium text-gray-800">
                    {selectedTransaction.buyer?.username || "N/A"}
                  </p>
                  <p className="text-xs text-gray-500 mt-1">
                    {selectedTransaction.buyer?.email || ""}
                  </p>
                </div>
                <div className="border border-gray-200 rounded-lg p-4">
                  <p className="text-xs text-gray-500 mb-1">Người bán</p>
                  <p className="font-medium text-gray-800">
                    {selectedTransaction.seller?.username || "N/A"}
                  </p>
                  <p className="text-xs text-gray-500 mt-1">
                    {selectedTransaction.seller?.email || ""}
                  </p>
                </div>
              </div>

              {/* Transaction Details */}
              <div className="border border-gray-200 rounded-lg p-4 space-y-3">
                <h3 className="font-semibold text-gray-800 mb-3">Thông tin giao dịch</h3>
                
                <div className="flex justify-between">
                  <span className="text-sm text-gray-500">Số lượng tín chỉ</span>
                  <span className="text-sm font-medium text-gray-800">
                    {Number(selectedTransaction.carbonCreditsAmount || selectedTransaction.credit?.creditAmount || 0).toFixed(2)} tCO₂
                  </span>
                </div>

                <div className="flex justify-between">
                  <span className="text-sm text-gray-500">Đơn giá</span>
                  <span className="text-sm font-medium text-gray-800">
                    {formatCurrency(
                      Number(selectedTransaction.totalPrice || selectedTransaction.amount || 0) /
                      Number(selectedTransaction.carbonCreditsAmount || selectedTransaction.credit?.creditAmount || 1)
                    )}/tCO₂
                  </span>
                </div>

                <div className="flex justify-between pt-3 border-t border-gray-200">
                  <span className="text-sm font-medium text-gray-700">Tổng giá trị</span>
                  <span className="text-lg font-semibold text-gray-800">
                    {formatCurrency(Number(selectedTransaction.totalPrice || selectedTransaction.amount || 0))}
                  </span>
                </div>
              </div>

              {/* Timestamps */}
              <div className="border border-gray-200 rounded-lg p-4 space-y-2">
                <h3 className="font-semibold text-gray-800 mb-3">Thời gian</h3>
                
                <div className="flex justify-between">
                  <span className="text-sm text-gray-500">Tạo lúc</span>
                  <span className="text-sm text-gray-800">{formatDate(selectedTransaction.createdAt)}</span>
                </div>

                {selectedTransaction.completedAt && (
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-500">Hoàn tất lúc</span>
                    <span className="text-sm text-green-600">{formatDate(selectedTransaction.completedAt)}</span>
                  </div>
                )}
              </div>

              {/* IDs */}
              <div className="border border-gray-200 rounded-lg p-4 space-y-2">
                <h3 className="font-semibold text-gray-800 mb-3">Thông tin kỹ thuật</h3>
                
                <div className="flex justify-between">
                  <span className="text-xs text-gray-500">Transaction ID</span>
                  <span className="text-xs font-mono text-gray-600">{selectedTransaction.id}</span>
                </div>

                {selectedTransaction.credit?.creditId && (
                  <div className="flex justify-between">
                    <span className="text-xs text-gray-500">Credit ID</span>
                    <span className="text-xs font-mono text-gray-600">{selectedTransaction.credit.creditId}</span>
                  </div>
                )}

                {selectedTransaction.listing?.listingId && (
                  <div className="flex justify-between">
                    <span className="text-xs text-gray-500">Listing ID</span>
                    <span className="text-xs font-mono text-gray-600">{selectedTransaction.listing.listingId}</span>
                  </div>
                )}
              </div>
            </div>

            <div className="border-t border-gray-200 px-6 py-4 flex justify-end">
              <button
                onClick={() => setShowDetailModal(false)}
                className="px-4 py-2 bg-gray-900 text-white rounded-lg hover:bg-gray-800 transition"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}