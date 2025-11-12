import React, { useEffect, useState } from "react";
import SidebarBuyer from "../components/BuyerSidebar";
import Header from "../components/BuyerHeader";
import { 
  Search, 
  Calendar, 
  CheckCircle2, 
  RefreshCw, 
  Leaf, 
  AlertCircle,
  Wallet,
  QrCode,
  X,
  Copy,
  ExternalLink
} from "lucide-react";
import axiosInstance from "../api/axiosInstance";

export default function BuyerMarket() {
  const [credits, setCredits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [wallet, setWallet] = useState(null);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [purchasingId, setPurchasingId] = useState(null);

  // QR Code Modal State
  const [showQRModal, setShowQRModal] = useState(false);
  const [qrCodeData, setQrCodeData] = useState(null);
  const [generatingQR, setGeneratingQR] = useState(false);
  const [selectedListing, setSelectedListing] = useState(null);

  // Filter states
  const [searchTerm, setSearchTerm] = useState("");
  const [minPrice, setMinPrice] = useState("");
  const [maxPrice, setMaxPrice] = useState("");
  const [sortBy, setSortBy] = useState("newest");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Exchange rate (from backend config)
  const EXCHANGE_RATE = 26330; // 1 USD = 26,330 VND

  useEffect(() => {
    loadMarketData();
  }, [sortBy, page]);

  /**
   * ✅ Check if user returned from MoMo payment gateway
   */
  useEffect(() => {
    const checkPaymentReturn = async () => {
      const urlParams = new URLSearchParams(window.location.search);
      
      // MoMo return parameters
      const momoResultCode = urlParams.get("resultCode");
      const momoAmount = urlParams.get("amount");
      const momoOrderId = urlParams.get("orderId");
      const momoMessage = urlParams.get("message");

      if (momoResultCode === "0") {
        // ✅ SUCCESS - MoMo payment completed
        const amountVND = parseInt(momoAmount || "0");
        const amountUSD = (amountVND / EXCHANGE_RATE).toFixed(2);
        
        setSuccessMessage(
          `✅ Nạp tiền thành công qua MoMo! +$${amountUSD} USD (${formatVND(amountVND)}) đã được thêm vào ví của bạn.`
        );
        
        console.log("✅ MoMo Payment Success:");
        console.log("   • Order ID:", momoOrderId);
        console.log("   • Amount VND:", formatVND(amountVND));
        console.log("   • Amount USD:", `$${amountUSD}`);
        console.log("   • Message:", momoMessage);
        
        // Reload wallet data
        await loadMarketData();
        
        // Clean URL
        window.history.replaceState({}, document.title, window.location.pathname);
        
        setTimeout(() => setSuccessMessage(""), 8000);
        
      } else if (momoResultCode) {
        // ❌ FAILED - Payment declined/cancelled
        const errorMessages = {
          "1": "Giao dịch thất bại",
          "2": "Giao dịch bị từ chối",
          "9": "Giao dịch bị hủy",
          "10": "Giao dịch không tồn tại",
          "11": "Giao dịch đã hết hạn",
          "12": "Thẻ/Tài khoản bị khóa",
          "13": "OTP không chính xác",
          "1001": "Giao dịch thanh toán thất bại do tài khoản không đủ tiền",
          "1002": "Giao dịch bị từ chối do nhà phát hành thẻ từ chối giao dịch",
          "1003": "Giao dịch bị hủy",
          "1004": "Giao dịch bị đảo (reversal)",
          "1005": "Giao dịch bị từ chối do sai địa chỉ URL",
          "1006": "Giao dịch bị từ chối do sai tham số",
          "1007": "Giao dịch đang được xử lý"
        };
        
        const errorMsg = errorMessages[momoResultCode] || `Lỗi thanh toán (Mã: ${momoResultCode})`;
        setError(`❌ ${errorMsg}. Vui lòng thử lại hoặc liên hệ hỗ trợ.`);
        
        console.error("❌ MoMo Payment Failed:");
        console.error("   • Result Code:", momoResultCode);
        console.error("   • Order ID:", momoOrderId);
        console.error("   • Error:", errorMsg);
        
        window.history.replaceState({}, document.title, window.location.pathname);
        setTimeout(() => setError(""), 8000);
      }
    };

    checkPaymentReturn();
  }, []);

  const loadMarketData = async () => {
    setLoading(true);
    setError("");

    try {
      // Fetch wallet
      const walletRes = await axiosInstance.get("/wallets/my-wallet");
      if (walletRes.data?.data) {
        setWallet(walletRes.data.data);
        console.log("💰 Wallet loaded:", {
          balance: formatUSD(walletRes.data.data.cashBalance),
          balanceVND: formatVND(walletRes.data.data.cashBalance * EXCHANGE_RATE)
        });
      }

      // Fetch listings
      let listingsRes;
      if (minPrice && maxPrice) {
        listingsRes = await axiosInstance.get("/listings/search", {
          params: {
            minPrice: parseFloat(minPrice),
            maxPrice: parseFloat(maxPrice),
            page,
            size: 10
          }
        });
      } else {
        listingsRes = await axiosInstance.get("/listings", {
          params: { page, size: 10, sortBy }
        });
      }

      if (listingsRes.data?.data) {
        const pageData = listingsRes.data.data;
        setCredits(pageData.content || []);
        setTotalPages(pageData.totalPages || 0);
        console.log(`📋 Loaded ${pageData.content?.length || 0} listings (Page ${page + 1}/${pageData.totalPages})`);
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

  /**
   * ✅ Generate MoMo QR Code for Top-Up
   * Creates a MoMo payment link and displays QR code
   */
  const handleGenerateTopUpQR = async (listing) => {
    setGeneratingQR(true);
    setError("");
    setSelectedListing(listing);

    try {
      const currentBalanceUSD = Number(wallet?.cashBalance || 0);
      const totalCostUSD = Number(listing.creditAmount) * Number(listing.price);
      const shortfallUSD = totalCostUSD - currentBalanceUSD;

      // Add 10% buffer to ensure enough funds after conversion + fees
      const topUpAmountUSD = Math.ceil((shortfallUSD * 1.1) * 100) / 100;

      console.log("💰 Generating MoMo Top-Up QR Code:");
      console.log("   • Current Balance:", formatUSD(currentBalanceUSD));
      console.log("   • Required Amount:", formatUSD(totalCostUSD));
      console.log("   • Shortfall:", formatUSD(shortfallUSD));
      console.log("   • Top-Up Amount (USD):", formatUSD(topUpAmountUSD));

      // ✅ Call MoMo deposit endpoint
      const response = await axiosInstance.post("/wallets/deposit/momo", {
        amountUsd: topUpAmountUSD,
        orderInfo: `Nạp tiền mua credit #${listing.listingId.substring(0, 8)}`
      });

      console.log("📡 MoMo API Response:", response.data);

      if (response.data?.success && response.data?.paymentUrl) {
        const amountVND = response.data?.amountVnd || Math.ceil(topUpAmountUSD * EXCHANGE_RATE);
        
        setQrCodeData({
          paymentUrl: response.data.paymentUrl,
          orderId: response.data.orderId,
          paymentId: response.data.paymentId,
          amountUSD: topUpAmountUSD,
          amountVND: amountVND,
          shortfallUSD: shortfallUSD,
          listingId: listing.listingId,
          creditAmount: listing.creditAmount,
          pricePerUnit: listing.price,
          paymentMethod: "MoMo",
          exchangeRate: response.data?.exchangeRate || EXCHANGE_RATE
        });

        setShowQRModal(true);
        console.log("✅ MoMo QR Code generated successfully");
        console.log("   • Payment URL:", response.data.paymentUrl);
        console.log("   • Order ID:", response.data.orderId);
        console.log("   • Amount VND:", formatVND(amountVND));
      } else {
        throw new Error(response.data?.message || "Failed to generate MoMo payment URL");
      }
    } catch (e) {
      console.error("❌ Error generating MoMo QR code:", e.response?.data || e.message);
      const errorMsg = e.response?.data?.message || "Không thể tạo mã QR MoMo. Vui lòng thử lại.";
      setError(errorMsg);
      setTimeout(() => setError(""), 5000);
    } finally {
      setGeneratingQR(false);
    }
  };

  /**
   * Copy MoMo payment URL to clipboard
   */
  const handleCopyPaymentUrl = () => {
    if (qrCodeData?.paymentUrl) {
      navigator.clipboard.writeText(qrCodeData.paymentUrl);
      setSuccessMessage("✅ Đã sao chép link thanh toán MoMo!");
      setTimeout(() => setSuccessMessage(""), 3000);
    }
  };

  /**
   * Open MoMo payment URL in new tab
   */
  const handleOpenPaymentUrl = () => {
    if (qrCodeData?.paymentUrl) {
      window.open(qrCodeData.paymentUrl, "_blank");
    }
  };

  const handlePurchase = async (listingId, creditAmount, price) => {
    const totalCost = Number(creditAmount) * Number(price);
    if (!wallet || Number(wallet.cashBalance) < totalCost) {
      setError(
        `Số dư không đủ! Bạn cần ${formatUSD(totalCost)} ` +
        `(${formatVND(totalCost * EXCHANGE_RATE)}) ` +
        `nhưng chỉ có ${formatUSD(wallet?.cashBalance || 0)} ` +
        `(${formatVND((wallet?.cashBalance || 0) * EXCHANGE_RATE)})`
      );
      setTimeout(() => setError(""), 5000);
      return;
    }

    setPurchasingId(listingId);
    setError("");
    setSuccessMessage("");

    try {
      const response = await axiosInstance.post(`/transactions/purchase/${listingId}`);
      
      if (response.data?.success) {
        setSuccessMessage(
          `✅ Mua tín chỉ thành công! Đã chi: ${formatUSD(totalCost)} (${formatVND(totalCost * EXCHANGE_RATE)})`
        );
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

<<<<<<< Updated upstream
  const formatUSD = (usd) => {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(usd || 0);
  };

  const formatVND = (vnd) => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(vnd || 0);
  };
=======
  const formatCurrency = (usd) =>
  new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(usd || 0);
>>>>>>> Stashed changes

  const formatDate = (s) =>
    s ? new Date(s).toLocaleDateString("vi-VN", { year: "numeric", month: "short", day: "numeric" }) : "N/A";

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
            <div className="bg-white border border-gray-200 rounded-lg px-4 py-3 shadow-sm">
              <div className="flex items-center gap-2 mb-1">
                <Wallet className="w-4 h-4 text-green-600" />
                <p className="text-xs text-gray-500 font-medium">Số dư ví</p>
              </div>
              <p className="text-xl font-bold text-green-600">
                {wallet ? formatUSD(wallet.cashBalance) : "---"}
              </p>
              <p className="text-xs text-gray-400 mt-0.5">
                ≈ {wallet ? formatVND(wallet.cashBalance * EXCHANGE_RATE) : "---"}
              </p>
            </div>
          </div>

          {/* Messages */}
          {successMessage && (
            <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg flex items-center gap-2">
              <CheckCircle2 className="text-green-600 w-5 h-5 flex-shrink-0" />
              <span className="text-sm text-green-700">{successMessage}</span>
            </div>
          )}
          {error && (
            <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2">
              <AlertCircle className="text-red-600 w-5 h-5 flex-shrink-0" />
              <span className="text-sm text-red-700">{error}</span>
            </div>
          )}

          {/* Search & Filters */}
          <div className="bg-white border border-gray-200 rounded-xl p-4 mb-6 shadow-sm">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
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
              <div>
                <label className="text-xs text-gray-500 mb-1 block">Giá tối thiểu (USD)</label>
                <input
                  type="number"
                  placeholder="0.00"
                  value={minPrice}
                  onChange={(e) => setMinPrice(e.target.value)}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                />
              </div>
              <div>
                <label className="text-xs text-gray-500 mb-1 block">Giá tối đa (USD)</label>
                <input
                  type="number"
                  placeholder="10000.00"
                  value={maxPrice}
                  onChange={(e) => setMaxPrice(e.target.value)}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                />
              </div>
            </div>

            <div className="flex items-center gap-3 mt-3">
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

          {/* Listings */}
          {loading ? (
            <div className="text-center py-12">
              <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-green-600 mx-auto mb-3"></div>
              <p className="text-gray-500 text-sm">Đang tải...</p>
            </div>
          ) : filteredCredits.length === 0 ? (
            <div className="text-center py-12 bg-white border border-gray-200 rounded-xl">
              <Leaf className="w-16 h-16 text-gray-400 mx-auto mb-3" />
              <p className="text-gray-500">Không tìm thấy tín chỉ nào</p>
            </div>
          ) : (
            <>
              <div className="space-y-4">
                {filteredCredits.map((credit) => {
                  const priceUSD = Number(credit.price || 0);
                  const creditAmount = Number(credit.creditAmount || 0);
                  const totalCostUSD = creditAmount * priceUSD;
                  const totalCostVND = totalCostUSD * EXCHANGE_RATE;
                  
                  const isAuction = credit.type === "AUCTION";
                  const isPurchasing = purchasingId === credit.listingId;
                  const isGeneratingQR = generatingQR && selectedListing?.listingId === credit.listingId;
                  const canAfford = wallet && Number(wallet.cashBalance) >= totalCostUSD;
                  const shortfallUSD = totalCostUSD - Number(wallet?.cashBalance || 0);

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
                            {creditAmount.toFixed(2)} tCO₂
                            <CheckCircle2 className="text-green-600 w-4 h-4" />
                            <span
                              className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                                isAuction ? "bg-blue-100 text-blue-700" : "bg-gray-800 text-white"
                              }`}
                            >
                              {isAuction ? "Đấu giá" : "Giá cố định"}
                            </span>
                            <span
                              className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                                credit.status === "ACTIVE" ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-700"
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
                          </div>
                        </div>
                      </div>

                      <div className="text-right">
                        <p className="text-gray-800 font-bold text-xl">{formatUSD(priceUSD)}/tCO₂</p>
                        <p className="text-xs text-gray-500">≈ {formatVND(priceUSD * EXCHANGE_RATE)}/tCO₂</p>
                        
                        <div className="mt-2 p-2 bg-gray-50 rounded-lg border border-gray-200">
                          <p className="text-xs text-gray-500">Tổng cộng:</p>
                          <p className="text-lg font-bold text-gray-800">{formatUSD(totalCostUSD)}</p>
                          <p className="text-xs text-gray-500">≈ {formatVND(totalCostVND)}</p>
                        </div>
                        
                        {!canAfford && credit.status === "ACTIVE" && (
                          <div className="mt-2 p-2 bg-orange-50 border border-orange-200 rounded-lg">
                            <p className="text-xs text-orange-700 font-medium">⚠️ Số dư không đủ</p>
                            <p className="text-xs text-orange-600">Thiếu: {formatUSD(shortfallUSD)}</p>
                          </div>
                        )}
                        
                        <div className="flex gap-2 justify-end mt-3">
                          <button
                            onClick={() => window.location.href = `/buyer/listing/${credit.listingId}`}
                            className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm hover:bg-gray-100 transition"
                          >
                            Chi tiết
                          </button>
                          
                          {credit.status === "ACTIVE" && !isAuction && (
                            <>
                              {canAfford ? (
                                <button
                                  onClick={() => handlePurchase(credit.listingId, creditAmount, priceUSD)}
                                  disabled={isPurchasing}
                                  className={`px-3 py-1.5 rounded-lg text-sm text-white transition ${
                                    isPurchasing ? "bg-gray-400 cursor-not-allowed" : "bg-gray-900 hover:bg-gray-800"
                                  }`}
                                >
                                  {isPurchasing ? "Đang xử lý..." : "Mua ngay"}
                                </button>
                              ) : (
                                <button
                                  onClick={() => handleGenerateTopUpQR(credit)}
                                  disabled={isGeneratingQR}
                                  className={`px-3 py-1.5 rounded-lg text-sm text-white transition flex items-center gap-1 ${
                                    isGeneratingQR ? "bg-pink-400 cursor-not-allowed" : "bg-pink-600 hover:bg-pink-700"
                                  }`}
                                >
                                  {isGeneratingQR ? (
                                    <>
                                      <RefreshCw className="w-4 h-4 animate-spin" />
                                      Tạo QR...
                                    </>
                                  ) : (
                                    <>
                                      <QrCode className="w-4 h-4" />
                                      Nạp MoMo
                                    </>
                                  )}
                                </button>
                              )}
                            </>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>

              {totalPages > 1 && (
                <div className="flex items-center justify-center gap-2 mt-6">
                  <button
                    onClick={() => setPage(Math.max(0, page - 1))}
                    disabled={page === 0}
                    className="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition"
                  >
                    Trước
                  </button>
                  <span className="text-sm text-gray-600">Trang {page + 1} / {totalPages}</span>
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
        </main>
      </div>

      {/* ✅ MoMo QR Code Modal */}
      {showQRModal && qrCodeData && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full shadow-2xl">
            {/* Header */}
            <div className="flex items-center justify-between p-6 border-b border-gray-200 bg-gradient-to-r from-pink-50 to-purple-50">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 bg-pink-500 rounded-full flex items-center justify-center">
                  <QrCode className="text-white w-6 h-6" />
                </div>
                <div>
                  <h2 className="text-xl font-bold text-gray-800">Nạp tiền qua MoMo</h2>
                  <p className="text-sm text-gray-600">Quét mã QR để thanh toán nhanh</p>
                </div>
              </div>
              <button
                onClick={() => {
                  setShowQRModal(false);
                  setQrCodeData(null);
                  setSelectedListing(null);
                }}
                className="text-gray-400 hover:text-gray-600 transition"
              >
                <X className="w-6 h-6" />
              </button>
            </div>

            {/* Content */}
            <div className="p-6">
              {/* Transaction Summary */}
              <div className="bg-gradient-to-br from-pink-50 to-purple-50 rounded-xl p-4 mb-6 border border-pink-200">
                <h3 className="text-sm font-semibold text-pink-900 mb-3 flex items-center gap-2">
                  <Wallet className="w-4 h-4" />
                  Thông tin giao dịch
                </h3>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-pink-700">Số dư hiện tại:</span>
                    <span className="font-semibold text-pink-900">
                      {formatUSD(wallet?.cashBalance || 0)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-pink-700">Giá tín chỉ:</span>
                    <span className="font-semibold text-pink-900">
                      {formatUSD(qrCodeData.creditAmount * qrCodeData.pricePerUnit)}
                    </span>
                  </div>
                  <div className="flex justify-between pt-2 border-t border-pink-200">
                    <span className="text-pink-700 font-semibold">Thiếu:</span>
                    <span className="font-bold text-red-600">
                      {formatUSD(qrCodeData.shortfallUSD)}
                    </span>
                  </div>
                  <div className="flex justify-between pt-2 border-t border-pink-300">
                    <span className="text-pink-900 font-bold">Nạp (có dự phòng 10%):</span>
                    <span className="font-bold text-green-600 text-lg">
                      {formatUSD(qrCodeData.amountUSD)}
                    </span>
                  </div>
                  <div className="flex justify-between text-xs">
                    <span className="text-pink-600">Tương đương VND:</span>
                    <span className="font-semibold text-pink-800">
                      {formatVND(qrCodeData.amountVND)}
                    </span>
                  </div>
                  <div className="flex justify-between text-xs pt-1 border-t border-pink-100">
                    <span className="text-pink-600">Mã đơn hàng:</span>
                    <span className="font-mono text-xs bg-white px-2 py-0.5 rounded">
                      {qrCodeData.orderId}
                    </span>
                  </div>
                </div>
              </div>

              {/* QR Code Display */}
              <div className="bg-gradient-to-br from-white to-pink-50 border-2 border-dashed border-pink-300 rounded-xl p-6 mb-4">
                <div className="flex flex-col items-center">
                  <div className="bg-white p-4 rounded-xl shadow-lg mb-4 border-4 border-pink-200">
                    <img
                      src={`https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(qrCodeData.paymentUrl)}`}
                      alt="MoMo Payment QR Code"
                      className="w-56 h-56"
                    />
                  </div>
                  <div className="flex items-center gap-2 bg-pink-100 px-4 py-2 rounded-full">
                    <div className="w-8 h-8 bg-pink-500 rounded-full flex items-center justify-center">
                      <span className="text-white font-bold text-sm">M</span>
                    </div>
                    <p className="text-sm font-medium text-pink-900">
                      Quét mã bằng ứng dụng MoMo
                    </p>
                  </div>
                  <p className="text-xs text-gray-500 text-center mt-3 max-w-xs">
                    Mở app MoMo → Chọn <strong>"Quét mã QR"</strong> → Quét mã trên để thanh toán
                  </p>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="grid grid-cols-2 gap-3 mb-4">
                <button
                  onClick={handleCopyPaymentUrl}
                  className="flex items-center justify-center gap-2 px-4 py-3 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm font-medium text-gray-700 transition"
                >
                  <Copy className="w-4 h-4" />
                  Sao chép link
                </button>
                <button
                  onClick={handleOpenPaymentUrl}
                  className="flex items-center justify-center gap-2 px-4 py-3 bg-pink-600 hover:bg-pink-700 rounded-lg text-sm font-medium text-white transition"
                >
                  <ExternalLink className="w-4 h-4" />
                  Mở MoMo
                </button>
              </div>

              {/* Instructions */}
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                <h4 className="text-sm font-semibold text-yellow-900 mb-2 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4" />
                  Hướng dẫn thanh toán
                </h4>
                <ol className="text-xs text-yellow-800 space-y-1.5 ml-6 list-decimal">
                  <li>Mở ứng dụng <strong>MoMo</strong> trên điện thoại</li>
                  <li>Chọn <strong>"Quét mã QR"</strong> ở màn hình chính</li>
                  <li>Quét mã QR phía trên</li>
                  <li>Xác nhận số tiền: <strong>{formatVND(qrCodeData.amountVND)}</strong></li>
                  <li>Nhập mã PIN/xác thực vân tay để hoàn tất</li>
                  <li>Sau thanh toán thành công, số dư ví sẽ tự động cập nhật</li>
                  <li>Bạn có thể mua tín chỉ ngay sau khi nạp tiền</li>
                </ol>
              </div>

              {/* Footer Note */}
              <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
                <p className="text-xs text-blue-700 text-center">
                  💡 <strong>Lưu ý:</strong> Giao dịch sẽ được xử lý tự động sau khi thanh toán thành công. 
                  Vui lòng không đóng trang này cho đến khi hoàn tất.
                </p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}