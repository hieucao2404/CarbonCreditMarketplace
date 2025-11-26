// src/pages/VerifierApproved.jsx
import { useState, useEffect } from "react";
import VerifierSidebar from "../components/VerifierSidebar";
import VerifierHeader from "../components/VerifierHeader";
import {
  CheckCircle2,
  XCircle,
  Eye,
  RefreshCw,
  ShoppingCart,
  Calendar,
  Award,
  TrendingUp,
  Leaf
} from "lucide-react";
import { cvaService } from "../services/cvaService";

export default function VerifierApproved() {
  const [credits, setCredits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState("ALL");
  const [sortBy, setSortBy] = useState("date");

  // Popup State
  const [showPopup, setShowPopup] = useState(false);
  const [selectedCredit, setSelectedCredit] = useState(null);

  useEffect(() => {
    loadApprovedCredits();
  }, []);

  const loadApprovedCredits = async () => {
    setLoading(true);
    setError("");

    try {
      const response = await cvaService.getApprovedCredits();

      if (response.success) {
        const creditsData = response.data || [];
        setCredits(Array.isArray(creditsData) ? creditsData : []);
      } else {
        setError(response.message || "Không thể tải danh sách tín chỉ.");
      }
    } catch (err) {
      console.error("Error loading approved credits:", err);
      setError("Không thể tải danh sách tín chỉ.");
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleDateString("vi-VN", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const getStatusConfig = (credit) => {
    const status = credit.verificationStatus || credit.status;

    const configs = {
      VERIFIED: {
        badge: "✓ Đã xác minh",
        bgColor: "bg-green-600",
        icon: <CheckCircle2 className="w-5 h-5 text-green-500" />,
      },
      LISTED: {
        badge: "📋 Đã niêm yết",
        bgColor: "bg-blue-600",
        icon: <ShoppingCart className="w-5 h-5 text-blue-500" />,
      },
      SOLD: {
        badge: "💰 Đã bán",
        bgColor: "bg-purple-600",
        icon: <TrendingUp className="w-5 h-5 text-purple-500" />,
      },
      REJECTED: {
        badge: "✗ Bị từ chối",
        bgColor: "bg-red-600",
        icon: <XCircle className="w-5 h-5 text-red-500" />,
      },
    };

    return configs[status] || {
      badge: status || "Không xác định",
      bgColor: "bg-gray-600",
      icon: <CheckCircle2 className="w-5 h-5 text-gray-500" />,
    };
  };

  const filteredAndSortedCredits = credits
    .filter((credit) => {
      const status = credit.verificationStatus || credit.status;
      if (filter === "ALL") return true;
      if (filter === "VERIFIED") return ["VERIFIED", "LISTED", "SOLD"].includes(status);
      if (filter === "REJECTED") return status === "REJECTED";
      return true;
    })
    .sort((a, b) => {
      if (sortBy === "date") {
        return new Date(b.createdAt) - new Date(a.createdAt);
      } else if (sortBy === "amount") {
        return (b.creditAmount || 0) - (a.creditAmount || 0);
      }
      return 0;
    });

  const stats = {
    total: credits.length,
    verified: credits.filter((c) =>
      ["VERIFIED", "LISTED", "SOLD"].includes(c.verificationStatus || c.status)
    ).length,
    rejected: credits.filter((c) => (c.verificationStatus || c.status) === "REJECTED").length,
    totalAmount: credits.reduce((sum, c) => sum + (c.creditAmount || 0), 0),
  };

  if (loading) {
    return (
      <div className="flex h-screen bg-gray-50">
        <VerifierSidebar />
        <div className="flex-1 flex flex-col">
          <VerifierHeader />
          <main className="flex-1 flex items-center justify-center">
            <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Đang tải danh sách...</p>
            </div>
          </main>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-screen bg-gray-50">
      <VerifierSidebar />
      <div className="flex-1 flex flex-col">
        <VerifierHeader />

        <main className="flex-1 overflow-y-auto p-8">
          {/* Header */}
          <div className="flex justify-between items-center mb-6">
            <div>
              <h1 className="text-2xl font-bold text-gray-800">
                Tín chỉ Carbon Đã Xác Minh
              </h1>
              <p className="text-gray-500 mt-1">
                Lịch sử đầy đủ các tín chỉ carbon đã xử lý
              </p>
            </div>
            <button
              onClick={loadApprovedCredits}
              className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition shadow-sm"
            >
              <RefreshCw className="w-4 h-4" />
              <span className="text-sm font-medium">Tải lại</span>
            </button>
          </div>

          {/* Error Alert */}
          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-3">
              <XCircle className="w-5 h-5 text-red-600" />
              <p className="text-red-600 text-sm">{error}</p>
            </div>
          )}

          {/* Stats */}
          <div className="grid grid-cols-4 gap-4 mb-6">
            <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2 bg-gray-100 rounded-lg">
                  <Award className="w-5 h-5 text-gray-600" />
                </div>
                <p className="text-sm font-medium text-gray-600">Tổng đã xử lý</p>
              </div>
              <p className="text-3xl font-bold text-gray-800">{stats.total}</p>
              <p className="text-xs text-gray-500 mt-1">
                {stats.totalAmount.toFixed(2)} tCO₂ tổng
              </p>
            </div>

            <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2 bg-green-100 rounded-lg">
                  <CheckCircle2 className="w-5 h-5 text-green-600" />
                </div>
                <p className="text-sm font-medium text-gray-600">Đã xác minh</p>
              </div>
              <p className="text-3xl font-bold text-green-600">{stats.verified}</p>
            </div>

            <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2 bg-red-100 rounded-lg">
                  <XCircle className="w-5 h-5 text-red-600" />
                </div>
                <p className="text-sm font-medium text-gray-600">Bị từ chối</p>
              </div>
              <p className="text-3xl font-bold text-red-600">{stats.rejected}</p>
            </div>

            <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2 bg-blue-100 rounded-lg">
                  <ShoppingCart className="w-5 h-5 text-blue-600" />
                </div>
                <p className="text-sm font-medium text-gray-600">Đã niêm yết / Đã bán</p>
              </div>
              <p className="text-3xl font-bold text-blue-600">
                {credits.filter((c) => ["LISTED", "SOLD"].includes(c.status)).length}
              </p>
            </div>
          </div>

          {/* Filters and Sort */}
          <div className="flex justify-between items-center mb-6">
            <div className="flex gap-2">
              <button
                onClick={() => setFilter("ALL")}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition ${
                  filter === "ALL"
                    ? "bg-green-600 text-white shadow-md"
                    : "bg-white text-gray-600 border border-gray-300 hover:bg-gray-50"
                }`}
              >
                Tất cả ({stats.total})
              </button>
              <button
                onClick={() => setFilter("VERIFIED")}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition ${
                  filter === "VERIFIED"
                    ? "bg-green-600 text-white shadow-md"
                    : "bg-white text-gray-600 border border-gray-300 hover:bg-gray-50"
                }`}
              >
                Đã xác minh ({stats.verified})
              </button>
              <button
                onClick={() => setFilter("REJECTED")}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition ${
                  filter === "REJECTED"
                    ? "bg-red-600 text-white shadow-md"
                    : "bg-white text-gray-600 border border-gray-300 hover:bg-gray-50"
                }`}
              >
                Bị từ chối ({stats.rejected})
              </button>
            </div>

            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              className="px-4 py-2 bg-white border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 transition"
            >
              <option value="date">Sắp xếp theo ngày</option>
              <option value="amount">Sắp xếp theo số lượng tín chỉ</option>
            </select>
          </div>

          {/* Credits List */}
          <div className="space-y-4">
            {filteredAndSortedCredits.length === 0 ? (
              <div className="bg-white rounded-xl border border-gray-200 p-12 text-center">
                <CheckCircle2 className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                <p className="text-gray-500 text-lg font-medium mb-2">
                  Không tìm thấy tín chỉ phù hợp
                </p>
              </div>
            ) : (
              filteredAndSortedCredits.map((credit, index) => {
                const statusConfig = getStatusConfig(credit);

                return (
                  <div
                    key={credit.creditId || index}
                    className="bg-white rounded-xl border border-gray-200 shadow-sm hover:shadow-md transition-all p-6"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-4 flex-1">
                        <div className="p-3 bg-gray-50 rounded-lg">
                          {statusConfig.icon}
                        </div>

                        <div className="flex-1">
                          <div className="flex items-center gap-3 mb-2">
                            <h3 className="text-lg font-semibold text-gray-800">
                              {(credit.creditAmount || 0).toFixed(4)} tCO₂
                            </h3>
                            <span
                              className={`${statusConfig.bgColor} text-white text-xs font-medium px-3 py-1 rounded-full`}
                            >
                              {statusConfig.badge}
                            </span>
                          </div>

                          <div className="grid grid-cols-2 gap-6 text-sm">
                            <div className="flex items-center gap-2 text-gray-600">
                              <Leaf className="w-4 h-4 text-green-500" />
                              <span>
                                Giảm thải:
                                <strong className="text-gray-800">
                                  {(credit.co2ReducedKg || 0).toFixed(2)} kg CO₂
                                </strong>
                              </span>
                            </div>

                            <div className="flex items-center gap-2 text-gray-600">
                              <Calendar className="w-4 h-4 text-gray-400" />
                              <span>{formatDate(credit.verifiedAt || credit.createdAt)}</span>
                            </div>
                          </div>

                          <p className="text-xs text-gray-400 mt-2">
                            Mã tín chỉ: #{credit.creditId?.substring(0, 8) || "N/A"}
                          </p>
                        </div>
                      </div>

                      {/* 🔥 BUTTON GỌI POPUP */}
                      <button
                        onClick={() => {
                          setSelectedCredit(credit);
                          setShowPopup(true);
                        }}
                        className="ml-6 px-5 py-2.5 border border-gray-300 rounded-lg text-sm font-medium hover:bg-gray-50 flex items-center gap-2 transition"
                      >
                        <Eye className="w-4 h-4" />
                        Xem chi tiết
                      </button>
                    </div>
                  </div>
                );
              })
            )}
          </div>

          {filteredAndSortedCredits.length > 0 && (
            <div className="mt-6 text-center">
              <p className="text-sm text-gray-500">
                Hiển thị{" "}
                <span className="font-semibold">{filteredAndSortedCredits.length}</span>{" "}
                trên tổng số{" "}
                <span className="font-semibold">{stats.total}</span>{" "}
                tín chỉ đã xử lý
              </p>
            </div>
          )}
        </main>
      </div>

      {/* =============================== */}
      {/* 📌 POPUP CHI TIẾT TÍN CHỈ */}
      {/* =============================== */}
      {showPopup && selectedCredit && (
        <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50 animate-fadeIn">
          <div className="bg-white w-[650px] rounded-xl shadow-xl p-6">
            
            {/* Header */}
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-bold text-gray-800">Chi tiết Tín chỉ Carbon</h2>
              <button
                onClick={() => setShowPopup(false)}
                className="text-gray-500 hover:text-black text-xl"
              >
                ✕
              </button>
            </div>

            {/* Nội dung Popup */}
            <div className="space-y-4 text-gray-700">

              <div className="flex justify-between">
                <span className="text-gray-600">Số lượng tín chỉ:</span>
                <strong>{selectedCredit.creditAmount?.toFixed(4)} tCO₂</strong>
              </div>

              <div className="flex justify-between">
                <span className="text-gray-600">Giảm thải CO₂:</span>
                <strong>{selectedCredit.co2ReducedKg?.toFixed(2)} kg</strong>
              </div>

              <div className="flex justify-between">
                <span className="text-gray-600">Trạng thái:</span>
                <span className="px-3 py-1 text-white rounded-full text-xs bg-green-600">
                  {selectedCredit.status || selectedCredit.verificationStatus}
                </span>
              </div>

              <div className="flex justify-between">
                <span className="text-gray-600">Ngày tạo:</span>
                <strong>{formatDate(selectedCredit.createdAt)}</strong>
              </div>

              <div className="flex justify-between">
                <span className="text-gray-600">Ngày xác minh:</span>
                <strong>{formatDate(selectedCredit.verifiedAt)}</strong>
              </div>

              <div>
                <p className="text-gray-600 mb-1">Mã tín chỉ:</p>
                <p className="font-mono bg-gray-100 p-2 rounded text-sm break-all">
                  {selectedCredit.creditId}
                </p>
              </div>

              {/* Nếu có description */}
              {selectedCredit.description && (
                <div>
                  <p className="text-gray-600 mb-1">Mô tả:</p>
                  <p className="bg-gray-100 p-3 rounded text-sm">{selectedCredit.description}</p>
                </div>
              )}

              {/* Nếu có tài liệu chứng từ */}
              {selectedCredit.documentUrl && (
                <div className="mt-3">
                  <p className="text-gray-600 mb-1">Tài liệu chứng từ:</p>
                  <a
                    href={selectedCredit.documentUrl}
                    className="text-blue-600 underline text-sm"
                    target="_blank"
                  >
                    Nhấn để xem tài liệu
                  </a>
                </div>
              )}

              {/* Buyer Info nếu LISTED hoặc SOLD */}
              {["LISTED", "SOLD"].includes(selectedCredit.status) && (
                <div className="mt-4 bg-gray-50 p-3 rounded-lg border">
                  <p className="font-medium text-gray-700 mb-2">Thông tin giao dịch</p>

                  <div className="flex justify-between text-sm">
                    <span>Giá bán:</span>
                    <strong>{selectedCredit.price || "N/A"} USD</strong>
                  </div>

                  <div className="flex justify-between text-sm mt-1">
                    <span>Người mua:</span>
                    <strong>{selectedCredit.buyerName || "Chưa có"}</strong>
                  </div>
                </div>
              )}
            </div>

            {/* Footer */}
            <div className="text-right mt-6">
              <button
                onClick={() => setShowPopup(false)}
                className="px-4 py-2 bg-gray-200 hover:bg-gray-300 rounded-lg text-sm font-medium"
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
