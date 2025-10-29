// src/pages/JourneyManagement.jsx
import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import Sidebar from "../components/Sidebar";
import Header from "../components/Header";
import { journeyService } from "../services/journeyService";
import { vehicleService } from "../services/vehicleService";
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
  XCircle
} from "lucide-react";

export default function JourneyManagement() {
  const navigate = useNavigate();
  const [journeys, setJourneys] = useState([]);
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [showAddForm, setShowAddForm] = useState(false);

  const [newJourney, setNewJourney] = useState({
    vehicleId: "",
    distanceKm: "",
    energyConsumedKwh: "",
    journeyDate: new Date().toISOString().split("T")[0],
    startLocation: "",
    endLocation: "",
    notes: "",
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [journeysRes, vehiclesRes] = await Promise.all([
        journeyService.getMyJourneys(),
        vehicleService.getMyVehicles(),
      ]);

      if (journeysRes.success) {
        const sortedJourneys = journeysRes.data.sort(
          (a, b) => new Date(b.journeyDate) - new Date(a.journeyDate)
        );
        setJourneys(sortedJourneys);
      }

      if (vehiclesRes.success) {
        setVehicles(vehiclesRes.data);
        if (vehiclesRes.data.length > 0) {
          setNewJourney((prev) => ({ ...prev, vehicleId: vehiclesRes.data[0].id }));
        }
      }
    } catch (err) {
      console.error("❌ Error loading data:", err);
      setError("Failed to load data. Please try again.");
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
        setJourneys([response.data, ...journeys]);
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

  const handleDelete = async (id, distance) => {
    if (!window.confirm(`Are you sure you want to delete this ${distance} km journey?`)) return;

    try {
      const response = await journeyService.deleteJourney(id);

      if (response.success) {
        setSuccess("🗑️ Journey deleted successfully!");
        setJourneys(journeys.filter((j) => j.id !== id));

        setTimeout(() => setSuccess(""), 3000);
      } else {
        setError(response.message || "Failed to delete journey");
      }
    } catch (err) {
      console.error("❌ Error deleting journey:", err);
      setError(err.response?.data?.message || "Failed to delete journey. Please try again.");
    }
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

  const getStatusBadge = (status) => {
    const statusConfig = {
      VERIFIED: { 
        icon: CheckCircle, 
        color: "bg-green-100 text-green-700 border-green-200", 
        label: "Verified" 
      },
      PENDING: { 
        icon: Clock, 
        color: "bg-yellow-100 text-yellow-700 border-yellow-200", 
        label: "Pending" 
      },
      REJECTED: { 
        icon: XCircle, 
        color: "bg-red-100 text-red-700 border-red-200", 
        label: "Rejected" 
      },
    };

    const config = statusConfig[status] || { 
      icon: Clock, 
      color: "bg-gray-100 text-gray-700 border-gray-200", 
      label: status 
    };
    
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
          {/* Back Button */}
          <button
            onClick={() => navigate("/home")}
            className="flex items-center gap-2 text-gray-600 hover:text-gray-800 mb-6 transition"
          >
            <ArrowLeft className="w-5 h-5" />
            <span>Back to Dashboard</span>
          </button>

          {/* Page Header */}
          <div className="flex justify-between items-center mb-8">
            <div>
              <h1 className="text-3xl font-bold text-gray-800 mb-2">
                Journey Management
              </h1>
              <p className="text-gray-600">
                Track your EV journeys and earn carbon credits
              </p>
            </div>

            {!showAddForm && (
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
                Add Journey
              </button>
            )}
          </div>

          {/* Alert Messages */}
          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-3">
              <span className="text-2xl">⚠️</span>
              <div>
                <p className="text-red-800 font-medium">Error</p>
                <p className="text-red-600 text-sm">{error}</p>
              </div>
            </div>
          )}

          {success && (
            <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg flex items-center gap-3">
              <span className="text-2xl">✅</span>
              <div>
                <p className="text-green-800 font-medium">Success!</p>
                <p className="text-green-600 text-sm">{success}</p>
              </div>
            </div>
          )}

          {/* No Vehicles Warning */}
          {vehicles.length === 0 && !loading && (
            <div className="mb-6 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
              <p className="text-yellow-800 font-medium mb-2">
                📌 No vehicles found
              </p>
              <p className="text-yellow-700 text-sm mb-3">
                You need to add a vehicle before recording journeys.
              </p>
              <button
                onClick={() => navigate("/vehicles")}
                className="text-sm bg-yellow-600 text-white px-4 py-2 rounded-lg hover:bg-yellow-700 transition"
              >
                Go to Vehicle Management →
              </button>
            </div>
          )}

          {/* Add Journey Form */}
          {showAddForm && (
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8 mb-8">
              <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-semibold text-gray-800">
                  Add New Journey
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
                {/* Vehicle Selection */}
                <div className="mb-6">
                  <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                    <Car className="w-4 h-4" />
                    Select Vehicle *
                  </label>
                  <select
                    name="vehicleId"
                    value={newJourney.vehicleId}
                    onChange={handleChange}
                    required
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
                  >
                    {vehicles.map((vehicle) => (
                      <option key={vehicle.id} value={vehicle.id}>
                        {vehicle.model} - {vehicle.vin}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Journey Details Grid */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
                  {/* Distance */}
                  <div>
                    <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                      <MapPin className="w-4 h-4" />
                      Distance (km) *
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

                  {/* Energy */}
                  <div>
                    <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                      <Zap className="w-4 h-4" />
                      Energy (kWh) *
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

                  {/* Date */}
                  <div>
                    <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                      <Calendar className="w-4 h-4" />
                      Date *
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

                {/* Locations */}
                <div className="grid grid-cols-2 gap-6 mb-6">
                  <div>
                    <label className="text-sm font-medium text-gray-700 mb-2 block">
                      Start Location (Optional)
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
                      End Location (Optional)
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

                {/* Notes */}
                <div className="mb-6">
                  <label className="text-sm font-medium text-gray-700 mb-2 block">
                    Notes (Optional)
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

                {/* Buttons */}
                <div className="flex gap-4">
                  <button
                    type="button"
                    onClick={() => {
                      setShowAddForm(false);
                      setError("");
                    }}
                    className="flex-1 px-6 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition font-medium"
                  >
                    Cancel
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
                    {submitting ? "Adding..." : "Add Journey"}
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* Journey List */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
            <h2 className="text-xl font-semibold text-gray-800 mb-6">
              Your Journeys ({journeys.length})
            </h2>

            {loading ? (
              <div className="text-center py-12">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto"></div>
                <p className="text-gray-500 text-sm mt-4">Loading journeys...</p>
              </div>
            ) : journeys.length === 0 ? (
              <div className="text-center py-12 border border-dashed border-gray-300 rounded-lg">
                <div className="mb-4">
                  <MapPin className="w-16 h-16 text-gray-400 mx-auto" />
                </div>
                <p className="text-gray-600 font-medium mb-2">
                  No journeys recorded yet
                </p>
                <p className="text-gray-500 text-sm mb-4">
                  Add your first EV journey to start earning carbon credits
                </p>
                {!showAddForm && vehicles.length > 0 && (
                  <button
                    onClick={() => setShowAddForm(true)}
                    className="inline-flex items-center gap-2 bg-green-600 text-white px-6 py-3 rounded-lg hover:bg-green-700 transition font-medium"
                  >
                    <Plus className="w-5 h-5" />
                    Add Your First Journey
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
                            {journey.distanceKm ? `${journey.distanceKm.toFixed(1)} km` : "N/A"}
                          </h3>
                          {getStatusBadge(journey.verificationStatus)}
                        </div>
                        <p className="text-sm text-gray-600 mb-1">
                          <span className="font-medium">Vehicle:</span>{" "}
                          {getVehicleModel(journey.vehicle?.id)}
                        </p>
                        <p className="text-sm text-gray-600">
                          <span className="font-medium">Date:</span>{" "}
                          {formatDate(journey.journeyDate)}
                        </p>
                      </div>

                      <button
                        onClick={() => handleDelete(journey.id, journey.distanceKm?.toFixed(1))}
                        className="flex items-center gap-2 px-4 py-2 text-red-600 hover:bg-red-50 rounded-lg transition"
                      >
                        <Trash2 className="w-4 h-4" />
                        <span className="text-sm font-medium">Delete</span>
                      </button>
                    </div>

                    <div className="grid grid-cols-4 gap-4 pt-4 border-t border-gray-100">
                      <div>
                        <p className="text-xs text-gray-500 mb-1">Energy Used</p>
                        <p className="text-sm font-medium text-gray-800">
                          {journey.energyConsumedKwh ? `${journey.energyConsumedKwh.toFixed(2)} kWh` : "N/A"}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-gray-500 mb-1">CO₂ Saved</p>
                        <p className="text-sm font-medium text-green-600">
                          {journey.co2Saved ? `${journey.co2Saved.toFixed(2)} kg` : "Pending"}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-gray-500 mb-1">Credits Earned</p>
                        <p className="text-sm font-medium text-blue-600">
                          {journey.creditsEarned ? `${journey.creditsEarned.toFixed(2)} tCO₂` : "Pending"}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-gray-500 mb-1">Route</p>
                        <p className="text-sm font-medium text-gray-800">
                          {journey.startLocation && journey.endLocation
                            ? `${journey.startLocation} → ${journey.endLocation}`
                            : "—"}
                        </p>
                      </div>
                    </div>

                    {journey.notes && (
                      <div className="mt-3 pt-3 border-t border-gray-100">
                        <p className="text-xs text-gray-500 mb-1">Notes</p>
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
    </div>
  );
}
