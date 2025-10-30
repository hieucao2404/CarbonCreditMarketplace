import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import VerifierSidebar from "../components/VerifierSidebar";
import VerifierHeader from "../components/VerifierHeader";
import {
  Eye, CheckCircle, XCircle, FileText, Calendar, User,
  DollarSign, RefreshCw, Map, Car
} from "lucide-react";

// CVA Service
const cvaService = {
  getPendingListings: async (page = 0, size = 50) => {
    const response = await fetch(
      `http://localhost:8080/api/cva/pending-listings?page=${page}&size=${size}`,
      {
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
          "Content-Type": "application/json"
        }
      }
    );
    return response.json();
  },

  getPendingJourneys: async () => {
    const response = await fetch(
      "http://localhost:8080/api/cva/pending-journeys",
      {
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
          "Content-Type": "application/json"
        }
      }
    );
    return response.json();
  },

  approveListing: async (listingId) => {
    const response = await fetch(
      `http://localhost:8080/api/cva/listing/${listingId}/approve`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
          "Content-Type": "application/json"
        }
      }
    );
    return response.json();
  },

  rejectListing: async (listingId, reason) => {
    const response = await fetch(
      `http://localhost:8080/api/cva/listing/${listingId}/reject?reason=${encodeURIComponent(reason)}`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
          "Content-Type": "application/json"
        }
      }
    );
    return response.json();
  },

  approveJourney: async (journeyId, notes = "") => {
    const response = await fetch(
      `http://localhost:8080/api/cva/journey/${journeyId}/approve`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ notes })
      }
    );
    return response.json();
  },

  rejectJourney: async (journeyId, reason) => {
    const response = await fetch(
      `http://localhost:8080/api/cva/journey/${journeyId}/reject?reason=${encodeURIComponent(reason)}`,  // ✅ Query param
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
          "Content-Type": "application/json"
        }
        // No body needed
      }
    );
    return response.json();
  }

};

