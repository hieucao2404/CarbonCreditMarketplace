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
  Send,
  Edit2,
} from "lucide-react";

/*
  NOTE:
  - Giữ nguyên service calls.
  - Sửa modal để max chiều cao và scroll nội dung nếu cần.
  - Hiển thị Tuyến đường theo format: [start]-[end] (khi cả hai có).
*/

// ---------- ScheduleModal (unchanged) ----------
function ScheduleModal({ journey, onClose, onSuccess }) {
  const [stations, setStations] = useState([]);
  const [loadingStations, setLoadingStations] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [selectedStationId, setSelectedStationId] = useState("");
  const [selectedTime, setSelectedTime] = useState("");

  useEffect(() => {
    verificationService
      .getActiveStations()
      .then((res) => {
        if (res.data?.success && Array.isArray(res.data.data)) {
          setStations(res.data.data);
          if (res.data.data.length > 0) {
            setSelectedStationId(res.data.data[0].id);
          }
        } else {
          setStations([]);
          setError("Could not load verification stations.");
        }
      })
      .catch((err) => {
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
      const response = await verificationService.scheduleAppointment(appointmentId, selectedStationId, selectedTime);
      if (response.data?.success) {
        onSuccess("Appointment scheduled successfully!");
      } else {
        setError(response.data?.message || "Failed to schedule appointment.");
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
      <div className="bg-white rounded-xl shadow-2xl p-6 max-w-lg w-full">
        <h2 className="text-xl font-semibold text-gray-800 mb-2">Đặt lịch kiểm tra</h2>
        <p className="text-sm text-gray-600 mb-4">
          Một CVA đã yêu cầu kiểm tra thực tế cho hành trình của bạn (<span className="font-medium">{Number(journey.distanceKm || 0).toFixed(1)} km</span>).
        </p>

        {error && <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded-lg text-sm">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label className="text-sm font-medium text-gray-700 mb-2 block">1. Chọn Trạm Xác Minh</label>
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
                <option value="" disabled>
                  Chọn trạm...
                </option>
                {stations.map((station) => (
                  <option key={station.id} value={station.id}>
                    {station.name} {station.address ? `- ${station.address}` : ""}
                  </option>
                ))}
              </select>
            )}
          </div>

          <div className="mb-4">
            <label className="text-sm font-medium text-gray-700 mb-2 block">2. Chọn ngày và giờ</label>
            <input
              type="datetime-local"
              value={selectedTime}
              onChange={(e) => setSelectedTime(e.target.value)}
              required
              min={new Date().toISOString().slice(0, 16)}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500"
            />
          </div>

          <div className="flex gap-3">
            <button type="button" onClick={onClose} disabled={submitting} className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700">
              Cancel
            </button>
            <button type="submit" disabled={submitting || loadingStations || !selectedStationId || !selectedTime} className="flex-1 px-4 py-2 rounded-lg bg-green-600 text-white">
              {submitting ? "Booking..." : "Book Appointment"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
// ---------- END ScheduleModal ----------

// ---------- EditJourneyModal (improved UI + scroll-safe) ----------
function EditJourneyModal({ journey, vehicles, onClose, onSuccess }) {
  // initialize form using journey values (with safe fallbacks)
  const initial = {
    vehicleId: journey?.vehicle?.id || journey?.vehicleId || journey?.vehicle || "",
    distanceKm: journey?.distanceKm != null ? String(journey.distanceKm) : "",
    energyConsumedKwh: journey?.energyConsumedKwh != null ? String(journey.energyConsumedKwh) : "",
    journeyDate: (journey?.journeyDate || journey?.date || "").split?.("T")?.[0] || new Date().toISOString().split("T")[0],
    startLocation: journey?.startLocation || journey?.origin || journey?.startAddress || "",
    endLocation: journey?.endLocation || journey?.destination || journey?.endAddress || "",
    notes: journey?.notes || "",
  };
  const [form, setForm] = useState(initial);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // sync when journey changes
  useEffect(() => {
    setForm({
      vehicleId: journey?.vehicle?.id || journey?.vehicleId || journey?.vehicle || "",
      distanceKm: journey?.distanceKm != null ? String(journey.distanceKm) : "",
      energyConsumedKwh: journey?.energyConsumedKwh != null ? String(journey.energyConsumedKwh) : "",
      journeyDate: (journey?.journeyDate || journey?.date || "").split?.("T")?.[0] || new Date().toISOString().split("T")[0],
      startLocation: journey?.startLocation || journey?.origin || journey?.startAddress || "",
      endLocation: journey?.endLocation || journey?.destination || journey?.endAddress || "",
      notes: journey?.notes || "",
    });
    setError("");
  }, [journey]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((p) => ({ ...p, [name]: value }));
    if (error) setError("");
  };

  const validate = () => {
    if (!form.vehicleId) { setError("Please select a vehicle"); return false; }
    if (!form.distanceKm || parseFloat(form.distanceKm) <= 0) { setError("Distance must be greater than 0"); return false; }
    if (!form.energyConsumedKwh || parseFloat(form.energyConsumedKwh) <= 0) { setError("Energy consumed must be greater than 0"); return false; }
    if (!form.journeyDate) { setError("Please select a journey date"); return false; }
    const selectedDate = new Date(form.journeyDate);
    const today = new Date(); today.setHours(23,59,59,999);
    if (selectedDate > today) { setError("Journey date cannot be in the future"); return false; }
    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;
    setSubmitting(true);
    try {
      const journeyData = {
        vehicle: { id: form.vehicleId },
        distanceKm: parseFloat(form.distanceKm),
        energyConsumedKwh: parseFloat(form.energyConsumedKwh),
        journeyDate: form.journeyDate,
        startLocation: form.startLocation || null,
        endLocation: form.endLocation || null,
        notes: form.notes || null,
      };
      const response = await journeyService.updateJourney(journey.id, journeyData);
      if (response.success) {
        onSuccess("✏️ Journey updated successfully!");
      } else {
        setError(response.message || "Failed to update journey");
      }
    } catch (err) {
      console.error("Error updating journey:", err);
      setError(err.response?.data?.message || "Failed to update journey. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  // helper label
  const findVehicleLabel = (idOrObj) => {
    if (!vehicles || vehicles.length === 0) return "Unknown Vehicle";
    const id = typeof idOrObj === "object" ? (idOrObj?.id || idOrObj?.vehicleId || "") : idOrObj;
    const found = vehicles.find((v) => v.id === id || v.vehicleId === id || String(v.id) === String(id));
    if (found) return `${found.model || found.name || "Vehicle"}${found.vin ? ` - ${found.vin}` : ""}`;
    if (typeof idOrObj === "string" && idOrObj.length > 3) return idOrObj;
    return "Unknown Vehicle";
  };

  return (
    // center modal and allow vertical scrolling inside modal box
    <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-y-auto">
        <div className="p-6 grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* LEFT: form (2/3) */}
          <div className="md:col-span-2">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg font-semibold">Sửa hành trình</h2>
              <button onClick={onClose} className="text-gray-500 hover:text-gray-700">✕</button>
            </div>

            {error && <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded-lg text-sm">{error}</div>}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                  <Car className="w-4 h-4" /> Chọn xe <span className="text-red-600">*</span>
                </label>
                <select name="vehicleId" value={form.vehicleId} onChange={handleChange} required className="w-full px-4 py-3 border border-gray-300 rounded-lg">
                  <option value="" disabled>Chọn xe...</option>
                  {vehicles.map((v) => (
                    <option key={v.id || v.vehicleId} value={v.id || v.vehicleId}>
                      {v.model || v.name} {v.vin ? `- ${v.vin}` : ""}
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="text-sm font-medium text-gray-700 mb-2 flex items-center gap-2">
                    <MapPin className="w-4 h-4" /> Khoảng cách (km) <span className="text-red-600">*</span>
                  </label>
                  <input type="number" name="distanceKm" value={form.distanceKm} onChange={handleChange} step="0.1" min="0.1" required className="w-full px-4 py-3 border border-gray-300 rounded-lg" />
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-700 mb-2 flex items-center gap-2">
                    <Zap className="w-4 h-4" /> Năng lượng (kWh) <span className="text-red-600">*</span>
                  </label>
                  <input type="number" name="energyConsumedKwh" value={form.energyConsumedKwh} onChange={handleChange} step="0.01" min="0.01" required className="w-full px-4 py-3 border border-gray-300 rounded-lg" />
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-700 mb-2 flex items-center gap-2">
                    <Calendar className="w-4 h-4" /> Ngày <span className="text-red-600">*</span>
                  </label>
                  <input type="date" name="journeyDate" value={form.journeyDate} onChange={handleChange} max={new Date().toISOString().split("T")[0]} required className="w-full px-4 py-3 border border-gray-300 rounded-lg" />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-gray-700 mb-2">Điểm xuất phát (Tùy chọn)</label>
                  <input type="text" name="startLocation" value={form.startLocation} onChange={handleChange} className="w-full px-4 py-3 border border-gray-300 rounded-lg" placeholder="e.g., Hà Nội" />
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-700 mb-2">Điểm đến (Tùy chọn)</label>
                  <input type="text" name="endLocation" value={form.endLocation} onChange={handleChange} className="w-full px-4 py-3 border border-gray-300 rounded-lg" placeholder="e.g., Hải Phòng" />
                </div>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-700 mb-2">Ghi chú (Tùy chọn)</label>
                <textarea name="notes" value={form.notes} onChange={handleChange} rows="3" className="w-full px-4 py-3 border border-gray-300 rounded-lg resize-none" placeholder="Thông tin thêm..."></textarea>
              </div>

              <div className="flex gap-3">
                <button type="button" onClick={onClose} className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700">Hủy</button>
                <button type="submit" disabled={submitting} className={`flex-1 px-4 py-2 rounded-lg text-white ${submitting ? "bg-gray-400" : "bg-green-600 hover:bg-green-700"}`}>{submitting ? "Đang lưu..." : "Lưu thay đổi"}</button>
              </div>
            </form>
          </div>

          {/* RIGHT: preview card (1/3) - removed sticky to avoid pushing modal */}
          <div className="md:col-span-1 md:pl-4">
            <div className="bg-gray-50 rounded-lg p-4">
              <h3 className="text-sm text-gray-600 mb-2">Xem trước</h3>
              <div className="bg-white rounded-md p-3 border border-gray-100">
                <div className="flex items-center justify-between mb-2">
                  <div>
                    <div className="text-sm font-medium">{findVehicleLabel(form.vehicleId)}</div>
                    <div className="text-xs text-gray-500">{journey?.vin || ""}</div>
                  </div>
                  <div className="text-xs text-gray-500">{/* status optional */}</div>
                </div>

                <div className="text-xs text-gray-500 mt-2">
                  <div className="mb-1"><span className="font-medium text-gray-700">Ngày:</span> {form.journeyDate || "N/A"}</div>
                  <div className="mb-1"><span className="font-medium text-gray-700">Khoảng cách:</span> {form.distanceKm ? `${Number(form.distanceKm).toFixed(1)} km` : "—"}</div>
                  <div className="mb-1"><span className="font-medium text-gray-700">Năng lượng:</span> {form.energyConsumedKwh ? `${Number(form.energyConsumedKwh).toFixed(2)} kWh` : "—"}</div>
                  <div className="mb-1"><span className="font-medium text-gray-700">Tuyến đường:</span> {form.startLocation && form.endLocation ? `[${form.startLocation}]-[${form.endLocation}]` : (form.startLocation || form.endLocation || "—")}</div>
                </div>

                <div className="mt-3 text-xs text-gray-500">
                  <div className="font-medium text-gray-700">Ghi chú</div>
                  <div className="mt-1 text-sm text-gray-600">{form.notes || "—"}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div> {/* end modal box */}
    </div>
  );
}
// ---------- END EditJourneyModal ----------

// ---------- Main Component ----------
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

  // confirm/message modals
  const [showConfirm, setShowConfirm] = useState(false);
  const [confirmMessage, setConfirmMessage] = useState("");
  const confirmActionRef = useRef(null);

  const [showMessageModal, setShowMessageModal] = useState(false);
  const [messageModalMessage, setMessageModalMessage] = useState("");

  // edit modal state
  const [showEditModal, setShowEditModal] = useState(false);
  const [editJourney, setEditJourney] = useState(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [journeysRes, vehiclesRes] = await Promise.all([journeyService.getMyJourneys(), vehicleService.getMyVehicles()]);

      if (journeysRes?.success && Array.isArray(journeysRes.data)) {
        const sorted = journeysRes.data.sort((a, b) => new Date(b.journeyDate || b.date || 0) - new Date(a.journeyDate || a.date || 0));
        setJourneys(sorted);
      } else {
        setJourneys([]);
      }

      if (vehiclesRes?.success && Array.isArray(vehiclesRes.data)) {
        setVehicles(vehiclesRes.data);
        if (vehiclesRes.data.length > 0) {
          setNewJourney((prev) => ({ ...prev, vehicleId: vehiclesRes.data[0].id || vehiclesRes.data[0].vehicleId }));
        }
      } else {
        setVehicles([]);
      }
    } catch (err) {
      console.error("Error loading data:", err);
      setError("Failed to load data. Please try again.");
      setJourneys([]);
      setVehicles([]);
    } finally {
      setLoading(false);
    }
  };

  // ---------- Helpers ----------
  const resolveVehicleLabel = (journey) => {
    const v = journey?.vehicle;
    const vid = v?.id || journey?.vehicleId || journey?.vehicle || v?.vehicleId;
    if (!vehicles || vehicles.length === 0) {
      const modelFromJourney = v?.model || v?.name;
      if (modelFromJourney) return modelFromJourney;
      if (typeof vid === "string" && vid.length > 0) return vid;
      return "Unknown Vehicle";
    }
    const found = vehicles.find((veh) => veh.id === vid || veh.vehicleId === vid || String(veh.id) === String(vid));
    if (found) return found.model || found.name || (found.vin ? `${found.vin}` : "Vehicle");
    return v?.model || v?.name || "Unknown Vehicle";
  };

  const resolveJourneyDate = (journey) => {
    const d = journey?.journeyDate || journey?.date || journey?.createdAt || null;
    if (!d) return "N/A";
    try {
      const dt = new Date(d);
      if (Number.isNaN(dt.getTime())) return "N/A";
      return dt.toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric" });
    } catch {
      return "N/A";
    }
  };

  // <-- UPDATED: return format [start]-[end] when both present -->
  const resolveRoute = (journey) => {
    const start = journey?.startLocation || journey?.origin || journey?.startAddress || journey?.from;
    const end = journey?.endLocation || journey?.destination || journey?.endAddress || journey?.to;
    if (start && end) return `[${start}]-[${end}]`;
    if (start) return `[${start}]`;
    if (end) return `[${end}]`;
    return "—";
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setNewJourney((prev) => ({ ...prev, [name]: value }));
    if (error) setError("");
  };

  const validateForm = () => {
    if (!newJourney.vehicleId) { setError("Please select a vehicle"); return false; }
    if (!newJourney.distanceKm || parseFloat(newJourney.distanceKm) <= 0) { setError("Distance must be greater than 0"); return false; }
    if (!newJourney.energyConsumedKwh || parseFloat(newJourney.energyConsumedKwh) <= 0) { setError("Energy consumed must be greater than 0"); return false; }
    if (!newJourney.journeyDate) { setError("Please select a journey date"); return false; }
    const selectedDate = new Date(newJourney.journeyDate);
    const today = new Date(); today.setHours(23,59,59,999);
    if (selectedDate > today) { setError("Journey date cannot be in the future"); return false; }
    return true;
  };

  const handleAddJourney = async (e) => {
    e.preventDefault();
    setError(""); setSuccess("");
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
        setJourneys((prev) => [response.data, ...prev]);
        setNewJourney({
          vehicleId: vehicles.length > 0 ? vehicles[0].id || vehicles[0].vehicleId : "",
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
      console.error("Error adding journey:", err);
      setError(err.response?.data?.message || "Failed to add journey. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  // delete
  const handleDelete = (id, distance) => {
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
        console.error("Error deleting journey:", err);
        setError(err.response?.data?.message || "Failed to delete journey. Please try again.");
      }
    };
    setShowConfirm(true);
  };

  const confirmExecute = async () => {
    setShowConfirm(false);
    const action = confirmActionRef.current;
    if (typeof action === "function") await action();
    confirmActionRef.current = null;
  };
  const confirmCancel = () => { setShowConfirm(false); confirmActionRef.current = null; };

  // schedule
  const openScheduleModal = (journey) => {
    if (!journey.appointmentId) {
      setMessageModalMessage("Error: Cannot find appointment ID for this journey. Please contact support.");
      setShowMessageModal(true);
      return;
    }
    setSelectedJourney(journey);
    setShowScheduleModal(true);
  };
  const closeScheduleModal = () => { setSelectedJourney(null); setShowScheduleModal(false); };
  const handleScheduleSuccess = (msg) => { setSuccess(msg); closeScheduleModal(); loadData(); setTimeout(() => setSuccess(""), 3000); };

  // edit handlers
  const openEditModal = (jour) => { setEditJourney({ ...(jour || {}) }); setShowEditModal(true); };
  const closeEditModal = () => { setEditJourney(null); setShowEditModal(false); };
  const handleEditSuccess = (message) => { setSuccess(message); closeEditModal(); loadData(); setTimeout(() => setSuccess(""), 3000); };

  // status badge
  const getStatusBadge = (journey) => {
    const journeyStatus = journey.verificationStatus;
    const apptStatus = journey.appointmentStatus;
    let config;
    const statusConfig = {
      VERIFIED: { icon: CheckCircle, color: "bg-green-100 text-green-700 border-green-200", label: "Verified" },
      PENDING_VERIFICATION: { icon: Clock, color: "bg-yellow-100 text-yellow-700 border-yellow-200", label: "Pending CVA" },
      PENDING_INSPECTION: { icon: Send, color: "bg-blue-100 text-blue-700 border-blue-200", label: "Inspection Required" },
      SCHEDULED: { icon: Calendar, color: "bg-purple-100 text-purple-700 border-purple-200", label: "Inspection Scheduled" },
      REJECTED: { icon: XCircle, color: "bg-red-100 text-red-700 border-red-200", label: "Rejected" },
    };
    if (journeyStatus === "PENDING_INSPECTION") {
      config = apptStatus === "SCHEDULED" ? statusConfig.SCHEDULED : statusConfig.PENDING_INSPECTION;
    } else {
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

  return (
    <div className="flex min-h-screen w-screen bg-[#F9FAFB]">
      <Sidebar />
      <div className="flex flex-col flex-1">
        <Header />
        <main className="flex-1 p-8">
          <button onClick={() => navigate("/home")} className="flex items-center gap-2 text-gray-600 hover:text-gray-800 mb-6 transition">
            <ArrowLeft className="w-5 h-5" />
            <span>Quay lại Bảng điều khiển</span>
          </button>

          <div className="flex justify-between items-center mb-8">
            <div>
              <h1 className="text-3xl font-bold text-gray-800 mb-2">Quản lý hành trình</h1>
              <p className="text-gray-600">Theo dõi các hành trình EV của bạn và kiếm tín chỉ carbon</p>
            </div>

            {!showAddForm && !loading && (
              <button onClick={() => setShowAddForm(true)} disabled={vehicles.length === 0}
                className={`flex items-center gap-2 px-6 py-3 rounded-lg font-medium shadow-sm transition ${vehicles.length === 0 ? "bg-gray-400 cursor-not-allowed text-white" : "bg-green-600 text-white hover:bg-green-700"}`}>
                <Plus className="w-5 h-5" /> Thêm hành trình
              </button>
            )}
          </div>

          {/* Alerts */}
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

          {/* No vehicles */}
          {vehicles.length === 0 && !loading && (
            <div className="mb-6 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
              <p className="text-yellow-800 font-medium mb-2">📌 Không tìm thấy xe nào</p>
              <p className="text-yellow-700 text-sm mb-3">Bạn cần thêm xe trước khi ghi lại các hành trình.</p>
              <button onClick={() => navigate("/vehicles")} className="text-sm bg-yellow-600 text-white px-4 py-2 rounded-lg hover:bg-yellow-700 transition">Đi đến Quản lý Xe →</button>
            </div>
          )}

          {/* Add form (unchanged) */}
          {showAddForm && (
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8 mb-8">
              <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-semibold text-gray-800">Thêm hành trình mới</h2>
                <button onClick={() => { setShowAddForm(false); setError(""); }} className="text-gray-500 hover:text-gray-700">✕</button>
              </div>
              <form onSubmit={handleAddJourney}>
                <div className="mb-6">
                  <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                    <Car className="w-4 h-4" /> Chọn xe <span className="text-red-600">*</span>
                  </label>
                  <select name="vehicleId" value={newJourney.vehicleId} onChange={handleChange} required className="w-full px-4 py-3 border border-gray-300 rounded-lg">
                    {vehicles.map((vehicle) => (<option key={vehicle.id || vehicle.vehicleId} value={vehicle.id || vehicle.vehicleId}>{vehicle.model || vehicle.name} {vehicle.vin ? `- ${vehicle.vin}` : ""}</option>))}
                  </select>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
                  <div>
                    <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                      <MapPin className="w-4 h-4" /> Khoảng cách (km) <span className="text-red-600">*</span>
                    </label>
                    <input type="number" name="distanceKm" value={newJourney.distanceKm} onChange={handleChange} step="0.1" min="0.1" required placeholder="45.2" className="w-full px-4 py-3 border border-gray-300 rounded-lg" />
                  </div>

                  <div>
                    <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                      <Zap className="w-4 h-4" /> Năng lượng tiêu thụ (kWh) <span className="text-red-600">*</span>
                    </label>
                    <input type="number" name="energyConsumedKwh" value={newJourney.energyConsumedKwh} onChange={handleChange} step="0.01" min="0.01" required placeholder="12.5" className="w-full px-4 py-3 border border-gray-300 rounded-lg" />
                  </div>

                  <div>
                    <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                      <Calendar className="w-4 h-4" /> Ngày <span className="text-red-600">*</span>
                    </label>
                    <input type="date" name="journeyDate" value={newJourney.journeyDate} onChange={handleChange} max={new Date().toISOString().split("T")[0]} required className="w-full px-4 py-3 border border-gray-300 rounded-lg" />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-6 mb-6">
                  <div>
                    <label className="text-sm font-medium text-gray-700 mb-2 block">Điểm xuất phát (Tùy chọn)</label>
                    <input type="text" name="startLocation" value={newJourney.startLocation} onChange={handleChange} placeholder="e.g., Hanoi" className="w-full px-4 py-3 border border-gray-300 rounded-lg" />
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-700 mb-2 block">Điểm đến (Tùy chọn)</label>
                    <input type="text" name="endLocation" value={newJourney.endLocation} onChange={handleChange} placeholder="e.g., Hai Phong" className="w-full px-4 py-3 border border-gray-300 rounded-lg" />
                  </div>
                </div>

                <div className="mb-6">
                  <label className="text-sm font-medium text-gray-700 mb-2 block">Ghi chú (Tùy chọn)</label>
                  <textarea name="notes" value={newJourney.notes} onChange={handleChange} rows="2" placeholder="Additional information..." className="w-full px-4 py-3 border border-gray-300 rounded-lg resize-none" />
                </div>

                <div className="flex gap-4">
                  <button type="button" onClick={() => { setShowAddForm(false); setError(""); }} className="flex-1 px-6 py-3 border border-gray-300 text-gray-700 rounded-lg">Hủy bỏ</button>
                  <button type="submit" disabled={submitting} className={`flex-1 px-6 py-3 rounded-lg font-medium ${submitting ? "bg-gray-400 cursor-not-allowed text-white" : "bg-green-600 text-white hover:bg-green-700"}`}>{submitting ? "Đang thêm..." : "Thêm hành trình"}</button>
                </div>
              </form>
            </div>
          )}

          {/* Journeys list */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
            <h2 className="text-xl font-semibold text-gray-800 mb-6">Các hành trình của bạn ({journeys.length})</h2>

            {loading ? (
              <div className="text-center py-12">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto" />
                <p className="text-gray-500 text-sm mt-4">Đang tải các hành trình...</p>
              </div>
            ) : journeys.length === 0 ? (
              <div className="text-center py-12 border border-dashed border-gray-300 rounded-lg">
                <div className="mb-4"><MapPin className="w-16 h-16 text-gray-400 mx-auto" /></div>
                <p className="text-gray-600 font-medium mb-2">Chưa có hành trình nào được ghi lại</p>
                <p className="text-gray-500 text-sm mb-4">Thêm hành trình EV đầu tiên của bạn để bắt đầu nhận tín chỉ carbon</p>
                {!showAddForm && vehicles.length > 0 && (
                  <button onClick={() => setShowAddForm(true)} className="inline-flex items-center gap-2 bg-green-600 text-white px-6 py-3 rounded-lg hover:bg-green-700 transition font-medium">
                    <Plus className="w-5 h-5" /> Thêm hành trình đầu tiên
                  </button>
                )}
              </div>
            ) : (
              <div className="space-y-4">
                {journeys.map((journey) => (
                  <div key={journey.id} className="p-6 border border-gray-200 rounded-lg hover:shadow-md transition">
                    <div className="flex justify-between items-start mb-4">
                      <div className="flex-1">
                        <div className="flex items-center gap-3 mb-2">
                          <h3 className="text-lg font-semibold text-gray-800">{resolveVehicleLabel(journey)}</h3>
                          {getStatusBadge(journey)}
                        </div>

                        <p className="text-sm text-gray-600"><span className="font-medium">Ngày:</span> {resolveJourneyDate(journey)}</p>
                        <p className="text-sm text-gray-600 mt-1"><span className="font-medium">Khoảng cách:</span> {journey.distanceKm != null ? `${Number(journey.distanceKm).toFixed(1)} km` : "—"}</p>
                      </div>

                      <div className="flex flex-col items-end gap-2">
                        {journey.verificationStatus === "PENDING_INSPECTION" && journey.appointmentStatus === "REQUESTED" && (
                          <button onClick={() => openScheduleModal(journey)} className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition font-medium"><Calendar className="w-4 h-4" /> Đặt lịch kiểm tra</button>
                        )}

                        {(journey.verificationStatus === "PENDING_VERIFICATION" || journey.verificationStatus === "REJECTED") && (
                          <div className="flex items-center gap-2">
                            <button onClick={() => { setEditJourney(journey); setShowEditModal(true); }} className="flex items-center gap-2 px-4 py-2 text-blue-600 hover:bg-blue-50 rounded-lg transition"><Edit2 className="w-4 h-4" /> <span className="text-sm font-medium">Sửa</span></button>
                            <button onClick={() => handleDelete(journey.id, journey.distanceKm?.toFixed(1))} className="flex items-center gap-2 px-4 py-2 text-red-600 hover:bg-red-50 rounded-lg transition"><Trash2 className="w-4 h-4" /> <span className="text-sm font-medium">Xóa</span></button>
                          </div>
                        )}

                        {journey.verificationStatus === "PENDING_INSPECTION" && journey.appointmentStatus === "SCHEDULED" && <p className="text-sm text-purple-600 font-medium p-2">Đã đặt lịch kiểm tra</p>}
                        {journey.verificationStatus === "VERIFIED" && <p className="text-sm text-green-600 font-medium p-2">Hoàn tất</p>}
                      </div>
                    </div>

                    <div className="grid grid-cols-4 gap-4 pt-4 border-t border-gray-100">
                      <div>
                        <p className="text-xs text-gray-500 mb-1">Năng lượng đã sử dụng</p>
                        <p className="text-sm font-medium text-gray-800">{journey.energyConsumedKwh != null ? `${Number(journey.energyConsumedKwh).toFixed(2)} kWh` : "N/A"}</p>
                      </div>

                      <div>
                        <p className="text-xs text-gray-500 mb-1">CO₂ Đã lưu</p>
                        <p className="text-sm font-medium text-green-600">{journey.co2Saved != null ? `${Number(journey.co2Saved).toFixed(2)} kg` : "Pending"}</p>
                      </div>

                      <div>
                        <p className="text-xs text-gray-500 mb-1">Điểm đã nhận</p>
                        <p className="text-sm font-medium text-blue-600">{journey.creditsEarned != null ? `${Number(journey.creditsEarned).toFixed(2)} tCO₂` : "Pending"}</p>
                      </div>

                      <div>
                        <p className="text-xs text-gray-500 mb-1">Tuyến đường</p>
                        <p className="text-sm font-medium text-gray-800">{resolveRoute(journey)}</p>
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

      {/* Confirm Modal */}
      {showConfirm && (
        <div className="fixed inset-0 flex items-center justify-center bg-black/40 z-50">
          <div className="bg-white rounded-2xl shadow-lg p-6 w-96 text-center animate-fadeSlideIn">
            <h2 className="text-lg font-semibold mb-3 text-gray-800">Xác nhận</h2>
            <p className="text-gray-600 mb-5">{confirmMessage}</p>
            <div className="flex justify-center gap-3">
              <button onClick={confirmCancel} className="px-4 py-2 rounded-lg bg-gray-200 hover:bg-gray-300 transition">Hủy</button>
              <button onClick={confirmExecute} className="px-4 py-2 rounded-lg bg-red-500 text-white hover:bg-red-600 transition">Xác nhận</button>
            </div>
          </div>
        </div>
      )}

      {/* Message Modal */}
      {showMessageModal && (
        <div className="fixed inset-0 flex items-center justify-center bg-black/40 z-50">
          <div className="bg-white rounded-2xl shadow-lg p-6 w-96 text-center animate-fadeSlideIn">
            <h2 className="text-lg font-semibold mb-3 text-gray-800">Thông báo</h2>
            <p className="text-gray-600 mb-5">{messageModalMessage}</p>
            <div className="flex justify-center">
              <button onClick={() => setShowMessageModal(false)} className="px-6 py-2 rounded-lg bg-gray-200 hover:bg-gray-300 transition">OK</button>
            </div>
          </div>
        </div>
      )}

      {showScheduleModal && selectedJourney && <ScheduleModal journey={selectedJourney} onClose={closeScheduleModal} onSuccess={handleScheduleSuccess} />}

      {showEditModal && editJourney && <EditJourneyModal journey={editJourney} vehicles={vehicles} onClose={closeEditModal} onSuccess={handleEditSuccess} />}
    </div>
  );
}

/* animation style */
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
