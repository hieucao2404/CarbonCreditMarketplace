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
      setError("Failed to load dashboard data");
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
              <p className="text-gray-600">Loading dashboard...</p>
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
                Verification Overview
              </h1>
              <p className="text-gray-500 text-sm mt-1">
                Track and manage journey & listing verification requests
              </p>
            </div>
            <button
              onClick={loadDashboardData}
              className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition"
            >
              <RefreshCw className="w-4 h-4" />
              <span className="text-sm">Refresh</span>
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
              <p className="text-gray-500 text-sm">Pending Verification</p>
              <h2 className="text-3xl font-bold text-gray-800 mt-2">
                {totalPending}
              </h2>
              <p className="text-xs text-gray-400">
                {stats.pendingJourneysCount} journeys, {stats.pendingListingsCount} listings
              </p>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm">
              <p className="text-gray-500 text-sm">Approved This Month</p>
              <h2 className="text-3xl font-bold text-gray-800 mt-2">
                {stats.approvedThisMonth}
              </h2>
              <p className="text-xs text-gray-400">
                +{stats.rejectedThisMonth} requests rejected
              </p>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm">
              <p className="text-gray-500 text-sm">Total Verified</p>
              <h2 className="text-3xl font-bold text-gray-800 mt-2">
                {stats.totalVerified.toLocaleString()}
              </h2>
              <p className="text-xs text-gray-400">Total carbon credits</p>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm">
              <p className="text-gray-500 text-sm">Approval Rate</p>
              <h2 className="text-3xl font-bold text-gray-800 mt-2">
                {stats.approvalRate.toFixed(0)}%
              </h2>
              <p className="text-xs text-gray-400">This month</p>
            </div>
          </div>

          {/* Lower grid */}
          <div className="grid grid-cols-2 gap-6">
            {/* High Priority Requests */}
            <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm">
              <h3 className="text-lg font-semibold text-gray-800 mb-2">
                High Priority Requests
              </h3>
              <p className="text-sm text-gray-500 mb-4">
                Urgent journeys & listings requiring processing
              </p>

              {highPriorityItems.length === 0 ? (
                <div className="text-center py-8">
                  <CheckCircle className="w-12 h-12 text-green-500 mx-auto mb-3" />
                  <p className="text-gray-500 text-sm">No high priority requests</p>
                  <p className="text-gray-400 text-xs mt-1">
                    All requests are within normal processing time
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
                      ? (item.user?.username || item.user?.fullName || "Unknown User")
                      : (item.seller?.username || item.seller?.fullName || "Unknown Seller");

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
                                {isJourney ? 'Journey' : 'Listing'}
                              </span>
                            </div>
                            <p className="text-xs text-gray-500">
                              {creditAmount.toFixed(2)} tCO₂
                            </p>
                            <p className="text-xs text-gray-400">
                              Submitted: {formatDate(item.createdAt)}
                            </p>
                          </div>
                        </div>
                        <button
                          onClick={() => handleViewItem(item)}
                          className="bg-black text-white text-sm px-4 py-1.5 rounded-lg hover:bg-gray-800 transition"
                        >
                          View Details
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
                Recent Activities
              </h3>
              <p className="text-sm text-gray-500 mb-4">
                Latest verification history
              </p>

              {recentVerifications.length === 0 ? (
                <div className="text-center py-8">
                  <AlertCircle className="w-12 h-12 text-gray-400 mx-auto mb-3" />
                  <p className="text-gray-500 text-sm">No recent activities</p>
                  <p className="text-gray-400 text-xs mt-1">
                    Your verification history will appear here
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
                                {verification.user?.username || verification.seller?.username || "Unknown"}
                              </p>
                              <span className="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-600">
                                {isJourney ? 'Journey' : 'Listing'}
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
                          {isApproved ? "Approved" : "Rejected"}
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
