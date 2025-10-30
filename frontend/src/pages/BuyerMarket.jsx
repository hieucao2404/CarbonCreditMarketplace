import React, { useState, useEffect } from "react";
import axios from "axios";
import SidebarBuyer from "../components/BuyerSidebar";
import Header from "../components/BuyerHeader";
import { Search, MapPin, Calendar, CheckCircle2, Loader2 } from "lucide-react";

export default function BuyerMarket() {
  const [credits, setCredits] = useState([]);
  const [filteredCredits, setFilteredCredits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [buyingId, setBuyingId] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [priceFilter, setPriceFilter] = useState("all");
  const token = localStorage.getItem("token");

  // ✅ Lấy danh sách credit đang mở bán từ API
  useEffect(() => {
    async function fetchCredits() {
      try {
        const res = await axios.get("http://localhost:8080/api/credits/marketplace", {
          headers: { Authorization: `Bearer ${token}` },
        });
        const data = res.data.data || [];
        setCredits(data);
        setFilteredCredits(data);
      } catch (error) {
        console.error("Lỗi khi tải tín chỉ:", error);
        alert("Không thể tải danh sách tín chỉ carbon!");
      } finally {
        setLoading(false);
      }
    }
    fetchCredits();
  }, [token]);

  // ✅ Xử lý lọc và tìm kiếm frontend
  useEffect(() => {
    let result = [...credits];

    // Lọc theo từ khóa
    if (searchTerm.trim() !== "") {
      const lowerSearch = searchTerm.toLowerCase();
      result = result.filter(
        (c) =>
          c.sellerName?.toLowerCase().includes(lowerSearch) ||
          c.projectName?.toLowerCase().includes(lowerSearch) ||
          c.location?.toLowerCase().includes(lowerSearch)
      );
    }

    // Lọc theo giá
    result = result.filter((c) => {
      if (!c.price) return false;
      if (priceFilter === "below25") return c.price < 25000;
      if (priceFilter === "25to26") return c.price >= 25000 && c.price <= 26000;
      if (priceFilter === "above26") return c.price > 26000;
      return true; // all
    });

    setFilteredCredits(result);
  }, [credits, searchTerm, priceFilter]);

  // ✅ Gọi API mua ngay
  async function handlePurchase(listingId) {
    if (!window.confirm("Xác nhận mua tín chỉ này?")) return;
    setBuyingId(listingId);
    try {
      const res = await axios.post(
        `http://localhost:8080/api/transactions/purchase/${listingId}`,
        {},
        { headers: { Authorization: `Bearer ${token}` } }
      );
      alert("🎉 Mua tín chỉ thành công!");
      console.log(res.data);

      // Cập nhật lại danh sách sau khi mua
      const updated = await axios.get("http://localhost:8080/api/credits/marketplace", {
        headers: { Authorization: `Bearer ${token}` },
      });
      setCredits(updated.data.data || []);
    } catch (err) {
      console.error("Lỗi mua tín chỉ:", err);
      alert("❌ Mua tín chỉ thất bại: " + (err.response?.data?.message || err.message));
    } finally {
      setBuyingId(null);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen text-gray-700">
        <Loader2 className="animate-spin w-5 h-5 mr-2" /> Đang tải danh sách tín chỉ...
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-[#F9FAFB]">
      <SidebarBuyer />
      <div className="flex flex-col flex-1">
        <Header />

        <main className="p-8 w-full">
          <div className="mb-6">
            <h1 className="text-2xl font-semibold text-gray-800">Thị trường tín chỉ carbon</h1>
            <p className="text-gray-500 text-sm mt-1">
              Tìm kiếm và mua tín chỉ carbon từ chủ sở hữu xe điện
            </p>
          </div>

          {/* Search & Filters */}
          <div className="flex items-center gap-3 mb-6">
            {/* Ô tìm kiếm */}
            <div className="flex items-center bg-gray-100 rounded-lg px-3 py-2 w-full">
              <Search size={18} className="text-gray-400" />
              <input
                type="text"
                placeholder="Tìm theo người bán, dự án, khu vực..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="bg-transparent flex-1 px-2 text-sm outline-none"
              />
            </div>

            {/* Lọc theo giá */}
            <select
              value={priceFilter}
              onChange={(e) => setPriceFilter(e.target.value)}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm text-gray-700"
            >
              <option value="all">Tất cả mức giá</option>
              <option value="below25">Dưới 25,000 VNĐ</option>
              <option value="25to26">25,000–26,000 VNĐ</option>
              <option value="above26">Trên 26,000 VNĐ</option>
            </select>
          </div>

          {/* Danh sách tín chỉ */}
          <div className="space-y-4">
            {filteredCredits.length === 0 ? (
              <p className="text-gray-600">Không tìm thấy tín chỉ phù hợp.</p>
            ) : (
              filteredCredits.map((credit) => (
                <div
                  key={credit.id}
                  className="flex items-center justify-between bg-white border border-gray-200 rounded-xl p-4 hover:shadow-sm transition-all"
                >
                  <div className="flex items-center gap-4">
                    <img
                      src={credit.imageUrl || "/images/carbon-default.png"}
                      alt="carbon"
                      className="w-16 h-16 rounded-lg object-cover"
                    />
                    <div>
                      <h2 className="text-lg font-semibold text-gray-800 flex items-center gap-2">
                        {credit.quantity} tCO₂
                        <CheckCircle2 className="text-green-600 w-4 h-4" />
                        <span className="text-xs px-2 py-0.5 rounded-full font-medium bg-gray-800 text-white">
                          Giá cố định
                        </span>
                      </h2>
                      <p className="text-sm text-gray-600">
                        Người bán: {credit.sellerName || "Không rõ"}
                      </p>
                      <p className="text-sm text-gray-600">
                        Nguồn: {credit.projectName || "Không có"}
                      </p>
                      <div className="flex items-center text-sm text-gray-500 gap-4 mt-1">
                        <span className="flex items-center gap-1">
                          <MapPin size={14} /> {credit.location || "—"}
                        </span>
                        <span className="flex items-center gap-1">
                          <Calendar size={14} /> {credit.listedDate?.slice(0, 10) || "—"}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="text-right">
                    <p className="text-gray-800 font-semibold text-sm">
                      {credit.price ? `${credit.price} VNĐ/tCO₂` : "—"}
                    </p>
                    <p className="text-xs text-gray-500 mb-3">
                      Tổng: {credit.totalPrice ? `${credit.totalPrice} VNĐ` : "—"}
                    </p>
                    <div className="flex gap-2 justify-end">
                      <button className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm hover:bg-gray-100">
                        Chi tiết
                      </button>
                      <button
                        onClick={() => handlePurchase(credit.id)}
                        disabled={buyingId === credit.id}
                        className={`px-3 py-1.5 rounded-lg text-sm text-white flex items-center justify-center gap-1 ${
                          buyingId === credit.id
                            ? "bg-gray-400 cursor-not-allowed"
                            : "bg-gray-900 hover:bg-gray-800"
                        }`}
                      >
                        {buyingId === credit.id ? (
                          <>
                            <Loader2 className="w-4 h-4 animate-spin" /> Đang mua...
                          </>
                        ) : (
                          "Mua ngay"
                        )}
                      </button>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </main>
      </div>
    </div>
  );
}
