import React, { useState, useEffect } from "react";
import Sidebar from "../components/Sidebar";
import Header from "../components/Header";
import { transactionService } from "../services/transactionService";
import { CheckCircle, Clock, XCircle, AlertCircle } from "lucide-react";

export default function Transactions() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    loadTransactions();
  }, []);

  const loadTransactions = async () => {
    setLoading(true);
    setError("");

    try {
      const response = await transactionService.getMyTransactions(0, 50);
      
      if (response.success) {
        const txData = response.data.content || response.data;
        setTransactions(Array.isArray(txData) ? txData : []);
      } else {
        setError(response.message || "Failed to load transactions");
      }
    } catch (err) {
      console.error("Error loading transactions:", err);
      setError("Failed to load transactions");
    } finally {
      setLoading(false);
    }
  };

  // Format date
  const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    const date = new Date(dateString);
    return date.toLocaleDateString("vi-VN");
  };

  // Get status display
  const getStatusDisplay = (status) => {
    const statusMap = {
      COMPLETED: {
        text: "Completed",
        icon: <CheckCircle size={16} className="text-green-600" />,
        className: "bg-green-100 text-green-700 border border-green-200"
      },
      PENDING: {
        text: "Processing",
        icon: <Clock size={16} className="text-yellow-600" />,
        className: "bg-yellow-100 text-yellow-700 border border-yellow-200"
      },
      CANCELLED: {
        text: "Cancelled",
        icon: <XCircle size={16} className="text-gray-600" />,
        className: "bg-gray-100 text-gray-700 border border-gray-200"
      },
      DISPUTED: {
        text: "Disputed",
        icon: <AlertCircle size={16} className="text-red-600" />,
        className: "bg-red-100 text-red-700 border border-red-200"
      }
    };

    return statusMap[status] || statusMap.PENDING;
  };

  // Determine transaction type and party
  const getTransactionInfo = (transaction) => {
    const user = JSON.parse(localStorage.getItem("user"));
    const currentUserId = user?.id;
    
    const isSale = transaction.seller?.id === currentUserId;
    const isPurchase = transaction.buyer?.id === currentUserId;

    if (isSale) {
      return {
        type: "Sale",
        party: transaction.buyer?.username || "Buyer",
        prefix: "Sold to"
      };
    } else if (isPurchase) {
      return {
        type: "Purchase",
        party: transaction.seller?.username || "Seller",
        prefix: "Bought from"
      };
    }

    return {
      type: "Unknown",
      party: "N/A",
      prefix: "Transaction with"
    };
  };

  if (loading) {
    return (
      <div className="flex min-h-screen w-screen bg-[#F9FAFB] overflow-hidden">
        <Sidebar />
        <div className="flex flex-col flex-1 min-h-screen w-full">
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
    <div className="flex min-h-screen w-screen bg-[#F9FAFB] overflow-hidden">
      <Sidebar />

      <div className="flex flex-col flex-1 min-h-screen w-full">
        <Header />

        <main className="flex-1 p-8 bg-[#F9FAFB] overflow-y-auto">
          <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6">
            <div className="flex justify-between items-center mb-6">
              <div>
                <h2 className="text-xl font-semibold text-gray-800 mb-2">
                  Lịch sử giao dịch
                </h2>
                <p className="text-gray-500 text-sm">
                  Theo dõi và quản lý các giao dịch tín chỉ carbon của bạn ({transactions.length} total)
                </p>
              </div>
              <button
                onClick={loadTransactions}
                className="px-4 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 transition"
              >
                Tải lại
              </button>
            </div>

            {/* Error Alert */}
            {error && (
              <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
                <p className="text-red-600 text-sm">{error}</p>
              </div>
            )}

            {/* Transaction List */}
            {transactions.length === 0 ? (
              <div className="text-center py-12 border border-dashed border-gray-300 rounded-lg">
                <CheckCircle className="w-12 h-12 text-gray-400 mx-auto mb-3" />
                <p className="text-gray-500 text-lg font-medium mb-1">Hiện chưa có giao dịch nào</p>
                <p className="text-gray-400 text-sm">
                  Lịch sử giao dịch của bạn sẽ hiển thị ở đây
                </p>
              </div>
            ) : (
              <div className="space-y-4">
                {transactions.map((t) => {
                  const txInfo = getTransactionInfo(t);
                  const statusDisplay = getStatusDisplay(t.status);

                  return (
                    <div
                      key={t.id}
                      className="flex items-center justify-between border border-gray-200 rounded-xl px-5 py-4 bg-white hover:bg-gray-50 transition"
                    >
                      {/* Left - Transaction Info */}
                      <div className="flex flex-col">
                        <span className="font-semibold text-gray-800">
                          {t.id?.toString().substring(0, 8).toUpperCase()}
                        </span>
                        <span className="text-sm text-gray-500">
                          {txInfo.prefix} {txInfo.party}
                        </span>
                      </div>

                      {/* Middle - Amount & Date */}
                      <div className="text-right">
                        <p className="text-gray-800 font-medium">
                          {(t.carbonCreditsAmount || 0).toFixed(2)} tCO₂
                        </p>
                        <p className="text-sm text-gray-400">
                          {formatDate(t.createdAt)}
                        </p>
                      </div>

                      {/* Price & Total */}
                      <div className="text-right">
                        <p className="text-sm text-gray-700">
                          {t.listing?.price 
                            ? `${t.listing.price.toLocaleString()} VND/tCO₂`
                            : "N/A"}
                        </p>
                        <p className="text-xs text-gray-400">
                          Total: {(t.totalPrice || 0).toLocaleString()} VND
                        </p>
                      </div>

                      {/* Status & Actions */}
                      <div className="flex items-center gap-3">
                        <span
                          className={`px-3 py-1 text-xs font-medium rounded-full flex items-center gap-1 ${statusDisplay.className}`}
                        >
                          {statusDisplay.icon}
                          {statusDisplay.text}
                        </span>
                        <button 
                          className="px-3 py-1 text-xs font-medium border border-gray-200 rounded-lg hover:bg-gray-100 transition"
                          onClick={() => {
                            // TODO: Implement transaction detail modal
                            console.log("View transaction:", t.id);
                          }}
                        >
                          Details
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}
