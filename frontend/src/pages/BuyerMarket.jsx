import React, { useEffect, useState } from "react";
import SidebarBuyer from "../components/BuyerSidebar";
import Header from "../components/BuyerHeader";
import { Search, MapPin, Calendar, CheckCircle2, Clock, RefreshCw, Leaf, AlertCircle } from "lucide-react";
import axiosInstance from "../api/axiosInstance";

export default function BuyerMarket() {
  const [credits, setCredits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [wallet, setWallet] = useState(null);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [purchasingId, setPurchasingId] = useState(null);

  // Filter states
  const [searchTerm, setSearchTerm] = useState("");
  const [minPrice, setMinPrice] = useState("");
  const [maxPrice, setMaxPrice] = useState("");
  const [sortBy, setSortBy] = useState("newest");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    loadMarketData();
  }, [sortBy, page]);

  const loadMarketData = async () => {
    setLoading(true);
    setError("");

    try {
      // Fetch wallet
      const walletRes = await axiosInstance.get("/wallets/my-wallet");
      if (walletRes.data?.data) {
        setWallet(walletRes.data.data);
      }

      // Fetch listings
      let listingsRes;
      if (minPrice && maxPrice) {
        // Search by price range
        listingsRes = await axiosInstance.get("/listings/search", {
          params: {
            minPrice: parseFloat(minPrice),
            maxPrice: parseFloat(maxPrice),
            page,
            size: 10
          }
        });
      } else {
        // Get all active listings
        listingsRes = await axiosInstance.get("/listings", {
          params: { page, size: 10, sortBy }
        });
      }

      if (listingsRes.data?.data) {
        const pageData = listingsRes.data.data;
        setCredits(pageData.content || []);
        setTotalPages(pageData.totalPages || 0);
      }
    } catch (e) {
      console.error("❌ Error loading market data:", e.response?.data || e.message);
      setError(e.response?.data?.message || "Không thể tải dữ liệu thị trường.");
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    setPage(0);
    loadMarketData();
  };

  const handlePurchase = async (listingId, creditAmount, price) => {
    // Check if user has enough balance
    const totalCost = Number(creditAmount) * Number(price);
    if (!wallet || Number(wallet.cashBalance) < totalCost) {
      setError(`Số dư không đủ! Bạn cần ${formatCurrency(totalCost)} nhưng chỉ có ${formatCurrency(wallet?.cashBalance || 0)}`);
      setTimeout(() => setError(""), 5000);
      return;
    }

    setPurchasingId(listingId);
    setError("");
    setSuccessMessage("");

    try {
      const response = await axiosInstance.post(`/transactions/purchase/${listingId}`);
      
      if (response.data?.success) {
        setSuccessMessage("Mua tín chỉ thành công! 🎉");
        // Reload data to reflect changes
        await loadMarketData();
        setTimeout(() => setSuccessMessage(""), 5000);
      }
    } catch (e) {
      console.error("❌ Purchase error:", e.response?.data || e.message);
      const errorMsg = e.response?.data?.message || "Không thể hoàn tất giao dịch.";
      setError(errorMsg);
      setTimeout(() => setError(""), 5000);
    } finally {
      setPurchasingId(null);
    }
  };

  const formatCurrency = (vnd) =>
    new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(vnd || 0);

  const formatDate = (s) =>
    s ? new Date(s).toLocaleDateString("vi-VN", { year: "numeric", month: "short", day: "numeric" }) : "N/A";

  // Filter credits by search term (client-side filtering)
  const filteredCredits = credits.filter((credit) => {
    if (!searchTerm) return true;
    const searchLower = searchTerm.toLowerCase();
    return (
      credit.sellerUsername?.toLowerCase().includes(searchLower) ||
      credit.creditId?.toLowerCase().includes(searchLower)
    );
  });

  if (loading && credits.length === 0) {
    return (
      <div className="flex min-h-screen bg-[#F9FAFB]">
        <SidebarBuyer />
        <div className="flex flex-col flex-1">
          <Header />
          <main className="flex-1 flex items-center justify-center">
            <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Đang tải dữ liệu thị trường...</p>
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
          {/* Title & Wallet Info */}
          <div className="mb-6 flex items-start justify-between">
            <div>
              <h1 className="text-2xl font-semibold text-gray-800">
                Thị trường tín chỉ carbon
              </h1>
              <p className="text-gray-500 text-sm mt-1">
                Tìm kiếm và mua tín chỉ carbon từ chủ sở hữu xe điện
              </p>
            </div>
            <div className="bg-white border border-gray-200 rounded-lg px-4 py-2 shadow-sm">
              <p className="text-xs text-gray-500">Số dư ví</p>
              <p className="text-lg font-semibold text-green-600">
                {wallet ? formatCurrency(wallet.cashBalance) : "---"}
              </p>
            </div>
          </div>

          {/* Success/Error Messages */}
          {successMessage && (
            <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg flex items-center gap-2">
              <CheckCircle2 className="text-green-600 w-5 h-5" />
              <span className="text-sm text-green-700">{successMessage}</span>
            </div>
          )}
          {error && (
            <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2">
              <AlertCircle className="text-red-600 w-5 h-5" />
              <span className="text-sm text-red-700">{error}</span>
            </div>
          )}

          {/* Search & Filters */}
          <div className="bg-white border border-gray-200 rounded-xl p-4 mb-6 shadow-sm">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
              {/* Search */}
              <div className="col-span-2">
                <label className="text-xs text-gray-500 mb-1 block">Tìm kiếm</label>
                <div className="flex items-center bg-gray-100 rounded-lg px-3 py-2">
                  <Search size={18} className="text-gray-400" />
                  <input
                    type="text"
                    placeholder="Tìm theo người bán..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="bg-transparent flex-1 px-2 text-sm outline-none"
                  />
                </div>
              </div>

              {/* Min Price */}
              <div>
                <label className="text-xs text-gray-500 mb-1 block">Giá tối thiểu (VNĐ)</label>
                <input
                  type="number"
                  placeholder="0"
                  value={minPrice}
                  onChange={(e) => setMinPrice(e.target.value)}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                />
              </div>

              {/* Max Price */}
              <div>
                <label className="text-xs text-gray-500 mb-1 block">Giá tối đa (VNĐ)</label>
                <input
                  type="number"
                  placeholder="999999999"
                  value={maxPrice}
                  onChange={(e) => setMaxPrice(e.target.value)}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                />
              </div>
            </div>

            <div className="flex items-center gap-3 mt-3">
              {/* Sort By */}
              <div className="flex-1">
                <label className="text-xs text-gray-500 mb-1 block">Sắp xếp</label>
                <select
                  value={sortBy}
                  onChange={(e) => {
                    setSortBy(e.target.value);
                    setPage(0);
                  }}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm text-gray-700"
                >
                  <option value="newest">Mới nhất</option>
                  <option value="oldest">Cũ nhất</option>
                  <option value="price_asc">Giá thấp đến cao</option>
                  <option value="price_desc">Giá cao đến thấp</option>
                </select>
              </div>

              {/* Buttons */}
              <div className="flex gap-2 mt-5">
                <button
                  onClick={handleSearch}
                  className="px-4 py-2 bg-green-600 text-white rounded-lg text-sm hover:bg-green-700 transition"
                >
                  Tìm kiếm
                </button>
                <button
                  onClick={() => {
                    setSearchTerm("");
                    setMinPrice("");
                    setMaxPrice("");
                    setSortBy("newest");
                    setPage(0);
                    loadMarketData();
                  }}
                  className="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:bg-gray-50 transition flex items-center gap-2"
                >
                  <RefreshCw className="w-4 h-4" />
                  Đặt lại
                </button>
              </div>
            </div>
          </div>

          {/* List */}
          {loading ? (
            <div className="text-center py-12">
              <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-green-600 mx-auto mb-3"></div>
              <p className="text-gray-500 text-sm">Đang tải...</p>
            </div>
          ) : filteredCredits.length === 0 ? (
            <div className="text-center py-12 bg-white border border-gray-200 rounded-xl">
              <Leaf className="w-16 h-16 text-gray-400 mx-auto mb-3" />
              <p className="text-gray-500">Không tìm thấy tín chỉ nào</p>
              <p className="text-gray-400 text-sm mt-1">Thử thay đổi bộ lọc của bạn</p>
            </div>
          ) : (
            <>
              <div className="space-y-4">
                {filteredCredits.map((credit) => {
                  const totalCost = Number(credit.creditAmount || 0) * Number(credit.price || 0);
                  const isAuction = credit.type === "AUCTION";
                  const isPurchasing = purchasingId === credit.listingId;
                  const canAfford = wallet && Number(wallet.cashBalance) >= totalCost;

                  return (
                    <div
                      key={credit.listingId}
                      className="flex items-center justify-between bg-white border border-gray-200 rounded-xl p-4 hover:shadow-md transition-all"
                    >
                      <div className="flex items-center gap-4">
                        <div className="w-16 h-16 rounded-lg bg-gradient-to-br from-green-400 to-green-600 flex items-center justify-center">
                          <Leaf className="text-white w-8 h-8" />
                        </div>
                        <div>
                          <h2 className="text-lg font-semibold text-gray-800 flex items-center gap-2">
                            {Number(credit.creditAmount || 0).toFixed(2)} tCO₂
                            <CheckCircle2 className="text-green-600 w-4 h-4" />
                            <span
                              className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                                isAuction
                                  ? "bg-blue-100 text-blue-700"
                                  : "bg-gray-800 text-white"
                              }`}
                            >
                              {isAuction ? "Đấu giá" : "Giá cố định"}
                            </span>
                            <span
                              className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                                credit.status === "ACTIVE"
                                  ? "bg-green-100 text-green-700"
                                  : "bg-gray-100 text-gray-700"
                              }`}
                            >
                              {credit.status}
                            </span>
                          </h2>
                          <p className="text-sm text-gray-600">
                            Người bán: <span className="font-medium">{credit.sellerUsername || "N/A"}</span>
                          </p>
                          <p className="text-sm text-gray-600">
                            ID: <span className="font-mono text-xs">{credit.creditId?.substring(0, 8)}...</span>
                          </p>
                          <div className="flex items-center text-sm text-gray-500 gap-4 mt-1">
                            <span className="flex items-center gap-1">
                              <Calendar size={14} /> {formatDate(credit.createdAt)}
                            </span>
                            {credit.auctionEndTime && (
                              <span className="flex items-center gap-1 text-red-500">
                                <Clock size={14} /> Kết thúc: {formatDate(credit.auctionEndTime)}
                              </span>
                            )}
                          </div>
                        </div>
                      </div>

                      {/* Right Side */}
                      <div className="text-right">
                        <p className="text-gray-800 font-semibold text-lg">
                          {formatCurrency(credit.price)}/tCO₂
                        </p>
                        <p className="text-xs text-gray-500 mb-3">
                          Tổng: <span className="font-semibold">{formatCurrency(totalCost)}</span>
                        </p>
                        {!canAfford && credit.status === "ACTIVE" && (
                          <p className="text-xs text-red-500 mb-2">⚠️ Số dư không đủ</p>
                        )}
                        <div className="flex gap-2 justify-end">
                          <button
                            onClick={() => window.location.href = `/buyer/listing/${credit.listingId}`}
                            className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm hover:bg-gray-100"
                          >
                            Chi tiết
                          </button>
                          {credit.status === "ACTIVE" && !isAuction && (
                            <button
                              onClick={() => handlePurchase(credit.listingId, credit.creditAmount, credit.price)}
                              disabled={isPurchasing || !canAfford}
                              className={`px-3 py-1.5 rounded-lg text-sm text-white ${
                                isPurchasing || !canAfford
                                  ? "bg-gray-400 cursor-not-allowed"
                                  : "bg-gray-900 hover:bg-gray-800"
                              }`}
                            >
                              {isPurchasing ? "Đang xử lý..." : "Mua ngay"}
                            </button>
                          )}
                          {credit.status === "ACTIVE" && isAuction && (
                            <button
                              className="px-3 py-1.5 rounded-lg text-sm text-white bg-blue-600 hover:bg-blue-700"
                            >
                              Đấu giá
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
        </main>
      </div>
    </div>
  );
}