export default function VerifierPendingAll() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("journeys"); // "journeys" or "listings"
  const [listings, setListings] = useState([]);
  const [journeys, setJourneys] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedItem, setSelectedItem] = useState(null);
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [showApproveModal, setShowApproveModal] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const [approvalNotes, setApprovalNotes] = useState("");
  const [processing, setProcessing] = useState(false);
  const [modalType, setModalType] = useState(""); // "journey" or "listing"

  useEffect(() => {
    loadAllPending();
  }, []);

  const loadAllPending = async () => {
    setLoading(true);
    setError("");

    try {
      const [listingsRes, journeysRes] = await Promise.allSettled([
        cvaService.getPendingListings(),
        cvaService.getPendingJourneys()
      ]);

      if (listingsRes.status === "fulfilled" && listingsRes.value?.success) {
        const listingsData = listingsRes.value.data?.content || listingsRes.value.data || [];
        setListings(Array.isArray(listingsData) ? listingsData : []);
      }

      if (journeysRes.status === "fulfilled" && journeysRes.value?.success) {
        const journeysData = journeysRes.value.data || [];
        setJourneys(Array.isArray(journeysData) ? journeysData : []);
      }
    } catch (err) {
      console.error("Error loading data:", err);
      setError("Failed to load pending items");
    } finally {
      setLoading(false);
    }
  };

  // Listing Handlers
  const handleApproveListing = async (listing) => {
    if (!confirm(`Approve listing for ${(listing.creditAmount || 0).toFixed(2)} tCO₂?`)) {
      return;
    }

    setProcessing(true);
    try {
      const response = await cvaService.approveListing(listing.listingId);
      if (response.success) {
        alert("Listing approved successfully!");
        loadAllPending();
      } else {
        alert(`Failed to approve: ${response.message}`);
      }
    } catch (err) {
      alert("Failed to approve listing");
    } finally {
      setProcessing(false);
    }
  };

  const handleRejectListing = (listing) => {
    setSelectedItem(listing);
    setModalType("listing");
    setShowRejectModal(true);
    setRejectReason("");
  };

  // Journey Handlers
  const handleApproveJourney = (journey) => {
    setSelectedItem(journey);
    setModalType("journey");
    setShowApproveModal(true);
    setApprovalNotes("");
  };

  const handleRejectJourney = (journey) => {
    setSelectedItem(journey);
    setModalType("journey");
    setShowRejectModal(true);
    setRejectReason("");
  };

  // Modal Handlers
  const handleApproveSubmit = async () => {
    setProcessing(true);
    try {
      const response = await cvaService.approveJourney(selectedItem.id, approvalNotes);
      if (response.success) {
        alert("Journey approved successfully!");
        setShowApproveModal(false);
        setSelectedItem(null);
        loadAllPending();
      } else {
        alert(`Failed to approve: ${response.message}`);
      }
    } catch (err) {
      alert("Failed to approve journey");
    } finally {
      setProcessing(false);
    }
  };

  const handleRejectSubmit = async () => {
    if (!rejectReason.trim()) {
      alert("Please provide a rejection reason");
      return;
    }

    setProcessing(true);
    try {
      let response;
      if (modalType === "journey") {
        response = await cvaService.rejectJourney(selectedItem.id, rejectReason);
      } else {
        response = await cvaService.rejectListing(selectedItem.listingId, rejectReason);
      }

      if (response.success) {
        alert(`${modalType === "journey" ? "Journey" : "Listing"} rejected successfully!`);
        setShowRejectModal(false);
        setSelectedItem(null);
        setRejectReason("");  // ✅ Add this to clear the reason
        loadAllPending();
      } else {
        alert(`Failed to reject: ${response.message}`);
      }
    } catch (err) {
      alert(`Failed to reject ${modalType}`);
    } finally {
      setProcessing(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric"
    });
  };

  const getPriorityBadge = (item, type) => {
    const amount = type === "journey"
      ? ((item.co2ReducedKg || 0) / 1000) // ✅ Convert kg to tonnes
      : (item.creditAmount || 0);
    const hours = (new Date() - new Date(item.createdAt)) / (1000 * 60 * 60);

    const threshold = type === "journey" ? { high: 0.02, medium: 0.01 } : { high: 50, medium: 20 };
    const timeThreshold = type === "journey" ? { high: 48, medium: 24 } : { high: 72, medium: 48 };

    if (amount > threshold.high || hours > timeThreshold.high) {
      return <span className="bg-red-100 text-red-600 text-xs font-medium px-2 py-0.5 rounded-md">High Priority</span>;
    } else if (amount > threshold.medium || hours > timeThreshold.medium) {
      return <span className="bg-yellow-100 text-yellow-600 text-xs font-medium px-2 py-0.5 rounded-md">Medium Priority</span>;
    } else {
      return <span className="bg-blue-100 text-blue-600 text-xs font-medium px-2 py-0.5 rounded-md">Normal</span>;
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
              <p className="text-gray-600">Loading pending items...</p>
            </div>
          </main>
        </div>
      </div>
    );
  }

  const currentItems = activeTab === "journeys" ? journeys : listings;

  return (
    <div className="flex h-screen bg-gray-50">
      <VerifierSidebar />
      <div className="flex-1 flex flex-col">
        <VerifierHeader />

        <main className="flex-1 overflow-y-auto p-8">
          {/* Header */}
          <div className="flex justify-between items-center mb-6">
            <div>
              <h1 className="text-xl font-semibold text-gray-800">
                Pending Approvals
              </h1>
              <p className="text-gray-500 mt-1">
                Review and approve journey verifications and listing requests
              </p>
            </div>
            <button
              onClick={loadAllPending}
              className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition"
            >
              <RefreshCw className="w-4 h-4" />
              <span className="text-sm">Refresh</span>
            </button>
          </div>

          {/* Tabs */}
          <div className="flex gap-2 mb-6 border-b border-gray-200">
            <button
              onClick={() => setActiveTab("journeys")}
              className={`px-6 py-3 font-medium transition-all ${activeTab === "journeys"
                ? "border-b-2 border-green-600 text-green-600"
                : "text-gray-500 hover:text-gray-700"
                }`}
            >
              <div className="flex items-center gap-2">
                <Map className="w-4 h-4" />
                <span>Journeys ({journeys.length})</span>
              </div>
            </button>
            <button
              onClick={() => setActiveTab("listings")}
              className={`px-6 py-3 font-medium transition-all ${activeTab === "listings"
                ? "border-b-2 border-green-600 text-green-600"
                : "text-gray-500 hover:text-gray-700"
                }`}
            >
              <div className="flex items-center gap-2">
                <FileText className="w-4 h-4" />
                <span>Listings ({listings.length})</span>
              </div>
            </button>
          </div>

          {/* Error Alert */}
          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-red-600 text-sm">{error}</p>
            </div>
          )}

          {/* Empty State */}
          {currentItems.length === 0 ? (
            <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-12 text-center">
              {activeTab === "journeys" ? (
                <Map className="w-16 h-16 text-gray-400 mx-auto mb-4" />
              ) : (
                <FileText className="w-16 h-16 text-gray-400 mx-auto mb-4" />
              )}
              <h3 className="text-lg font-semibold text-gray-800 mb-2">
                No Pending {activeTab === "journeys" ? "Journeys" : "Listings"}
              </h3>
              <p className="text-gray-500">
                All {activeTab === "journeys" ? "journey verification" : "listing"} requests have been processed
              </p>
            </div>
          ) : (
            <>
              {/* Count */}
              <div className="mb-4">
                <p className="text-sm text-gray-600">
                  <span className="font-semibold">{currentItems.length}</span>{" "}
                  {activeTab === "journeys" ? "journey" : "listing"}
                  {currentItems.length !== 1 ? "s" : ""} pending {activeTab === "journeys" ? "verification" : "approval"}
                </p>
              </div>

              {/* Journey Items */}
              {activeTab === "journeys" && journeys.map((journey, index) => (
                <div
                  key={journey.id || index}
                  className="bg-white mt-4 p-5 rounded-xl shadow-sm border border-gray-100"
                >
                  <div className="flex justify-between">
                    <div className="flex-1">
                      <div className="flex items-center space-x-3 mb-3">
                        <span className="font-semibold text-gray-800">
                          {journey.id?.substring(0, 8) || "N/A"}
                        </span>
                        {getPriorityBadge(journey, "journey")}
                        <span className="bg-yellow-100 text-yellow-600 text-xs font-medium px-2 py-0.5 rounded-md">
                          Pending Verification
                        </span>
                      </div>
                      {/* Journey Details - Updated field mapping */}
                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <p className="text-gray-700 flex items-center gap-2">
                            <User className="w-4 h-4" />
                            <strong>Owner:</strong> {journey.user?.username || "Unknown"}
                          </p>
                          <p className="text-gray-700 flex items-center gap-2 mt-1">
                            <Car className="w-4 h-4" />
                            <strong>Vehicle:</strong> {journey.vehicle?.make || "N/A"} {journey.vehicle?.model || ""}
                          </p>
                          <p className="text-gray-700 flex items-center gap-2 mt-1">
                            <Calendar className="w-4 h-4" />
                            <strong>Created:</strong> {formatDate(journey.createdAt)}
                          </p>
                        </div>

                        <div>
                          <p className="text-gray-700">
                            <strong>Distance:</strong>{" "}
                            {/* ✅ Use distanceKm instead of distance */}
                            <span className="font-semibold">
                              {(journey.distanceKm || 0).toFixed(2)} km
                            </span>
                          </p>
                          <p className="text-gray-700 mt-1">
                            <strong>CO₂ Reduced:</strong>{" "}
                            <span className="text-green-600 font-semibold">
                              {(journey.co2ReducedKg || 0).toFixed(2)} kg
                            </span>
                          </p>
                          <p className="text-gray-700 mt-1">
                            <strong>Credits:</strong>{" "}
                            <span className="text-blue-600 font-semibold">
                              {/* ✅ Convert kg to tonnes: divide by 1000 */}
                              {((journey.co2ReducedKg || 0) / 1000).toFixed(4)} tCO₂
                            </span>
                          </p>
                        </div>
                      </div>

                    </div>

                    <div className="flex flex-col justify-start items-end gap-2 ml-6">
                      <button
                        onClick={() => handleApproveJourney(journey)}
                        disabled={processing}
                        className="flex items-center space-x-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:bg-gray-400 transition"
                      >
                        <CheckCircle className="w-4 h-4" />
                        <span>Approve</span>
                      </button>
                      <button
                        onClick={() => handleRejectJourney(journey)}
                        disabled={processing}
                        className="flex items-center space-x-2 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:bg-gray-400 transition"
                      >
                        <XCircle className="w-4 h-4" />
                        <span>Reject</span>
                      </button>
                      <button
                        onClick={() => navigate(`/verifier/journey/${journey.id}`)}
                        className="flex items-center space-x-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-100 transition"
                      >
                        <Eye className="w-4 h-4" />
                        <span>View</span>
                      </button>
                    </div>
                  </div>
                </div>
              ))}

              {/* Listing Items */}
              {activeTab === "listings" && listings.map((listing, index) => (
                <div
                  key={listing.listingId || index}
                  className="bg-white mt-4 p-5 rounded-xl shadow-sm border border-gray-100"
                >
                  <div className="flex justify-between">
                    <div className="flex-1">
                      <div className="flex items-center space-x-3 mb-3">
                        <span className="font-semibold text-gray-800">
                          #{listing.listingId?.substring(0, 8) || "N/A"}
                        </span>
                        {getPriorityBadge(listing, "listing")}
                        <span className="bg-yellow-100 text-yellow-600 text-xs font-medium px-2 py-0.5 rounded-md">
                          Pending Approval
                        </span>
                      </div>

                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <p className="text-gray-700 flex items-center gap-2">
                            <User className="w-4 h-4" />
                            <strong>Seller:</strong> {listing.sellerUsername || "Unknown"}
                          </p>
                          <p className="text-gray-700 flex items-center gap-2 mt-1">
                            <Calendar className="w-4 h-4" />
                            <strong>Created:</strong> {formatDate(listing.createdAt)}
                          </p>
                        </div>

                        <div>
                          <p className="text-gray-700">
                            <strong>Credit Amount:</strong>{" "}
                            <span className="text-green-600 font-semibold">
                              {(listing.creditAmount || 0).toFixed(2)} tCO₂
                            </span>
                          </p>
                          <p className="text-gray-700 flex items-center gap-2 mt-1">
                            <DollarSign className="w-4 h-4" />
                            <strong>Price:</strong> ${(listing.price || 0).toLocaleString()}/tCO₂
                          </p>
                        </div>
                      </div>

                      <div className="mt-2">
                        <span className="text-xs px-2 py-1 bg-gray-100 rounded-md">
                          {listing.type || "FIXED"}
                        </span>
                      </div>
                    </div>

                    <div className="flex flex-col justify-start items-end gap-2 ml-6">
                      <button
                        onClick={() => handleApproveListing(listing)}
                        disabled={processing}
                        className="flex items-center space-x-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:bg-gray-400 transition"
                      >
                        <CheckCircle className="w-4 h-4" />
                        <span>Approve</span>
                      </button>
                      <button
                        onClick={() => handleRejectListing(listing)}
                        disabled={processing}
                        className="flex items-center space-x-2 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:bg-gray-400 transition"
                      >
                        <XCircle className="w-4 h-4" />
                        <span>Reject</span>
                      </button>
                      <button
                        onClick={() => navigate(`/verifier/listing/${listing.listingId}`)}
                        className="flex items-center space-x-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-100 transition"
                      >
                        <Eye className="w-4 h-4" />
                        <span>View</span>
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </>
          )}
        </main>
      </div>

      {/* Approve Modal (Journey only) */}
      {showApproveModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">Approve Journey</h3>
            <p className="text-gray-600 mb-2">
              Journey ID: #{selectedItem?.id?.substring(0, 8)}
            </p>
            <p className="text-gray-600 mb-4">
              Credits: <span className="font-semibold text-green-600">
                {(selectedItem?.creditsEarned || 0).toFixed(2)} tCO₂
              </span>
            </p>
            <textarea
              value={approvalNotes}
              onChange={(e) => setApprovalNotes(e.target.value)}
              className="w-full border border-gray-300 rounded-lg p-3 mb-4 h-24"
              placeholder="Optional notes..."
            />
            <div className="flex gap-3">
              <button
                onClick={() => {
                  setShowApproveModal(false);
                  setSelectedItem(null);
                }}
                disabled={processing}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-100"
              >
                Cancel
              </button>
              <button
                onClick={handleApproveSubmit}
                disabled={processing}
                className="flex-1 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700"
              >
                {processing ? "Approving..." : "Confirm"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Reject Modal */}
      {showRejectModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">
              Reject {modalType === "journey" ? "Journey" : "Listing"}
            </h3>
            <p className="text-gray-600 mb-4">
              Please provide a reason:
            </p>
            <textarea
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              className="w-full border border-gray-300 rounded-lg p-3 mb-4 h-32"
              placeholder="Enter rejection reason..."
            />
            <div className="flex gap-3">
              <button
                onClick={() => {
                  setShowRejectModal(false);
                  setSelectedItem(null);
                }}
                disabled={processing}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-100"
              >
                Cancel
              </button>
              <button
                onClick={handleRejectSubmit}
                disabled={processing || !rejectReason.trim()}
                className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:bg-gray-400"
              >
                {processing ? "Rejecting..." : "Confirm"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
