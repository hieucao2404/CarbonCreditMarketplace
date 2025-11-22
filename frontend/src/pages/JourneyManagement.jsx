import React, { useEffect, useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import Sidebar from "../components/Sidebar";
import Header from "../components/Header";
import { journeyService } from "../services/journeyService";
import { vehicleService } from "../services/vehicleService";
import { verificationService } from "../services/verificationService"; 
import { 
  MapPin, 
  Trash2, 
  Plus, 
  Calendar, 
  Zap, 
  ArrowLeft, 
  Car,
  CheckCircle,
  Clock,
  XCircle,
  Send
} from "lucide-react";

// --- ScheduleModal (NO CHANGES) ---
function ScheduleModal({ journey, onClose, onSuccess }) {
  const [stations, setStations] = useState([]);
  const [loadingStations, setLoadingStations] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [selectedStationId, setSelectedStationId] = useState("");
  const [selectedTime, setSelectedTime] = useState("");

  useEffect(() => {
    verificationService.getActiveStations()
      .then(res => {
        if (res.data.success && Array.isArray(res.data.data)) { // Check for array
          setStations(res.data.data);
          if (res.data.data.length > 0) {
            setSelectedStationId(res.data.data[0].id);
          }
        } else {
          setStations([]); // Set to empty array on fail
          setError("Could not load verification stations.");
        }
      })
      .catch(err => {
        console.error("Error fetching stations:", err);
        setError("Could not load verification stations.");
      })
      .finally(() => setLoadingStations(false));
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedStationId || !selectedTime) {
      setError("Please select a station and a time.");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      const appointmentId = journey.appointmentId; 
      if (!appointmentId) {
        setError("Error: This journey has no linked appointment. Please contact support.");
        setSubmitting(false);
        return;
      }
      const response = await verificationService.scheduleAppointment(
        appointmentId,
        selectedStationId,
        selectedTime
      );
      if (response.data.success) {
        onSuccess("Appointment scheduled successfully!");
      } else {
        setError(response.data.message || "Failed to schedule appointment.");
      }
    } catch (err) {
      console.error("Error scheduling:", err);
      setError(err.response?.data?.message || "An error occurred.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-2xl p-8 max-w-lg w-full">
        <h2 className="text-xl font-semibold text-gray-800 mb-4">
          Đặt lịch kiểm tra
        </h2>
        <p className="text-sm text-gray-600 mb-6">
          Một CVA đã yêu cầu kiểm tra thực tế cho hành trình của bạn
          (<span className="font-medium">{journey.distanceKm.toFixed(1)} km</span>).
          Vui lòng chọn trạm và thời gian.
        </p>

        {error && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded-lg text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label className="text-sm font-medium text-gray-700 mb-2 block">
              1. Chọn Trạm Xác Minh
            </label>
            {loadingStations ? (
              <p>Đang tải trạm...</p>
            ) : stations.length === 0 ? (
              <p className="text-sm text-yellow-700">Không tìm thấy trạm xác minh hoạt động nào. Vui lòng liên hệ bộ phận hỗ trợ.</p>
            ) : (
              <select
                value={selectedStationId}
                onChange={(e) => setSelectedStationId(e.target.value)}
                required
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500"
              >
                <option value="" disabled>Select a station</option>
                {stations.map(station => (
                  <option key={station.id} value={station.id}>
                    {station.name} ({station.address})
                  </option>
                ))}
              </select>
            )}
          </div>
          <div className="mb-6">
            <label className="text-sm font-medium text-gray-700 mb-2 block">
              2. Chọn ngày và giờ
            </label>
            <input
              type="datetime-local"
              value={selectedTime}
              onChange={(e) => setSelectedTime(e.target.value)}
              required
              min={new Date().toISOString().slice(0, 16)}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500"
            />
          </div>
          <div className="flex gap-4">
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              className="flex-1 px-6 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition font-medium"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting || loadingStations || !selectedStationId || !selectedTime}
              className="flex-1 px-6 py-3 rounded-lg font-medium transition bg-green-600 text-white hover:bg-green-700 disabled:bg-gray-400"
            >
              {submitting ? "Booking..." : "Book Appointment"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
// --- END OF MODAL COMPONENT ---


export default function JourneyManagement() {
  const navigate = useNavigate();
  const [journeys, setJourneys] = useState([]);
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [showAddForm, setShowAddForm] = useState(false);

  const [showScheduleModal, setShowScheduleModal] = useState(false);
  const [selectedJourney, setSelectedJourney] = useState(null);

  const [newJourney, setNewJourney] = useState({
    vehicleId: "",
    distanceKm: "",
    energyConsumedKwh: "",
    journeyDate: new Date().toISOString().split("T")[0],
    startLocation: "",
    endLocation: "",
    notes: "",
  });

  // --- New modal states to replace native alert/confirm ---
  const [showConfirm, setShowConfirm] = useState(false);
  const [confirmMessage, setConfirmMessage] = useState("");
  const confirmActionRef = useRef(null);

  const [showMessageModal, setShowMessageModal] = useState(false);
  const [messageModalMessage, setMessageModalMessage] = useState("");

  useEffect(() => {
    loadData();
  }, []);

  // --- 
  // --- THIS FUNCTION IS NOW FIXED ---
  // ---
  const loadData = async () => {
    setLoading(true);
    try {
      const [journeysRes, vehiclesRes] = await Promise.all([
        journeyService.getMyJourneys(),
        vehicleService.getMyVehicles(),
      ]);

      // --- FIX: Check if data is an array (not null) before sorting ---
      if (journeysRes.success && Array.isArray(journeysRes.data)) {
        const sortedJourneys = journeysRes.data.sort(
          (a, b) => new Date(b.journeyDate) - new Date(a.journeyDate)
        );
        setJourneys(sortedJourneys);
      } else {
        setJourneys([]); // Set to empty array on fail
      }

      // --- FIX: Check if data is an array (not null) before setting ---
      if (vehiclesRes.success && Array.isArray(vehiclesRes.data)) {
        setVehicles(vehiclesRes.data);
        // Only set default vehicle if the array is not empty
        if (vehiclesRes.data.length > 0) {
          setNewJourney((prev) => ({ ...prev, vehicleId: vehiclesRes.data[0].id }));
        }
      } else {
        setVehicles([]); // Set to empty array on fail
      }
    } catch (err) {
      console.error("❌ Error loading data:", err);
      setError("Failed to load data. Please try again.");
      setJourneys([]); // Also set to empty on crash
      setVehicles([]);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setNewJourney((prev) => ({
      ...prev,
      [name]: value,
    }));
    if (error) setError("");
  };

  const validateForm = () => {
    if (!newJourney.vehicleId) {
      setError("Please select a vehicle");
      return false;
    }
    if (!newJourney.distanceKm || parseFloat(newJourney.distanceKm) <= 0) {
      setError("Distance must be greater than 0");
      return false;
    }
    if (!newJourney.energyConsumedKwh || parseFloat(newJourney.energyConsumedKwh) <= 0) {
      setError("Energy consumed must be greater than 0");
      return false;
    }
    if (!newJourney.journeyDate) {
      setError("Please select a journey date");
      return false;
    }
    const selectedDate = new Date(newJourney.journeyDate);
    const today = new Date();
    today.setHours(23, 59, 59, 999);
    if (selectedDate > today) {
      setError("Journey date cannot be in the future");
      return false;
    }
    return true;
  };

  const handleAddJourney = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");

    if (!validateForm()) return;

    setSubmitting(true);

    try {
      const journeyData = {
        vehicle: { id: newJourney.vehicleId },
        distanceKm: parseFloat(newJourney.distanceKm),
        energyConsumedKwh: parseFloat(newJourney.energyConsumedKwh),
        journeyDate: newJourney.journeyDate,
        startLocation: newJourney.startLocation || null,
        endLocation: newJourney.endLocation || null,
        notes: newJourney.notes || null,
      };

      const response = await journeyService.createJourney(journeyData);

      if (response.success) {
        setSuccess("✅ Journey added successfully! Awaiting CVA verification.");
        setJourneys(prev => [response.data, ...prev]);
        setNewJourney({
          vehicleId: vehicles.length > 0 ? vehicles[0].id : "",
          distanceKm: "",
          energyConsumedKwh: "",
          journeyDate: new Date().toISOString().split("T")[0],
          startLocation: "",
          endLocation: "",
          notes: "",
        });
        setShowAddForm(false);

        setTimeout(() => setSuccess(""), 3000);
      } else {
        setError(response.message || "Failed to add journey");
      }
    } catch (err) {
      console.error("❌ Error adding journey:", err);
      setError(err.response?.data?.message || "Failed to add journey. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  // --- REPLACED native confirm with in-app confirm modal ---
  const handleDelete = (id, distance) => {
    // Open confirm modal and store action in ref
    setConfirmMessage(`Are you sure you want to delete this ${distance} km journey?`);
    confirmActionRef.current = async () => {
      try {
        const response = await journeyService.deleteJourney(id);

        if (response.success) {
          setSuccess("🗑️ Journey deleted successfully!");
          setJourneys((prev) => prev.filter((j) => j.id !== id));

          setTimeout(() => setSuccess(""), 3000);
        } else {
          setError(response.message || "Failed to delete journey");
        }
      } catch (err) {
        console.error("❌ Error deleting journey:", err);
        setError(err.response?.data?.message || "Failed to delete journey. Please try again.");
      }
    };
    setShowConfirm(true);
  };

  const confirmExecute = async () => {
    setShowConfirm(false);
    const action = confirmActionRef.current;
    if (typeof action === 'function') {
      await action();
    }
    confirmActionRef.current = null;
  };

  const confirmCancel = () => {
    setShowConfirm(false);
    confirmActionRef.current = null;
  };

  const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    const date = new Date(dateString);
    return date.toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  const getVehicleModel = (vehicleId) => {
    const vehicle = vehicles.find((v) => v.id === vehicleId);
    return vehicle ? vehicle.model : "Unknown Vehicle";
  };

  const getStatusBadge = (journey) => {
    const journeyStatus = journey.verificationStatus;
    const apptStatus = journey.appointmentStatus; // <-- Get the new field

    let config;
    const statusConfig = {
      VERIFIED: { icon: CheckCircle, color: "bg-green-100 text-green-700 border-green-200", label: "Verified" },
      PENDING_VERIFICATION: { icon: Clock, color: "bg-yellow-100 text-yellow-700 border-yellow-200", label: "Pending CVA" },
      PENDING_INSPECTION: { icon: Send, color: "bg-blue-100 text-blue-700 border-blue-200", label: "Inspection Required" },
      SCHEDULED: { icon: Calendar, color: "bg-purple-100 text-purple-700 border-purple-200", label: "Inspection Scheduled" },
      REJECTED: { icon: XCircle, color: "bg-red-100 text-red-700 border-red-200", label: "Rejected" },
    };

    if (journeyStatus === 'PENDING_INSPECTION') {
      // If journey is PENDING_INSPECTION, check the appointment status
      if (apptStatus === 'SCHEDULED') {
        config = statusConfig.SCHEDULED;
      } else {
        // Default to PENDING_INSPECTION (which means REQUESTED)
        config = statusConfig.PENDING_INSPECTION;
      }
    } else {
      // For all other journey statuses, use them directly
      config = statusConfig[journeyStatus] || statusConfig.PENDING_VERIFICATION;
    }
    
    const Icon = config.icon;

    return (
      <div className={`flex items-center gap-2 px-3 py-1 rounded-full border ${config.color}`}>
        <Icon className="w-4 h-4" />
        <span className="text-sm font-medium">{config.label}</span>
      </div>
    );
  };

  // --- Modal handler functions (NO CHANGE except the alert -> message modal) ---
  const openScheduleModal = (journey) => {
    if (!journey.appointmentId) {
      // Replaced native alert with in-app message modal
      setMessageModalMessage("Error: Cannot find appointment ID for this journey. Please contact support.");
      setShowMessageModal(true);
      return;
    }
    setSelectedJourney(journey);
    setShowScheduleModal(true);
  };

  const closeScheduleModal = () => {
    setSelectedJourney(null);
    setShowScheduleModal(false);
  };

  const handleScheduleSuccess = (message) => {
    setSuccess(message);
    closeScheduleModal();
    loadData(); 
    setTimeout(() => setSuccess(""), 3000);
  };

  return (
    <div className="flex min-h-screen w-screen bg-[#F9FAFB]">
      <Sidebar />
      <div className="flex flex-col flex-1">
        <Header />
        <main className="flex-1 p-8">
          {/* Back Button */}
          <button
            onClick={() => navigate("/home")}
            className="flex items-center gap-2 text-gray-600 hover:text-gray-800 mb-6 transition"
          >
            <ArrowLeft className="w-5 h-5" />
            <span>Quay lại Bảng điều khiển</span>
          </button>

          {/* Page Header */}
          <div className="flex justify-between items-center mb-8">
            <div>
              <h1 className="text-3xl font-bold text-gray-800 mb-2">
                Quản lý hành trình
              </h1>
              <p className="text-gray-600">
                Theo dõi các hành trình EV của bạn và kiếm tín chỉ carbon
              </p>
            </div>

            {/* This button will now appear once loading is false and the form is hidden */}
            {!showAddForm && !loading && ( // <-- Added !loading check
              <button
                onClick={() => setShowAddForm(true)}
                disabled={vehicles.length === 0}
                className={`flex items-center gap-2 px-6 py-3 rounded-lg font-medium shadow-sm transition ${
                  vehicles.length === 0
                    ? "bg-gray-400 cursor-not-allowed text-white"
                    : "bg-green-600 text-white hover:bg-green-700"
                }`}
              >
                <Plus className="w-5 h-5" />
                Thêm hành trình
              </button>
            )}
          </div>

          {/* Alert Messages */}
          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-3">
              <span className="text-2xl">⚠️</span>
              <div>
                <p className="text-red-800 font-medium">Lỗi</p>
                <p className="text-red-600 text-sm">{error}</p>
              </div>
            </div>
          )}

          {success && (
            <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg flex items-center gap-3">
              <span className="text-2xl">✅</span>
              <div>
                <p className="text-green-800 font-medium">Thành công!</p>
                <p className="text-green-600 text-sm">{success}</p>
              </div>
            </div>
          )}

          {/* No Vehicles Warning */}
          {/* --- FIX: This check is now safe --- */}
          {vehicles.length === 0 && !loading && (
            <div className="mb-6 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
              <p className="text-yellow-800 font-medium mb-2">
                📌 Không tìm thấy xe nào
              </p>
              <p className="text-yellow-700 text-sm mb-3">
                Bạn cần thêm xe trước khi ghi lại các hành trình.
              </p>
              <button
                onClick={() => navigate("/vehicles")}
                className="text-sm bg-yellow-600 text-white px-4 py-2 rounded-lg hover:bg-yellow-700 transition"
              >
                Đi đến Quản lý Xe →
              </button>
            </div>
          )}

          {/* Add Journey Form */}
          {showAddForm && (
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8 mb-8">
             <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-semibold text-gray-800">
                  Thêm hành trình mới
                </h2>
                <button
                  onClick={() => {
                    setShowAddForm(false);
                    setError("");
                  }}
                  className="text-gray-500 hover:text-gray-700"
                >
                  ✕
                </button>
              </div>
             <form onSubmit={handleAddJourney}>
                <div className="mb-6">
                  <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                    <Car className="w-4 h-4" />
                    Chọn xe <span className="text-red-600">*</span>
                  </label>
                  <select
                    name="vehicleId"
                    value={newJourney.vehicleId}
                    onChange={handleChange}
                    required
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
                  >
                    {/* --- FIX: Safe map --- */}
                    {vehicles.map((vehicle) => (
                      <option key={vehicle.id} value={vehicle.id}>
                        {vehicle.model} - {vehicle.vin}
                      </option>
                    ))}
                  </select>
                </div>
                 <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
                  <div>
                    <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                      <MapPin className="w-4 h-4" />
                      Khoảng cách (km) <span className="text-red-600">*</span>
                    </label>
                    <input
                      type="number"
                      name="distanceKm"
                      value={newJourney.distanceKm}
                      onChange={handleChange}
                      step="0.1"
                      min="0.1"
                      required
                      placeholder="45.2"
                      className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
                    />
                  </div>
                  <div>
                    <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                      <Zap className="w-4 h-4" />
                      Năng lượng tiêu thụ (kWh) <span className="text-red-600">*</span>
                    </label>
                    <input
                      type="number"
                      name="energyConsumedKwh"
                      value={newJourney.energyConsumedKwh}
                      onChange={handleChange}
                      step="0.01"
                      min="0.01"
                      required
                      placeholder="12.5"
                      className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
                    />
                  </div>
                  <div>
                    <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                      <Calendar className="w-4 h-4" />
                      Ngày <span className="text-red-600">*</span>
                    </label>
                    <input
                      type="date"
                      name="journeyDate"
                      value={newJourney.journeyDate}
                      onChange={handleChange}
                      max={new Date().toISOString().split("T")[0]}
                      required
                      className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
                    />
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-6 mb-6">
                  <div>
                    <label className="text-sm font-medium text-gray-700 mb-2 block">
                     Điểm xuất phát (Tùy chọn)
                    </label>
                    <input
                      type="text"
                      name="startLocation"
                      value={newJourney.startLocation}
                      onChange={handleChange}
                      placeholder="e.g., Hanoi"
                      className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
                    />
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-700 mb-2 block">
                      Điểm đến (Tùy chọn)
                    </label>
                    <input
                      type="text"
                      name="endLocation"
                      value={newJourney.endLocation}
                      onChange={handleChange}
                      placeholder="e.g., Hai Phong"
                      className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
                    />
                  </div>
                </div>
                <div className="mb-6">
                  <label className="text-sm font-medium text-gray-700 mb-2 block">
                    Ghi chú (Tùy chọn)
                  </label>
                  <textarea
                    name="notes"
                    value={newJourney.notes}
                    onChange={handleChange}
                    rows="2"
                    placeholder="Additional information..."
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent resize-none"
                  />
                </div>
                <div className="flex gap-4">
                  <button
                    type="button"
                    onClick={() => {
                      setShowAddForm(false);
                      setError("");
                    }}
                    className="flex-1 px-6 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition font-medium"
                  >
                    Hủy bỏ
                  </button>
                  <button
                    type="submit"
                    disabled={submitting}
                    className={`flex-1 px-6 py-3 rounded-lg font-medium transition ${
                      submitting
                        ? "bg-gray-400 cursor-not-allowed text-white"
                        : "bg-green-600 text-white hover:bg-green-700"
                    }`}
                  >
                    {submitting ? "Đang thêm..." : "Thêm hành trình"}
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* Journey List */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
            <h2 className="text-xl font-semibold text-gray-800 mb-6">
              Các hành trình của bạn ({journeys.length})
            </h2>

            {loading ? (
              <div className="text-center py-12">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto"></div>
                <p className="text-gray-500 text-sm mt-4">Đang tải các hành trình...</p>
              </div>
            ) : journeys.length === 0 ? (
              <div className="text-center py-12 border border-dashed border-gray-300 rounded-lg">
                <div className="mb-4">
                  <MapPin className="w-16 h-16 text-gray-400 mx-auto" />
                </div>
                <p className="text-gray-600 font-medium mb-2">
                  Chưa có hành trình nào được ghi lại
                </p>
                <p className="text-gray-500 text-sm mb-4">
                  Thêm hành trình EV đầu tiên của bạn để bắt đầu nhận tín chỉ carbon
                </p>
                {/* --- FIX: Safe check --- */}
                {!showAddForm && vehicles.length > 0 && (
                  <button
                    onClick={() => setShowAddForm(true)}
                    className="inline-flex items-center gap-2 bg-green-600 text-white px-6 py-3 rounded-lg hover:bg-green-700 transition font-medium"
                  >
                    <Plus className="w-5 h-5" />
                    Thêm hành trình đầu tiên
                  </button>
                )}
              </div>
            ) : (
              <div className="space-y-4">
                {journeys.map((journey) => (
                  <div
                    key={journey.id}
                    className="p-6 border border-gray-200 rounded-lg hover:shadow-md transition"
                  >
                    <div className="flex justify-between items-start mb-4">
                      <div className="flex-1">
                        <div className="flex items-center gap-3 mb-2">
                          <h3 className="text-lg font-semibold text-gray-800">
                            {getVehicleModel(journey.vehicle?.id)}
                          </h3>
                          {getStatusBadge(journey)}
                        </div>
                        <p className="text-sm text-gray-600 mb-1">
                          <span className="font-medium">Phương tiện:</span>{" "}
                          {getVehicleModel(journey.vehicle?.id)}
                        </p>
                        <p className="text-sm text-gray-600">
                          <span className="font-medium">Ngày:</span>{" "}
                          {formatDate(journey.journeyDate)}
                        </p>
                        <p className="text-sm text-gray-600 mt-1">
                          <span className="font-medium">Khoảng cách:</span>{" "}
                          {journey.distanceKm ? `${journey.distanceKm.toFixed(1)} km` : "—"}
                        </p>
                      </div>

                      <div className="flex flex-col items-end gap-2">
                        {journey.verificationStatus === 'PENDING_INSPECTION' && journey.appointmentStatus === 'REQUESTED' && (
                          <button
                            onClick={() => openScheduleModal(journey)}
                            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition font-medium"
                          >
                            <Calendar className="w-4 h-4" />
                            Đặt lịch kiểm tra
                          </button>
                        )}
                        
                        {(journey.verificationStatus === 'PENDING_VERIFICATION' || journey.verificationStatus === 'REJECTED') && (
                          <button
                            onClick={() => handleDelete(journey.id, journey.distanceKm?.toFixed(1))}
                            className="flex items-center gap-2 px-4 py-2 text-red-600 hover:bg-red-50 rounded-lg transition"
                          >
                            <Trash2 className="w-4 h-4" />
                            <span className="text-sm font-medium">Xóa</span>
                          </button>
                        )}
                        
                        {journey.verificationStatus === 'PENDING_INSPECTION' && journey.appointmentStatus === 'SCHEDULED' && (
                           <p className="text-sm text-purple-600 font-medium p-2">Đã đặt lịch kiểm tra</p>
                        )}
                        {(journey.verificationStatus === 'VERIFIED') && (
                           <p className="text-sm text-green-600 font-medium p-2">Hoàn tất</p>
                        )}
                      </div>
                    </div>

                    <div className="grid grid-cols-4 gap-4 pt-4 border-t border-gray-100">
                      <div>
                        <p className="text-xs text-gray-500 mb-1">Năng lượng đã sử dụng</p>
                        <p className="text-sm font-medium text-gray-800">
                          {journey.energyConsumedKwh ? `${journey.energyConsumedKwh.toFixed(2)} kWh` : "N/A"}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-gray-500 mb-1">CO₂ Đã lưu</p>
                        <p className="text-sm font-medium text-green-600">
                          {journey.co2Saved ? `${journey.co2Saved.toFixed(2)} kg` : "Pending"}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-gray-500 mb-1">Điểm đã nhận</p>
                        <p className="text-sm font-medium text-blue-600">
                          {journey.creditsEarned ? `${journey.creditsEarned.toFixed(2)} tCO₂` : "Pending"}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-gray-500 mb-1">Tuyến đường</p>
                        <p className="text-sm font-medium text-gray-800">
                          {journey.startLocation && journey.endLocation
                            ? `${journey.startLocation} → ${journey.endLocation}`
                            : "—"}
                        </p>
                      </div>
                    </div>

                    {journey.notes && (
                      <div className="mt-3 pt-3 border-t border-gray-100">
                        <p className="text-xs text-gray-500 mb-1">Ghi chú</p>
                        <p className="text-sm text-gray-700">{journey.notes}</p>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </main>
      </div>

      {/* --- Confirm Modal (replaces window.confirm) --- */}
      {showConfirm && (
        <div className="fixed inset-0 flex items-center justify-center bg-black/40 z-50">
          <div className="bg-white rounded-2xl shadow-lg p-6 w-96 text-center animate-fadeSlideIn">
            <h2 className="text-lg font-semibold mb-3 text-gray-800">Xác nhận</h2>
            <p className="text-gray-600 mb-5">{confirmMessage}</p>
            <div className="flex justify-center gap-3">
              <button
                onClick={confirmCancel}
                className="px-4 py-2 rounded-lg bg-gray-200 hover:bg-gray-300 transition"
              >
                Hủy
              </button>
              <button
                onClick={confirmExecute}
                className="px-4 py-2 rounded-lg bg-red-500 text-white hover:bg-red-600 transition"
              >
                Xác nhận
              </button>
            </div>
          </div>
        </div>
      )}

      {/* --- Message Modal (replaces window.alert) --- */}
      {showMessageModal && (
        <div className="fixed inset-0 flex items-center justify-center bg-black/40 z-50">
          <div className="bg-white rounded-2xl shadow-lg p-6 w-96 text-center animate-fadeSlideIn">
            <h2 className="text-lg font-semibold mb-3 text-gray-800">Thông báo</h2>
            <p className="text-gray-600 mb-5">{messageModalMessage}</p>
            <div className="flex justify-center">
              <button
                onClick={() => setShowMessageModal(false)}
                className="px-6 py-2 rounded-lg bg-gray-200 hover:bg-gray-300 transition"
              >
                OK
              </button>
            </div>
          </div>
        </div>
      )}

      {showScheduleModal && selectedJourney && (
        <ScheduleModal
          journey={selectedJourney}
          onClose={closeScheduleModal}
          onSuccess={handleScheduleSuccess}
        />
      )}
    </div>
  );
}

/* 💫 Animation */
const style = document.createElement("style");
style.innerHTML = `
@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}
.animate-fadeSlideIn {
  animation: fadeSlideIn 0.25s ease-out;
}
`;
document.head.appendChild(style);
