import React, { useEffect, useState } from "react";
import Sidebar from "../components/Sidebar";
import Header from "../components/Header";
import { Lightbulb } from "lucide-react";
import { carbonCreditService } from "../services/carbonCreditService";
import { creditListingService } from "../services/creditListingService";

export default function CarbonListing() {
  const [activeTab, setActiveTab] = useState("create");
  const [listingType, setListingType] = useState("fixed");
  const [creditOptions, setCreditOptions] = useState([]);
  const [listings, setListings] = useState([]);
  const [creditId, setCreditId] = useState("");
  const [price, setPrice] = useState("");
  const [auctionEnd, setAuctionEnd] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // ✅ Giá gợi ý cố định (có thể sau này thay bằng API)
  const suggestedPrice = 26.8;

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    setError("");
    setSuccess("");

    try {
      const creditsRes = await carbonCreditService.getMyCredits();
      const creditsArray = Array.isArray(creditsRes.data?.content)
        ? creditsRes.data.content
        : [];

      const user = JSON.parse(localStorage.getItem("user"));
      let listingsArray = [];

      if (user?.id) {
        const listingsRes = await creditListingService.getMyListings();
        listingsArray = Array.isArray(listingsRes.data?.content)
          ? listingsRes.data.content
          : Array.isArray(listingsRes.data)
          ? listingsRes.data
          : [];
        setListings(listingsArray);
      }

      const listedCreditIds = new Set(
        listingsArray
          .filter(
            (l) => l.status === "ACTIVE" || l.status === "PENDING_APPROVAL"
          )
          .map((l) => l.credit?.creditId || l.creditId)
      );

      setCreditOptions(
        creditsArray.filter(
          (c) =>
            c.status === "VERIFIED" &&
            !c.listedAt &&
            c.creditAmount > 0 &&
            !listedCreditIds.has(c.creditId)
        )
      );
    } catch (err) {
      console.error("❌ Error loading data:", err);
      setError("Failed to load data");
    }
  }

  async function handleCreateListing(e) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setSubmitting(true);

    if (!creditId || !price) {
      setError("Please fill all required fields!");
      setSubmitting(false);
      return;
    }

    try {
      const resp = await creditListingService.createListing(
        creditId,
        parseFloat(price)
      );

      if (resp.success) {
        setSuccess("Listing request submitted! Awaiting CVA approval.");
        setCreditId("");
        setPrice("");
        await loadData();
      } else {
        setError(resp.message || "Failed to create listing");
      }
    } catch (err) {
      console.error("❌ Error creating listing:", err);
      setError(err.response?.data?.message || "Error creating listing");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-screen w-screen bg-[#F9FAFB] overflow-hidden">
      <Sidebar />
      <div className="flex flex-col flex-1 min-h-screen w-full">
        <Header />
        <main className="flex-1 p-8 bg-[#F9FAFB] overflow-y-auto">
          <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6">
            <h2 className="text-xl font-semibold text-gray-800 mb-2">
              Carbon Credit Listing
            </h2>
            <p className="text-gray-500 text-sm mb-6">
              Đưa các tín chỉ carbon đã được xác minh của bạn ra bán với giá cố định hoặc đấu giá.
            </p>

            {/* Tabs */}
            <div className="flex mb-6 border-b border-gray-200">
              <button
                onClick={() => setActiveTab("create")}
                className={`flex-1 text-center py-2 rounded-t-lg font-medium transition ${
                  activeTab === "create"
                    ? "bg-gray-100 text-gray-800 border border-gray-200 border-b-transparent"
                    : "bg-white text-gray-500"
                }`}
              >
                Danh sách mới
              </button>
              <button
                onClick={() => setActiveTab("manage")}
                className={`flex-1 text-center py-2 rounded-t-lg font-medium transition ${
                  activeTab === "manage"
                    ? "bg-gray-100 text-gray-800 border border-gray-200 border-b-transparent"
                    : "bg-white text-gray-500"
                }`}
              >
                Quản lý danh sách
              </button>
            </div>

            {/* Tab Content */}
            {activeTab === "create" ? (
              <div className="grid grid-cols-2 gap-8">
                {/* Create Listing Form */}
                <form onSubmit={handleCreateListing}>
                  <div className="mb-4">
                    <label className="text-sm text-gray-700 font-medium">
                      Chọn tín chỉ đủ điều kiện
                    </label>
                    <select
                      value={creditId}
                      onChange={(e) => setCreditId(e.target.value)}
                      required
                      className="w-full mt-1 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 outline-none"
                    >
                      <option value="">--Chọn tín chỉ--</option>
                      {creditOptions.map((credit) => (
                        <option key={credit.creditId} value={credit.creditId}>
                          {credit.creditAmount} tCO₂ • Đã xác minh:{" "}
                          {credit.verifiedAt?.substring(0, 10)}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="mb-4">
                    <label className="text-sm text-gray-700 font-medium">
                      {listingType === "fixed"
                        ? "Price ($/tCO₂)"
                        : "Starting Price ($/tCO₂)"}
                    </label>
                    <input
                      type="number"
                      value={price}
                      min={1}
                      step={0.1}
                      onChange={(e) => setPrice(e.target.value)}
                      className="w-full mt-1 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 outline-none"
                      required
                    />
                  </div>

                  {listingType === "auction" && (
                    <div className="mb-4">
                      <label className="text-sm text-gray-700 font-medium">
                        Thời gian kết thúc đấu giá
                      </label>
                      <input
                        type="datetime-local"
                        value={auctionEnd}
                        onChange={(e) => setAuctionEnd(e.target.value)}
                        className="w-full mt-1 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 outline-none"
                        required
                      />
                    </div>
                  )}

                  <button
                    disabled={submitting}
                    className="w-full bg-black text-white py-2.5 rounded-lg mt-4 hover:bg-gray-800 transition disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {submitting ? "Submitting..." : "Request Listing"}
                  </button>

                  {error && (
                    <div className="text-red-500 text-sm mt-3 p-2 bg-red-50 border border-red-200 rounded">
                      {error}
                    </div>
                  )}
                  {success && (
                    <div className="text-green-600 text-sm mt-3 p-2 bg-green-50 border border-green-200 rounded">
                      {success}
                    </div>
                  )}
                </form>

                {/* AI Price Suggestion */}
                <div className="bg-gray-50 rounded-xl border border-gray-200 p-6">
                  <div className="flex items-center gap-2 mb-4">
                    <Lightbulb size={18} className="text-green-600" />
                    <h4 className="font-semibold text-gray-800">
                      Đề xuất giá AI
                    </h4>
                  </div>
                  <p className="text-green-600 text-3xl font-bold mb-1">
                    {suggestedPrice} $/tCO₂
                  </p>
                  <p className="text-sm text-gray-500 mb-3">Giá đề xuất</p>
                  <div className="flex items-center gap-2 mb-3">
                    <span className="text-sm text-gray-600">Độ tin cậy:</span>
                    <div className="flex-1 bg-gray-200 rounded-full h-2">
                      <div
                        className="bg-green-500 h-2 rounded-full"
                        style={{ width: "85%" }}
                      />
                    </div>
                    <span className="text-sm font-medium text-gray-700">
                      85%
                    </span>
                  </div>
                  <div className="bg-blue-50 text-blue-600 text-sm p-3 rounded-lg mb-4 border border-blue-100">
                    Giá thị trường hiện tại đã tăng 5% so với tuần trước. Nhu cầu tại khu vực của bạn đang cao.
                  </div>

                  {/* ✅ Nút Apply Suggestion có chức năng */}
                  <button
                    type="button"
                    onClick={() => setPrice(suggestedPrice.toString())}
                    className="w-full border border-gray-200 rounded-lg py-2 text-sm font-medium hover:bg-gray-100 transition"
                  >
                    Áp dụng gợi ý
                  </button>
                </div>
              </div>
            ) : (
              /* Manage Listings Tab */
              <div className="space-y-4">
                {listings.length === 0 ? (
                  <div className="text-center text-gray-500 py-12 border border-dashed border-gray-300 rounded-lg">
                    <p className="text-lg font-medium mb-1">Chưa có mục đăng bán nào</p>
                    <p className="text-sm">
                      Tạo mục đăng bán đầu tiên để bắt đầu
                    </p>
                  </div>
                ) : (
                  listings.map((listing) => (
                    <div
                      key={listing.listingId}
                      className="border border-gray-200 bg-white rounded-xl p-5 hover:shadow-md transition-all"
                    >
                      <div className="flex justify-between items-start">
                        {/* Left - Amount */}
                        <div className="flex-1">
                          <div className="flex items-baseline gap-2 mb-1">
                            <span className="text-2xl font-bold text-gray-900">
                              {listing.creditAmount || 0}
                            </span>
                            <span className="text-sm text-gray-500">tCO₂</span>
                          </div>
                          <p className="text-sm text-gray-500">
                            {listing.type === "FIXED"
                              ? "Fixed Price"
                              : "Auction"}{" "}
                            - {listing.createdAt?.substring(0, 10)}
                          </p>
                        </div>

                        {/* Middle - Price */}
                        <div className="flex-1 text-center">
                          <div className="flex items-baseline justify-center gap-1">
                            <span className="text-xl font-semibold text-gray-900">
                              {listing.price}
                            </span>
                            <span className="text-sm text-gray-500">
                              $/tCO₂
                            </span>
                          </div>
                          <p className="text-xs text-gray-500 mt-1">
                            Total:{" "}
                            {(
                              (listing.creditAmount || 0) *
                              (listing.price || 0)
                            ).toLocaleString()}{" "}
                            $
                          </p>
                        </div>

                        {/* Right - Status */}
                        <div className="flex flex-col items-end gap-2">
                          <span
                            className={`px-4 py-1.5 text-xs font-semibold rounded-full ${
                              listing.status === "ACTIVE"
                                ? "bg-black text-white"
                                : listing.status === "SOLD"
                                ? "bg-gray-200 text-gray-700"
                                : listing.status === "PENDING_APPROVAL"
                                ? "bg-yellow-100 text-yellow-700 border border-yellow-200"
                                : listing.status === "REJECTED"
                                ? "bg-red-100 text-red-700 border border-red-200"
                                : "bg-gray-100 text-gray-600"
                            }`}
                          >
                            {listing.status}
                          </span>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}
