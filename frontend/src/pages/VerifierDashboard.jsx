import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import VerifierSidebar from "../components/VerifierSidebar";
import VerifierHeader from "../components/VerifierHeader";
import { AlertCircle, CheckCircle, XCircle, RefreshCw, FileText, Map } from "lucide-react";

// Create CVA service
const cvaService = {
  getPendingJourneys: async () => {
    const response = await fetch("http://localhost:8080/api/cva/pending-journeys", {
      headers: {
        "Authorization": `Bearer ${localStorage.getItem("token")}`,
        "Content-Type": "application/json"
      }
    });
    return response.json();
  },

  getPendingListings: async () => {
    const response = await fetch("http://localhost:8080/api/cva/pending-listings", {
      headers: {
        "Authorization": `Bearer ${localStorage.getItem("token")}`,
        "Content-Type": "application/json"
      }
    });
    return response.json();
  },

  getStatistics: async () => {
    const response = await fetch("http://localhost:8080/api/cva/statistics", {
      headers: {
        "Authorization": `Bearer ${localStorage.getItem("token")}`,
        "Content-Type": "application/json"
      }
    });
    return response.json();
  },

  getMyVerifications: async () => {
    const response = await fetch("http://localhost:8080/api/cva/my-verifications", {
      headers: {
        "Authorization": `Bearer ${localStorage.getItem("token")}`,
        "Content-Type": "application/json"
      }
    });
    return response.json();
  }
};

