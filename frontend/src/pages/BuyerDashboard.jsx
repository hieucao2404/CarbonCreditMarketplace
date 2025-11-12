  import React, { useEffect, useState } from "react";
  import SidebarBuyer from "../components/BuyerSidebar";
  import Header from "../components/BuyerHeader";
  import { Leaf, ShoppingCart, DollarSign, Award, RefreshCw } from "lucide-react";
  import axiosInstance from "../api/axiosInstance";

  export default function BuyerDashboard() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const [wallet, setWallet] = useState(null);
    const [recentPurchases, setRecentPurchases] = useState([]);
    const [availableCredits, setAvailableCredits] = useState([]);

    useEffect(() => {
      loadDashboard();
    }, []);

    const loadDashboard = async () => {
      setLoading(true);
      setError("");

      try {
        // Fetch wallet - Response structure: { success, message, data: WalletResponse }
        const walletRes = await axiosInstance.get("/wallets/my-wallet");
        console.log("💰 Wallet Response:", walletRes.data);
        
        if (walletRes.data?.data) {
          const walletData = walletRes.data.data;
          setWallet({
            id: walletData.walletId,
            userId: walletData.userId,
            username: walletData.username,
            creditBalance: walletData.creditBalance || 0,
            cashBalance: walletData.cashBalance || 0,
            lastUpdated: walletData.lastUpdated
          });
        }

        // Fetch transactions - Response structure: { success, message, data: Page<TransactionDTO> }
        const txRes = await axiosInstance.get("/transactions/my-history", { 
          params: { page: 0, size: 5 } 
        });
        console.log("📊 Transactions Response:", txRes.data);
        
        if (txRes.data?.data?.content) {
          setRecentPurchases(txRes.data.data.content);
        }

        // Fetch listings - Response structure: { success, message, data: Page<CreditListingDTO> }
        const listingsRes = await axiosInstance.get("/listings", { 
          params: { page: 0, size: 8, sortBy: "newest" } 
        });
        console.log("📋 Listings Response:", listingsRes.data);
        
        if (listingsRes.data?.data?.content) {
          setAvailableCredits(listingsRes.data.data.content);
        }
      } catch (e) {
        console.error("❌ Error loading dashboard:", e.response?.data || e.message);
        setError("Không thể tải dữ liệu. Vui lòng thử lại.");
      } finally {
        setLoading(false);
      }
    };

    const formatCurrency = (usd) =>
  new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(usd || 0);

    const formatDate = (s) =>
      s ? new Date(s).toLocaleDateString("vi-VN", { year: "numeric", month: "short", day: "numeric" }) : "N/A";

    const statusBadge = (status) => {
      const map = {
        COMPLETED: "bg-green-100 text-green-700",
        PENDING: "bg-yellow-100 text-yellow-700",
        FAILED: "bg-red-100 text-red-700",
        CANCELLED: "bg-gray-100 text-gray-700"
      };
      const cls = map[status] || "bg-gray-100 text-gray-700";
      const labels = {
        COMPLETED: "Hoàn thành",
        PENDING: "Đang xử lý",
        FAILED: "Thất bại",
        CANCELLED: "Đã hủy"
      };
      const label = labels[status] || status;
      return <span className={`text-xs px-2 py-0.5 rounded-full ${cls}`}>{label}</span>;
    };

    if (loading) {
      return (
        <div className="flex min-h-screen w-screen bg-[#F9FAFB]">
          <SidebarBuyer />
          <div className="flex flex-col flex-1">
            <Header />
            <main className="flex-1 flex items-center justify-center">
              <div className="text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto mb-4"></div>
                <p className="text-gray-600">Đang tải dữ liệu...</p>
              </div>
            </main>
          </div>
        </div>
      );
    }

    return (
      <div className="flex min-h-screen w-screen bg-[#F9FAFB] overflow-hidden">
        <SidebarBuyer />

        <div className="flex flex-col flex-1 min-h-screen w-full">
          <Header />

          <main className="flex-1 p-8 w-full bg-[#F9FAFB] overflow-y-auto">
            <div className="mb-8 flex items-center justify-between">
              <div>
                <h1 className="text-2xl font-semibold text-gray-800">Carbon Credit Exchange</h1>
                <button className="mt-2 bg-green-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-green-700">
                  Người mua tín chỉ
                </button>
              </div>
              <button
                onClick={loadDashboard}
                className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition"
              >
                <RefreshCw className="w-4 h-4" />
                <span className="text-sm">Tải lại</span>
              </button>
            </div>

            {error && (
              <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">{error}</div>
            )}

            {/* Stat cards */}
            <div className="grid grid-cols-3 gap-6 mb-8">
              <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
                <h2 className="text-gray-500 text-sm">Tín chỉ trong ví</h2>
                <p className="text-2xl font-semibold text-gray-800 mt-1">
                  {wallet ? Number(wallet.creditBalance || 0).toFixed(2) : "0.00"} tCO₂
                </p>
                <p className="text-sm text-green-600">Sẵn sàng sử dụng</p>
              </div>

              <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
                <h2 className="text-gray-500 text-sm">Số dư tiền</h2>
                <p className="text-2xl font-semibold text-gray-800 mt-1">
                  {wallet ? formatCurrency(Number(wallet.cashBalance || 0)) : formatCurrency(0)}
                </p>
                <p className="text-sm text-gray-500">Ví của {wallet?.username || "bạn"}</p>
              </div>

              <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
                <h2 className="text-gray-500 text-sm">Listings đang mở</h2>
                <p className="text-2xl font-semibold text-gray-800 mt-1">{availableCredits.length}</p>
                <p className="text-sm text-gray-500">Đang niêm yết trên marketplace</p>
              </div>
            </div>

            {/* 2 columns */}
            <div className="grid grid-cols-2 gap-6">
              {/* Recent purchases */}
              <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-2">Giao dịch gần đây</h3>
                <p className="text-sm text-gray-500 mb-4">Lịch sử giao dịch mới nhất</p>

                {recentPurchases.length === 0 ? (
                  <div className="text-center py-8">
                    <ShoppingCart className="w-12 h-12 text-gray-400 mx-auto mb-3" />
                    <p className="text-gray-500 text-sm">Chưa có giao dịch</p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {recentPurchases.map((tx) => (
                      <div key={tx.id} className="flex justify-between items-center border-b border-gray-200 pb-2">
                        <div className="flex items-center gap-2">
                          <ShoppingCart className="text-green-600 w-4 h-4" />
                          <p className="text-gray-700 text-sm">
                            {Number(tx.carbonCreditsAmount || tx.credit?.creditAmount || 0).toFixed(2)} tCO₂
                          </p>
                        </div>
                        <div className="text-right">
                          <p className="text-sm font-medium text-gray-700">
                            {formatCurrency(Number(tx.totalPrice || tx.amount || 0))}
                          </p>
                          {statusBadge(tx.status)}
                          <div className="text-[11px] text-gray-400 mt-1">
                            {formatDate(tx.createdAt)}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Available listings */}
              <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-6">
                <h3 className="text-lg font-semibold text-gray-800 mb-2">Tín chỉ carbon có sẵn</h3>
                <p className="text-sm text-gray-500 mb-4">Các tín chỉ mới nhất trên thị trường</p>

                {availableCredits.length === 0 ? (
                  <div className="text-center py-8">
                    <Leaf className="w-12 h-12 text-gray-400 mx-auto mb-3" />
                    <p className="text-gray-500 text-sm">Chưa có tín chỉ</p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {availableCredits.map((credit) => (
                      <div
                        key={credit.listingId}
                        className="flex justify-between items-center border-b border-gray-200 pb-2 hover:bg-gray-50 rounded-md px-2 cursor-pointer"
                        onClick={() => (window.location.href = `/buyer/marketplace`)}
                      >
                        <div className="flex items-center gap-2">
                          <Leaf className="text-green-600 w-4 h-4" />
                          <p className="text-gray-700 text-sm">
                            {Number(credit.creditAmount || 0).toFixed(2)} tCO₂
                          </p>
                        </div>
                        <div className="text-right">
                          <p className="text-sm font-medium text-gray-700">
                            {formatCurrency(Number(credit.price || 0))}/tCO₂
                          </p>
                          <span className="text-xs bg-gray-800 text-white px-2 py-0.5 rounded-full">
                            {credit.type === "AUCTION" ? "Đấu giá" : "Giá cố định"}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </main>
        </div>
      </div>
    );
  }