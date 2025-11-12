import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import VerifierSidebar from "../components/VerifierSidebar";
import VerifierHeader from "../components/VerifierHeader";
import {
  Eye, CheckCircle, XCircle, FileText, Calendar, User,
  DollarSign, RefreshCw, Map, Car, Send, Clock, Edit
} from "lucide-react";

// --- IMPORT CVA SERVICE (which has all functions) ---
import { cvaService } from "../services/cvaService"; //

export default function VerifierPendingAll() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("journeys"); // "journeys", "listings", or "inspections"
  const [listings, setListings] = useState([]);
  const [journeys, setJourneys] = useState([]);
  
  // --- NEW: State for inspections ---
  const [inspections, setInspections] = useState([]); 
  
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedItem, setSelectedItem] = useState(null);
  const [showRejectModal, setShowRejectModal] = useState(false);
  
  // --- NEW: State for completion modal ---
  const [showCompleteModal, setShowCompleteModal] = useState(false);
  const [completionNotes, setCompletionNotes] = useState("");
  const [isApprove, setIsApprove] = useState(true); // For the complete modal

  const [rejectReason, setRejectReason] = useState("");
  const [processing, setProcessing] = useState(false);
  const [modalType, setModalType] = useState(""); // "journey" or "listing"

  useEffect(() => {
    loadAllPending();
  }, []);

  const loadAllPending = async () => {
    setLoading(true);
    setError("");

    try {
      // --- MODIFIED: Fetch 3 lists ---
      const [listingsRes, journeysRes, inspectionsRes] = await Promise.allSettled([
        cvaService.getPendingListings(),
        cvaService.getPendingJourneys(),
        cvaService.getMyInspections() // <-- NEW call
      ]);

      if (listingsRes.status === "fulfilled" && listingsRes.value?.success) {
        const listingsData = listingsRes.value.data?.content || listingsRes.value.data || [];
        setListings(Array.isArray(listingsData) ? listingsData : []);
      }

      if (journeysRes.status === "fulfilled" && journeysRes.value?.success) {
        const journeysData = journeysRes.value.data || [];
        setJourneys(Array.isArray(journeysData) ? journeysData : []);
      }

      // --- Load inspections (filter for SCHEDULED only) ---
      if (inspectionsRes.status === "fulfilled") {
        // Fix: The response structure is value.data.data, not value.data
        const response = inspectionsRes.value?.data;
        if (response && response.success) {
          const inspectionsData = response.data || [];
          console.log("=== INSPECTIONS DEBUG ===");
          console.log("Total appointments received:", inspectionsData.length);
          console.log("Full data:", JSON.stringify(inspectionsData, null, 2));
          
          // Log each appointment status
          if (Array.isArray(inspectionsData)) {
            inspectionsData.forEach((appt, idx) => {
              console.log(`Appointment ${idx + 1}:`, {
                id: appt.id,
                status: appt.status,
                journeyId: appt.journeyId,
                appointmentTime: appt.appointmentTime
              });
            });
          }
          
          // Only show SCHEDULED appointments ready for physical verification/approval
          const scheduledInspections = Array.isArray(inspectionsData) 
            ? inspectionsData.filter(appt => appt.status === 'SCHEDULED')
            : [];
          
          console.log("SCHEDULED appointments after filter:", scheduledInspections.length);
          console.log("Filtered data:", scheduledInspections);
          setInspections(scheduledInspections);
        }
      } else {
        console.log("=== INSPECTIONS LOAD FAILED ===");
        console.log("Status:", inspectionsRes.status);
        console.log("Reason:", inspectionsRes.reason);
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
  const handleRequestInspection = async (journey) => {
    if (!window.confirm(`Request a physical inspection for journey ${journey.id.substring(0, 8)}? This will notify the EV Owner.`)) {
      return;
    }
    setProcessing(true);
    try {
      const response = await cvaService.requestInspection(journey.id);
      if (response.data.success) {
        alert("Inspection requested! The EV Owner has been notified.");
        loadAllPending(); // Reloads all lists
      } else {
        alert(`Failed to request inspection: ${response.data.message}`);
      }
    } catch (err) {
      alert(err.response?.data?.message || "Failed to request inspection");
    } finally {
      setProcessing(false);
    }
  };

  const handleRejectJourney = (journey) => {
    setSelectedItem(journey);
    setModalType("journey");
    setShowRejectModal(true);
    setRejectReason("");
  };

  // --- NEW: Inspection Handlers ---
  const openCompleteModal = (inspection, approve) => {
    setSelectedItem(inspection); // This is an InspectionAppointmentDTO
    setIsApprove(approve);
    setCompletionNotes("");
    setShowCompleteModal(true);
  };
  
  const handleCompleteSubmit = async () => {
    if (!completionNotes.trim()) {
      alert("Please provide completion notes or a rejection reason.");
      return;
    }
    setProcessing(true);
    try {
      const response = await cvaService.completeInspection(
        selectedItem.id, // appointmentId
        isApprove,
        completionNotes
      );
      
      if (response.data.success) {
        alert(`Inspection ${isApprove ? 'approved' : 'rejected'} successfully!`);
        setShowCompleteModal(false);
        setSelectedItem(null);
        loadAllPending(); // Reload all lists
      } else {
        alert(`Failed to complete inspection: ${response.data.message}`);
      }
    } catch (err) {
      alert(err.response?.data?.message || "Failed to complete inspection");
    } finally {
      setProcessing(false);
    }
  };


  // Modal Handlers
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
        setRejectReason("");
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
      ? ((item.co2ReducedKg || 0) / 1000) // Convert kg to tonnes
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

  // --- NEW: Logic to choose current list ---
  let currentItems = [];
  if (activeTab === 'journeys') {
    currentItems = journeys;
  } else if (activeTab === 'listings') {
    currentItems = listings;
  } else {
    currentItems = inspections;
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
              <h1 className="text-xl font-semibold text-gray-800">
                Verification Dashboard
              </h1>
              <p className="text-gray-500 mt-1">
                Request inspections, verify scheduled appointments, and approve listings
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

          {/* --- MODIFIED: Tabs --- */}
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
                <span>New Journeys ({journeys.length})</span>
              </div>
            </button>
            {/* --- NEW TAB (This is the "approving" tab) --- */}
            <button
              onClick={() => setActiveTab("inspections")}
              className={`px-6 py-3 font-medium transition-all ${activeTab === "inspections"
                ? "border-b-2 border-blue-600 text-blue-600"
                : "text-gray-500 hover:text-gray-700"
                }`}
            >
              <div className="flex items-center gap-2">
                <Edit className="w-4 h-4" />
                <span>My Inspections ({inspections.length})</span>
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
              ) : activeTab === "listings" ? (
                <FileText className="w-16 h-16 text-gray-400 mx-auto mb-4" />
              ) : (
                <Edit className="w-16 h-16 text-gray-400 mx-auto mb-4" />
              )}
              <h3 className="text-lg font-semibold text-gray-800 mb-2">
                No Pending {activeTab === "journeys" ? "Journeys" : activeTab === "listings" ? "Listings" : "Inspections"}
              </h3>
              <p className="text-gray-500">
                All {activeTab} have been processed or are waiting for action.
              </p>
            </div>
          ) : (
            <>
              {/* Count */}
              <div className="mb-4">
                 <p className="text-sm text-gray-600">
                  <span className="font-semibold">{currentItems.length}</span>{" "}
                  {activeTab === "journeys" ? "journey" : activeTab === "listings" ? "listing" : "inspection"}
                  {currentItems.length !== 1 ? "s" : ""} pending
                </p>
              </div>

              {/* === Journey Items (Tab 1) === */}
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
                              {((journey.co2ReducedKg || 0) / 1000).toFixed(4)} tCO₂
                            </span>
                          </p>
                        </div>
                      </div>
                    </div>
                    <div className="flex flex-col justify-start items-end gap-2 ml-6">
                      <button
                        onClick={() => handleRequestInspection(journey)}
                        disabled={processing}
                        className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 transition w-full"
                      >
                        <Send className="w-4 h-4" />
                        <span>Request Inspection</span>
                      </button>
                      <button
                        onClick={() => handleRejectJourney(journey)}
                        disabled={processing}
                        className="flex items-center space-x-2 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:bg-gray-400 transition w-full"
                      >
                        <XCircle className="w-4 h-4" />
                        <span>Reject</span>
                      </button>
                      <button
                        onClick={() => navigate(`/verifier/journey/${journey.id}`)}
                        className="flex items-center space-x-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-100 transition w-full"
                      >
                        <Eye className="w-4 h-4" />
                        <span>View</span>
                      </button>
                    </div>
                  </div>
                </div>
              ))}

              {/* === Inspection Items (Tab 2) === */}
              {activeTab === "inspections" && inspections.map((appt, index) => (
                <div
                  key={appt.id || index}
                  className="bg-white mt-4 p-5 rounded-xl shadow-sm border border-gray-100"
                >
                  <div className="flex justify-between">
                    <div className="flex-1">
                      <div className="flex items-center space-x-3 mb-3">
                        <span className="font-semibold text-gray-800">
                          Journey #{appt.journeyId?.substring(0, 8) || "N/A"}
                        </span>
                        {appt.status === 'SCHEDULED' ? (
                           <span className="bg-purple-100 text-purple-600 text-xs font-medium px-2 py-0.5 rounded-md">Scheduled</span>
                        ) : (
                           <span className="bg-blue-100 text-blue-600 text-xs font-medium px-2 py-0.5 rounded-md">Awaiting Owner</span>
                        )}
                      </div>

                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <p className="text-gray-700 flex items-center gap-2">
                            <User className="w-4 h-4" />
                            <strong>Owner:</strong> {appt.evOwner?.username || "Unknown"}
                          </p>
                          <p className="text-gray-700 flex items-center gap-2 mt-1">
                            <Calendar className="w-4 h-4" />
                            <strong>Requested:</strong> {formatDate(appt.createdAt)}
                          </p>
                        </div>
                        <div>
                          <p className="text-gray-700">
                            <strong>Status:</strong>{" "}
                            <span className="font-semibold">
                              {appt.status === 'SCHEDULED' ? 'Booked' : 'Waiting for owner to book'}
                            </span>
                          </p>
                          {appt.status === 'SCHEDULED' && (
                            <p className="text-gray-700 mt-1">
                              <strong>Time:</strong>{" "}
                              <span className="text-purple-600 font-semibold">
                                {new Date(appt.appointmentTime).toLocaleString()}
                              </span>
                            </p>
                          )}
                           {appt.status === 'SCHEDULED' && appt.station && (
                            <p className="text-gray-700 mt-1">
                              <strong>Location:</strong>{" "}
                              <span className="text-purple-600 font-semibold">
                                {appt.station.name}
                              </span>
                            </p>
                          )}
                        </div>
                      </div>
                    </div>

                    <div className="flex flex-col justify-start items-end gap-2 ml-6">
                      <button
                        onClick={() => openCompleteModal(appt, true)}
                        disabled={processing || appt.status !== 'SCHEDULED'}
                        className="flex items-center space-x-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:bg-gray-400 transition w-full"
                      >
                        <CheckCircle className="w-4 h-4" />
                        <span>Approve</span>
                      </button>
                      <button
                        onClick={() => openCompleteModal(appt, false)}
                        disabled={processing || appt.status !== 'SCHEDULED'}
                        className="flex items-center space-x-2 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:bg-gray-400 transition w-full"
                      >
                        <XCircle className="w-4 h-4" />
                        <span>Reject</span>
                      </button>
                      <button
                        onClick={() => navigate(`/verifier/journey/${appt.journeyId}`)}
                        className="flex items-center space-x-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-100 transition w-full"
                      >
                        <Eye className="w-4 h-4" />
                        <span>View Journey</span>
                      </button>
                    </div>
                  </div>
                </div>
              ))}

              {/* === Listing Items (Tab 3) === */}
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

      {/* --- NEW: Complete Inspection Modal --- */}
      {showCompleteModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 max-w-md w-full mx-4">
            <h3 className={`text-lg font-semibold ${isApprove ? 'text-green-800' : 'text-red-800'} mb-4`}>
              {isApprove ? "Complete & Approve Inspection" : "Complete & Reject Inspection"}
            </h3>
            <p className="text-gray-600 mb-4">
              {isApprove 
                ? "Please add any final verification notes."
                : "Please provide a clear reason for rejection."
              }
            </p>
            <textarea
              value={completionNotes}
              onChange={(e) => setCompletionNotes(e.target.value)}
              className="w-full border border-gray-300 rounded-lg p-3 mb-4 h-32"
              placeholder={isApprove ? "e.g., Verified VIN and odometer." : "e.g., Odometer reading mismatch."}
            />
            <div className="flex gap-3">
              <button
                onClick={() => {
                  setShowCompleteModal(false);
                  setSelectedItem(null);
                }}
                disabled={processing}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-100"
              >
                Cancel
              </button>
              <button
                onClick={handleCompleteSubmit}
                disabled={processing || !completionNotes.trim()}
                className={`flex-1 px-4 py-2 text-white rounded-lg disabled:bg-gray-400
                  ${isApprove ? 'bg-green-600 hover:bg-green-700' : 'bg-red-600 hover:bg-red-700'}`
                }
              >
                {processing ? "Submitting..." : (isApprove ? "Approve" : "Reject")}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Reject Modal (for Journeys and Listings) */}
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