export default function VerifierDashboard() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [stats, setStats] = useState({
    pendingJourneysCount: 0,
    pendingListingsCount: 0,
    approvedThisMonth: 0,
    rejectedThisMonth: 0,
    totalVerified: 0,
    approvalRate: 0
  });
  const [pendingJourneys, setPendingJourneys] = useState([]);
  const [pendingListings, setPendingListings] = useState([]);
  const [recentVerifications, setRecentVerifications] = useState([]);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    setLoading(true);
    setError("");

    try {
      const [journeysRes, listingsRes, statsRes, verificationsRes] = await Promise.allSettled([
        cvaService.getPendingJourneys(),
        cvaService.getPendingListings(),
        cvaService.getStatistics(),
        cvaService.getMyVerifications()
      ]);

      // ✅ Declare variables FIRST
      let journeys = [];
      let listings = [];
      let verifications = [];

      // Pending journeys
      if (journeysRes.status === "fulfilled" && journeysRes.value?.success) {
        journeys = journeysRes.value.data || [];
        setPendingJourneys(Array.isArray(journeys) ? journeys : []);
      }

      // Pending listings
      if (listingsRes.status === "fulfilled" && listingsRes.value?.success) {
        listings = listingsRes.value.data || [];
        setPendingListings(Array.isArray(listings) ? listings : []);
      }

      // Recent verifications
      if (verificationsRes.status === "fulfilled" && verificationsRes.value?.success) {
        verifications = verificationsRes.value.data || [];
        setRecentVerifications(verifications.slice(0, 5));
      }

      // ✅ NOW you can use listings variable
      // Statistics - Match backend field names
      if (statsRes.status === "fulfilled" && statsRes.value?.success) {
        const statsData = statsRes.value.data || {};

        console.log("Backend statistics:", statsData);

        setStats({
          // Backend returns "pendingReview" for all pending journeys
          pendingJourneysCount: statsData.pendingReview || 0,

          // Calculate listings count from array
          pendingListingsCount: listings.length || 0,

          // ✅ Backend returns totalVerified and totalRejected (not split by month)
          approvedThisMonth: statsData.totalVerified || 0,
          rejectedThisMonth: statsData.totalRejected || 0,

          // ✅ Total processed (verified + rejected)
          totalVerified: statsData.totalProcessed || 0,

          // ✅ Backend returns approval rate as number
          approvalRate: statsData.approvalRate || 0
        });
      } else {
        // ✅ Set default values if API fails
        setStats({
          pendingJourneysCount: 0,
          pendingListingsCount: 0,
          approvedThisMonth: 0,
          rejectedThisMonth: 0,
          totalVerified: 0,
          approvalRate: 0
        });
      }

    } catch (err) {
      console.error("Error loading dashboard data:", err);
      setError("Tải dữ liệu bảng điều khiển thất bại");
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    const date = new Date(dateString);
    return date.toLocaleDateString("vi-VN");
  };

  // Combined high priority items (journeys + listings)
  const getHighPriorityItems = () => {
    const now = new Date();

    // ✅ Ensure pendingJourneys is always an array
    const journeysArray = Array.isArray(pendingJourneys) ? pendingJourneys : [];

    // High priority journeys (>20 tCO₂ or >48 hours old)
    const priorityJourneys = journeysArray
      .filter(j => {
        const creditAmount = j.creditsEarned || j.co2ReducedKg || 0;
        const submittedDate = new Date(j.createdAt);
        const hoursSinceSubmission = (now - submittedDate) / (1000 * 60 * 60);
        return creditAmount > 20 || hoursSinceSubmission > 48;
      })
      .map(j => ({ ...j, type: 'journey' }));

    // ✅ Ensure pendingListings is always an array
    const listingsArray = Array.isArray(pendingListings) ? pendingListings : [];

    // High priority listings (>50 tCO₂ or >72 hours old)
    const priorityListings = listingsArray
      .filter(l => {
        const creditAmount = l.creditAmount || 0;
        const submittedDate = new Date(l.createdAt);
        const hoursSinceSubmission = (now - submittedDate) / (1000 * 60 * 60);
        return creditAmount > 50 || hoursSinceSubmission > 72;
      })
      .map(l => ({ ...l, type: 'listing' }));

    // Combine and sort by creation date (oldest first)
    return [...priorityJourneys, ...priorityListings]
      .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
      .slice(0, 4); // Show top 4
  };

  const handleViewItem = (item) => {
    if (item.type === 'journey') {
      navigate(`/verifier/journey/${item.journeyId}`);
    } else if (item.type === 'listing') {
      navigate(`/verifier/listing/${item.id || item.listingId}`);
    }
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
              <p className="text-gray-600">Đang tải bảng điều khiển...</p>
            </div>
          </main>
        </div>
      </div>
    );
  }

  const highPriorityItems = getHighPriorityItems();
  const totalPending = stats.pendingJourneysCount + stats.pendingListingsCount;

  return (
    <div className="flex h-screen bg-gray-50">
      <VerifierSidebar />

      <div className="flex-1 flex flex-col">
        <VerifierHeader />

        <main className="flex-1 overflow-y-auto p-8">
          {/* Title */}
          <div className="mb-6 flex justify-between items-center">
            <div>
              <h1 className="text-2xl font-semibold text-gray-800">
                Tổng quan Xác thực
              </h1>
              <p className="text-gray-500 text-sm mt-1">
                Theo dõi và quản lý các yêu cầu xác thực hành trình & tin rao
              </p>
            </div>
            <button
              onClick={loadDashboardData}
              className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition"
            >
              <RefreshCw className="w-4 h-4" />
              <span className="text-sm">Làm mới</span>
            </button>
          </div>

          {/* Error Alert */}
          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-red-600 text-sm">{error}</p>
            </div>
          )}

          {/* Summary cards */}
          <div className="grid grid-cols-4 gap-6 mb-8">
            <div className="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm">
              <p className="text-gray-500 text-sm">Chờ xác thực</p>
              <h2 className="text-3xl font-bold text-gray-800 mt-2">
                {totalPending}
              </h2>
              <p className="text-xs text-gray-400">
                {stats.pendingJourneysCount} hành trình, {stats.pendingListingsCount} tin rao
              </p>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm">
              <p className="text-gray-500 text-sm">Đã phê duyệt trong tháng</p>
              <h2 className="text-3xl font-bold text-gray-800 mt-2">
                {stats.approvedThisMonth}
              </h2>
              <p className="text-xs text-gray-400">
                +{stats.rejectedThisMonth} yêu cầu bị từ chối
              </p>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm">
              <p className="text-gray-500 text-sm">Tổng đã xác thực</p>
              <h2 className="text-3xl font-bold text-gray-800 mt-2">
                {stats.totalVerified.toLocaleString()}
              </h2>
              <p className="text-xs text-gray-400">Tổng tín chỉ carbon</p>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm">
              <p className="text-gray-500 text-sm">Tỷ lệ phê duyệt</p>
              <h2 className="text-3xl font-bold text-gray-800 mt-2">
                {stats.approvalRate.toFixed(0)}%
              </h2>
              <p className="text-xs text-gray-400">Trong tháng</p>
            </div>
          </div>

          {/* Lower grid */}
          <div className="grid grid-cols-2 gap-6">
            {/* High Priority Requests */}
            <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm">
              <h3 className="text-lg font-semibold text-gray-800 mb-2">
                Yêu cầu ưu tiên cao
              </h3>
              <p className="text-sm text-gray-500 mb-4">
                Hành trình & tin rao khẩn cần xử lý
              </p>

              {highPriorityItems.length === 0 ? (
                <div className="text-center py-8">
                  <CheckCircle className="w-12 h-12 text-green-500 mx-auto mb-3" />
                  <p className="text-gray-500 text-sm">Không có yêu cầu ưu tiên cao</p>
                  <p className="text-gray-400 text-xs mt-1">
                    Tất cả yêu cầu đang trong thời gian xử lý bình thường
                  </p>
                </div>
              ) : (
                <div className="space-y-3">
                  {highPriorityItems.map((item, index) => {
                    const isJourney = item.type === 'journey';
                    const creditAmount = isJourney
                      ? (item.creditsEarned || item.co2ReducedKg || 0)
                      : (item.creditAmount || 0);
                    const userName = isJourney
                      ? (item.user?.username || item.user?.fullName || "Người dùng không xác định")
                      : (item.seller?.username || item.seller?.fullName || "Người bán không xác định");

                    return (
                      <div
                        key={`${item.type}-${item.journeyId || item.id || item.listingId}-${index}`}
                        className="flex items-center justify-between bg-red-50 border border-red-200 p-3 rounded-xl"
                      >
                        <div className="flex items-center gap-3">
                          {isJourney ? (
                            <Map className="text-red-500" size={20} />
                          ) : (
                            <FileText className="text-red-500" size={20} />
                          )}
                          <div>
                            <div className="flex items-center gap-2">
                              <p className="font-medium text-gray-800">{userName}</p>
                              <span className="text-xs px-2 py-0.5 rounded-full bg-red-100 text-red-600">
                                {isJourney ? 'Hành trình' : 'Tin rao'}
                              </span>
                            </div>
                            <p className="text-xs text-gray-500">
                              {creditAmount.toFixed(2)} tCO₂
                            </p>
                            <p className="text-xs text-gray-400">
                              Nộp: {formatDate(item.createdAt)}
                            </p>
                          </div>
                        </div>
                        <button
                          onClick={() => handleViewItem(item)}
                          className="bg-black text-white text-sm px-4 py-1.5 rounded-lg hover:bg-gray-800 transition"
                        >
                          Xem chi tiết
                        </button>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Recent Activities */}
            <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm">
              <h3 className="text-lg font-semibold text-gray-800 mb-2">
                Hoạt động gần đây
              </h3>
              <p className="text-sm text-gray-500 mb-4">
                Lịch sử xác thực mới nhất
              </p>

              {recentVerifications.length === 0 ? (
                <div className="text-center py-8">
                  <AlertCircle className="w-12 h-12 text-gray-400 mx-auto mb-3" />
                  <p className="text-gray-500 text-sm">Không có hoạt động gần đây</p>
                  <p className="text-gray-400 text-xs mt-1">
                    Lịch sử xác thực của bạn sẽ xuất hiện ở đây
                  </p>
                </div>
              ) : (
                <div className="space-y-3">
                  {recentVerifications.map((verification, index) => {
                    const isApproved = verification.verificationStatus === "VERIFIED" ||
                      verification.status === "APPROVED" ||
                      verification.status === "ACTIVE";
                    const isJourney = verification.journeyId !== undefined;

                    return (
                      <div
                        key={`verification-${verification.journeyId || verification.id || verification.listingId}-${index}`}
                        className="flex items-center justify-between"
                      >
                        <div className="flex items-center gap-2">
                          {isApproved ? (
                            <CheckCircle className="text-green-500" size={20} />
                          ) : (
                            <XCircle className="text-red-500" size={20} />
                          )}
                          <div>
                            <div className="flex items-center gap-2">
                              <p className="font-medium text-gray-800">
                                {verification.user?.username || verification.seller?.username || "Không xác định"}
                              </p>
                              <span className="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-600">
                                {isJourney ? 'Hành trình' : 'Tin rao'}
                              </span>
                            </div>
                            <p className="text-xs text-gray-500">
                              {(verification.creditsEarned || verification.creditAmount || verification.co2ReducedKg || 0).toFixed(2)} tCO₂
                            </p>
                            <p className="text-xs text-gray-400">
                              {formatDate(verification.verifiedAt || verification.updatedAt)}
                            </p>
                          </div>
                        </div>
                        <span
                          className={`text-xs px-3 py-1 rounded-md ${isApproved
                            ? "bg-gray-900 text-white"
                            : "bg-red-600 text-white"
                            }`}
                        >
                          {isApproved ? "Đã phê duyệt" : "Bị từ chối"}
                        </span>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
