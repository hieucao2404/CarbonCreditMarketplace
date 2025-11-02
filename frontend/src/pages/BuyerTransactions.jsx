import React, { useState, useEffect } from "react";
import SidebarBuyer from "../components/BuyerSidebar";
import Header from "../components/BuyerHeader";
import { Calendar, Download, CheckCircle2, Clock, AlertCircle, RefreshCw, Eye } from "lucide-react";
import axiosInstance from "../api/axiosInstance";

export default function BuyerTransactions() {
  const [purchases, setPurchases] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selectedTransaction, setSelectedTransaction] = useState(null);
  const [showDetailModal, setShowDetailModal] = useState(false);

  useEffect(() => {
    loadTransactions();
  }, [page]);

  const loadTransactions = async () => {
    setLoading(true);
    setError("");

    try {
      const response = await axiosInstance.get("/transactions/my-history", {
        params: { page, size: 10 }
      });

      console.log("📊 Transactions Response:", response.data);

      if (response.data?.data) {
        const pageData = response.data.data;
        setPurchases(pageData.content || []);
        setTotalPages(pageData.totalPages || 0);
      }
    } catch (e) {
      console.error("❌ Error loading transactions:", e.response?.data || e.message);
      setError(e.response?.data?.message || "Không thể tải lịch sử giao dịch.");
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

  const formatCurrency = (vnd) =>
    new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(vnd || 0);

  const formatDate = (s) =>
    s ? new Date(s).toLocaleDateString("vi-VN", { 
      year: "numeric", 
      month: "short", 
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit"
    }) : "N/A";

  const getStatusConfig = (status) => {
    const statusMap = {
      COMPLETED: { 
        label: "Hoàn thành", 
        class: "bg-green-600 text-white",
        icon: <CheckCircle2 size={14} />
      },
      PENDING: { 
        label: "Đang xử lý", 
        class: "bg-yellow-500 text-white",
        icon: <Clock size={14} />
      },
      FAILED: { 
        label: "Thất bại", 
        class: "bg-red-600 text-white",
        icon: <AlertCircle size={14} />
      },
      CANCELLED: { 
        label: "Đã hủy", 
        class: "bg-gray-500 text-white",
        icon: <AlertCircle size={14} />
      }
    };
    return statusMap[status] || { 
      label: status, 
      class: "bg-gray-200 text-gray-700",
      icon: null
    };
  };

  const handleDownloadCertificate = (transaction) => {
    // Placeholder for certificate download
    const creditAmount = Number(transaction.carbonCreditsAmount || transaction.credit?.creditAmount || 0);
    alert(`Tải xuống chứng nhận\n\nGiao dịch: ${transaction.id}\nSố lượng: ${creditAmount.toFixed(2)} tCO₂\n\nTính năng này sẽ được phát triển.`);
  };

  if (loading && purchases.length === 0) {
    return (
      <div className="flex min-h-screen bg-[#F9FAFB]">
        <SidebarBuyer />
        <div className="flex flex-col flex-1">
          <Header />
          <main className="flex-1 flex items-center justify-center">
            <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Đang tải giao dịch...</p>
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
          {/* Title & Actions */}
          <div className="mb-6 flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-semibold text-gray-800">
                Lịch sử mua tín chỉ carbon
              </h1>
              <p className="text-gray-500 text-sm mt-1">
                Quản lý và theo dõi các giao dịch mua của bạn
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
            </div>
          )}

          {/* List of purchases */}
          <div className="bg-white rounded-xl border border-gray-200 p-5">
            {loading ? (
              <div className="text-center py-12">
                <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-green-600 mx-auto mb-3"></div>
                <p className="text-gray-500 text-sm">Đang tải...</p>
              </div>
            ) : purchases.length === 0 ? (
              <div className="text-center py-12">
                <Calendar className="w-16 h-16 text-gray-400 mx-auto mb-3" />
                <p className="text-gray-500">Chưa có giao dịch nào</p>
                <p className="text-gray-400 text-sm mt-1">Bắt đầu mua tín chỉ carbon ngay!</p>
              </div>
            ) : (
              <>
                <div className="space-y-4">
                  {purchases.map((item) => {
                    const creditAmount = Number(item.carbonCreditsAmount || item.credit?.creditAmount || 0);
                    const totalPrice = Number(item.totalPrice || item.amount || 0);
                    const pricePerUnit = creditAmount > 0 ? totalPrice / creditAmount : 0;
                    const sellerName = item.seller?.username || "N/A";
                    const statusConfig = getStatusConfig(item.status);
                    const isCompleted = item.status === "COMPLETED";

                    return (
                      <div
                        key={item.id}
                        className="flex items-center justify-between bg-gray-50 border border-gray-200 rounded-lg p-4 hover:bg-gray-100 transition"
                      >
                        {/* Left Section */}
                        <div className="flex-1">
                          <div className="flex items-center gap-2">
                            <h2 className="text-base font-semibold text-gray-800">
                              #{item.id.toString().substring(0, 8)}
                            </h2>
                            <span className={`flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${statusConfig.class}`}>
                              {statusConfig.icon}
                              {statusConfig.label}
                            </span>
                          </div>
                          <p className="text-sm text-gray-600 mt-1">
                            {creditAmount.toFixed(2)} tCO₂ × {formatCurrency(pricePerUnit)}
                          </p>
                          <p className="text-sm text-gray-500">
                            Người bán: <span className="font-medium">{sellerName}</span>
                          </p>
                          <div className="flex items-center gap-4 mt-2">
                            <div className="flex items-center gap-1 text-xs text-gray-400">
                              <Calendar size={14} /> 
                              Tạo: {formatDate(item.createdAt)}
                            </div>
                            {item.completedAt && (
                              <div className="flex items-center gap-1 text-xs text-green-600">
                                <CheckCircle2 size={14} /> 
                                Hoàn tất: {formatDate(item.completedAt)}
                              </div>
                            )}
                          </div>
                          {item.listing && (
                            <p className="text-xs text-gray-400 mt-1">
                              Listing: {item.listing.listingId?.toString().substring(0, 8)}...
                            </p>
                          )}
                        </div>

                        {/* Right Section */}
                        <div className="text-right ml-4">
                          <p className="text-lg font-semibold text-gray-800">
                            {formatCurrency(totalPrice)}
                          </p>
                          <p className="text-xs text-gray-500 mb-3">
                            Tổng thanh toán
                          </p>
                          
                          <div className="flex items-center justify-end gap-2">
                            <button 
                              onClick={() => loadTransactionDetail(item.id)}
                              className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm hover:bg-white flex items-center gap-1 transition"
                            >
                              <Eye size={14} />
                              Chi tiết
                            </button>

                            {isCompleted && (
                              <button 
                                onClick={() => handleDownloadCertificate(item)}
                                className="border border-green-600 bg-green-50 text-green-700 rounded-lg px-3 py-1.5 text-sm hover:bg-green-100 flex items-center gap-1 transition"
                              >
                                <Download size={14} />
                                Chứng nhận
                              </button>
                            )}
                          </div>
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
          <div className="bg-white rounded-xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
              <h2 className="text-xl font-semibold text-gray-800">Chi tiết giao dịch</h2>
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
              {/* Transaction ID & Status */}
              <div className="bg-gray-50 rounded-lg p-4">
                <div className="flex items-center justify-between mb-3">
                  <span className="text-sm text-gray-500">Mã giao dịch</span>
                  <span className="font-mono text-sm font-medium text-gray-800">
                    {selectedTransaction.id}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-500">Trạng thái</span>
                  <span className={`flex items-center gap-1 px-3 py-1 rounded-full text-sm font-medium ${getStatusConfig(selectedTransaction.status).class}`}>
                    {getStatusConfig(selectedTransaction.status).icon}
                    {getStatusConfig(selectedTransaction.status).label}
                  </span>
                </div>
              </div>

              {/* Transaction Info */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-xs text-gray-500 mb-1">Người mua</p>
                  <p className="text-sm font-medium text-gray-800">
                    {selectedTransaction.buyer?.username || "N/A"}
                  </p>
                </div>
                <div>
                  <p className="text-xs text-gray-500 mb-1">Người bán</p>
                  <p className="text-sm font-medium text-gray-800">
                    {selectedTransaction.seller?.username || "N/A"}
                  </p>
                </div>
              </div>

              {/* Credit Info */}
              <div className="border-t border-gray-200 pt-4">
                <h3 className="text-sm font-semibold text-gray-700 mb-3">Thông tin tín chỉ</h3>
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-500">Số lượng tín chỉ</span>
                    <span className="text-sm font-medium text-gray-800">
                      {Number(selectedTransaction.carbonCreditsAmount || selectedTransaction.credit?.creditAmount || 0).toFixed(2)} tCO₂
                    </span>
                  </div>
                  {selectedTransaction.credit?.creditId && (
                    <div className="flex justify-between">
                      <span className="text-sm text-gray-500">Credit ID</span>
                      <span className="text-xs font-mono text-gray-600">
                        {selectedTransaction.credit.creditId.toString().substring(0, 16)}...
                      </span>
                    </div>
                  )}
                  {selectedTransaction.listing?.listingId && (
                    <div className="flex justify-between">
                      <span className="text-sm text-gray-500">Listing ID</span>
                      <span className="text-xs font-mono text-gray-600">
                        {selectedTransaction.listing.listingId.toString().substring(0, 16)}...
                      </span>
                    </div>
                  )}
                </div>
              </div>

              {/* Payment Info */}
              <div className="border-t border-gray-200 pt-4">
                <h3 className="text-sm font-semibold text-gray-700 mb-3">Thông tin thanh toán</h3>
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-500">Tổng tiền</span>
                    <span className="text-lg font-semibold text-gray-800">
                      {formatCurrency(Number(selectedTransaction.totalPrice || selectedTransaction.amount || 0))}
                    </span>
                  </div>
                </div>
              </div>

              {/* Timestamps */}
              <div className="border-t border-gray-200 pt-4">
                <h3 className="text-sm font-semibold text-gray-700 mb-3">Thời gian</h3>
                <div className="space-y-2">
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
              </div>
            </div>

            <div className="border-t border-gray-200 px-6 py-4 flex justify-end gap-2">
              <button
                onClick={() => setShowDetailModal(false)}
                className="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:bg-gray-50 transition"
              >
                Đóng
              </button>
              {selectedTransaction.status === "COMPLETED" && (
                <button
                  onClick={() => {
                    handleDownloadCertificate(selectedTransaction);
                    setShowDetailModal(false);
                  }}
                  className="px-4 py-2 bg-green-600 text-white rounded-lg text-sm hover:bg-green-700 transition flex items-center gap-2"
                >
                  <Download size={16} />
                  Tải chứng nhận
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}