// src/pages/HomePage.jsx
import { useState, useEffect } from "react";
import Sidebar from "../components/Sidebar";
import Header from "../components/Header";
import StatCard from "../components/StatCard";
import { vehicleService } from "../services/vehicleService";
import { journeyService } from "../services/journeyService";
import { walletService } from "../services/walletService";
import { carbonCreditService } from "../services/carbonCreditService";
import { transactionService } from "../services/transactionService"

export default function HomePage() {
  const [vehicles, setVehicles] = useState([]);
  const [journeys, setJourneys] = useState([]);
  const [wallet, setWallet] = useState(null);
  const [credits, setCredits] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    setLoading(true);
    setError("");

    try {
      const token = localStorage.getItem("token");
      if (!token) {
        setError("No authentication token found. Please login again.");
        window.location.href = "/login";
        return;
      }

      console.log("📡 Fetching dashboard data...");

      // Fetch all data in parallel
      const [vehiclesRes, journeysRes, walletRes, creditsRes, transactionsRes] = await Promise.allSettled([
        vehicleService.getMyVehicles(),
        journeyService.getMyJourneys(),
        walletService.getMyWallet(),
        carbonCreditService.getMyCredits(),
        transactionService.getMyTransactions(0, 100),
      ]);

      // Handle Vehicles
      if (vehiclesRes.status === "fulfilled" && vehiclesRes.value?.success) {
        setVehicles(vehiclesRes.value.data);
        console.log("✅ Vehicles loaded:", vehiclesRes.value.data);
      }

      // Handle Journeys
      if (journeysRes.status === "fulfilled" && journeysRes.value?.success) {
        const sortedJourneys = journeysRes.value.data.sort(
          (a, b) => new Date(b.journeyDate) - new Date(a.journeyDate)
        );
        setJourneys(sortedJourneys);
        console.log("✅ Journeys loaded:", sortedJourneys);
      }


      // Handle Wallet - Better debugging
      if (walletRes.status === "fulfilled") {
        console.log("📦 Wallet Response:", walletRes.value);

        if (walletRes.value?.success) {
          setWallet(walletRes.value.data);
          console.log("✅ Wallet loaded:", walletRes.value.data);
        } else {
          console.warn("⚠️ Wallet response not successful:", walletRes.value);
          // Set default wallet if response exists but not success
          if (walletRes.value?.data) {
            setWallet(walletRes.value.data);
          } else {
            setWallet({ creditBalance: 0, cashBalance: 0 });
          }
        }
      } else {
        console.error("❌ Wallet request failed:", walletRes.reason);
        setWallet({ creditBalance: 0, cashBalance: 0 });
      }

      // Handle Credits
      if (creditsRes.status === "fulfilled" && creditsRes.value?.success) {
        const creditData = creditsRes.value.data.content || creditsRes.value.data;
        setCredits(Array.isArray(creditData) ? creditData : []);
        console.log("✅ Credits loaded:", creditData);
      }

      //handle transactions
      if (transactionsRes.status === "fulfilled" && transactionsRes.value?.success) {
        const transactionData = transactionsRes.value.data.content || transactionsRes.value.data;
        setTransactions(Array.isArray(transactionData) ? transactionData : []);
        console.log("✅Transactions loaded:", transactionData);
      }
    } catch (err) {
      console.error("❌ Error loading dashboard:", err);
      if (err.response?.status === 403 || err.response?.status === 401) {
        setError("Your session has expired. Please login again.");
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        setTimeout(() => {
          window.location.href = "/login";
        }, 2000);
      } else {
        setError(err.response?.data?.message || "Failed to load some dashboard data");
      }
    } finally {
      setLoading(false);
    }
  };

  const calculateRevenue = () => {
    console.log("💰 All Transactions:", transactions);

    if (!transactions || transactions.length === 0) return { total: 0, thisMonth: 0, count: 0 };

    const currentUser = JSON.parse(localStorage.getItem("user") || "{}");
    const currentUserId = currentUser.id;

    const now = new Date();
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);

    // ✅ Filter where YOU are the SELLER
    const sellTransactions = transactions.filter((t) => {
      const isSeller = t.seller?.id === currentUserId;
      const isCompleted = t.status === "COMPLETED";
      console.log("Transaction check:", {
        isSeller,
        isCompleted,
        sellerID: t.seller?.id,
        yourID: currentUserId,
        status: t.status
      });
      return isSeller && isCompleted;
    });

    console.log("💰 Your Sell Transactions:", sellTransactions);

    // ✅ Use totalPrice (not amount)
    const totalRevenue = sellTransactions.reduce((sum, t) => sum + (parseFloat(t.totalPrice) || 0), 0);

    const thisMonthRevenue = sellTransactions
      .filter((t) => new Date(t.createdAt) >= startOfMonth)
      .reduce((sum, t) => sum + (parseFloat(t.totalPrice) || 0), 0);

    console.log("💰 Revenue Calculated:", { totalRevenue, thisMonthRevenue, count: sellTransactions.length });

    return {
      total: totalRevenue.toFixed(2),
      thisMonth: thisMonthRevenue.toFixed(2),
      count: sellTransactions.length,
    };
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

  const calculateStats = () => {
    console.log("📊 Stats Debug - Credits:", credits);
    console.log("📊 Stats Debug - Journeys:", journeys);

    // ✅ Try multiple field names for credits
    const totalCredits = credits.reduce((sum, credit) => {
      const amount = credit.amount || credit.creditAmount || 0;
      return sum + amount;
    }, 0);

    // ✅ Use co2ReducedKg (from JourneyDataDTO), fallback to co2Saved
    const pendingCredits = journeys
      .filter((j) => j.verificationStatus === "PENDING")
      .reduce((sum, j) => sum + ((j.co2ReducedKg || 0) / 1000), 0);

    // ✅ Use co2ReducedKg instead of co2Saved
    const totalCO2 = journeys.reduce((sum, j) => sum + (j.co2ReducedKg || 0), 0);

    const verifiedJourneys = journeys.filter((j) => j.verificationStatus === "VERIFIED").length;

    console.log("📈 Calculated Stats:", { totalCredits, pendingCredits, totalCO2, verifiedJourneys });

    return {
      totalCredits: totalCredits.toFixed(2),
      pendingCredits: pendingCredits.toFixed(2),
      totalCO2: totalCO2.toFixed(2),
      verifiedJourneys,
    };
  };

  const stats = calculateStats();
  const revenue = calculateRevenue();

  const getStatusBadge = (status) => {
    const statusConfig = {
      VERIFIED: { color: "bg-green-100 text-green-700", label: "Verified" },
      PENDING: { color: "bg-yellow-100 text-yellow-700", label: "Pending" },
      REJECTED: { color: "bg-red-100 text-red-700", label: "Rejected" },
    };

    const config = statusConfig[status] || { color: "bg-gray-100 text-gray-700", label: status };
    return (
      <span className={`text-xs px-2 py-1 rounded-full ${config.color}`}>
        {config.label}
      </span>
    );
  };

  return (
    <div className="flex min-h-screen w-screen bg-[#F9FAFB] overflow-hidden">
      <Sidebar />

      <div className="flex flex-col flex-1 min-h-screen w-full">
        <Header />

        <main className="flex-1 p-8 w-full bg-[#F9FAFB] overflow-y-auto">
          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-3">
              <span className="text-2xl">⚠️</span>
              <div>
                <p className="text-red-800 font-medium">Error</p>
                <p className="text-red-600 text-sm">{error}</p>
              </div>
            </div>
          )}

          {/* Statistics Cards */}
          <div className="grid grid-cols-4 gap-6 mb-8 w-full">
            <StatCard
              title="Carbon Credit Balance"
              value={`${wallet?.creditBalance?.toFixed(2) || '0.00'} tCO₂`}  // ✅ Use wallet balance
              sub={`+${stats.pendingCredits} tCO₂ pending verification`}
            />
            <StatCard
              title="Verified Journeys"
              value={stats.verifiedJourneys}
              sub={`Out of ${journeys.length} total journeys`}
            />
            <StatCard
              title="Total CO₂ Saved"
              value={`${stats.totalCO2} kg`}
              sub="From all verified journeys"
            />
            <StatCard
              title="Total Revenue"
              value={`$${revenue.total} USD`}
              sub={`$${revenue.thisMonth} this month • ${revenue.count} sales`}
            />
          </div>

          {/* Main Grid */}
          <div className="grid grid-cols-2 gap-6 w-full">
            {/* Your Electric Vehicles */}
            <div className="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
              <div className="flex justify-between items-center mb-4">
                <div>
                  <h2 className="font-semibold text-lg">Your Electric Vehicles</h2>
                  <p className="text-gray-500 text-sm">Registered vehicles</p>
                </div>
                <button
                  onClick={() => (window.location.href = "/vehicles")}
                  className="text-sm text-green-600 hover:text-green-700 font-medium"
                >
                  Manage →
                </button>
              </div>

              {loading ? (
                <div className="text-center py-8">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-green-600 mx-auto"></div>
                  <p className="text-gray-500 text-sm mt-2">Loading vehicles...</p>
                </div>
              ) : vehicles.length > 0 ? (
                <div className="space-y-3">
                  {vehicles.map((vehicle) => (
                    <div
                      key={vehicle.id}
                      className="flex items-center gap-4 p-4 border border-gray-200 rounded-lg hover:shadow-md transition"
                    >
                      <div className="w-14 h-14 bg-green-100 rounded-lg flex items-center justify-center flex-shrink-0">
                        <span className="text-2xl">🚗</span>
                      </div>
                      <div className="flex-1">
                        <p className="font-semibold text-gray-800">{vehicle.model}</p>
                        <p className="text-sm text-gray-500">VIN: {vehicle.vin || "N/A"}</p>
                        <p className="text-xs text-gray-400 mt-1">
                          Registered: {formatDate(vehicle.registrationDate)}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-12 border border-dashed border-gray-300 rounded-lg">
                  <span className="text-5xl mb-3 block">🚗</span>
                  <p className="text-gray-500 text-sm mb-3">No vehicles registered</p>
                  <button
                    onClick={() => (window.location.href = "/vehicles")}
                    className="text-sm bg-green-600 text-white px-6 py-2 rounded-lg hover:bg-green-700 transition"
                  >
                    Add Your First Vehicle
                  </button>
                </div>
              )}
            </div>

            {/* All Current Journeys */}
            <div className="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
              <div className="flex justify-between items-center mb-4">
                <div>
                  <h2 className="font-semibold text-lg">All Journeys</h2>
                  <p className="text-gray-500 text-sm">
                    {journeys.length} total journey{journeys.length !== 1 ? "s" : ""}
                  </p>
                </div>
              </div>

              {loading ? (
                <div className="text-center py-8">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-green-600 mx-auto"></div>
                  <p className="text-gray-500 text-sm mt-2">Loading journeys...</p>
                </div>
              ) : journeys.length > 0 ? (
                <div className="space-y-3 max-h-[400px] overflow-y-auto">
                  {journeys.map((journey) => (
                    <div
                      key={journey.id}
                      className="p-4 border border-gray-200 rounded-lg hover:shadow-md transition"
                    >
                      <div className="flex justify-between items-start mb-2">
                        <div>
                          <p className="font-semibold text-gray-800">
                            {journey.distanceKm ? `${journey.distanceKm.toFixed(1)} km` : "N/A"}
                          </p>
                          <p className="text-sm text-gray-500">{formatDate(journey.journeyDate)}</p>
                        </div>
                        {getStatusBadge(journey.verificationStatus)}
                      </div>

                      <div className="grid grid-cols-2 gap-2 mt-3 pt-3 border-t border-gray-100">
                        <div>
                          <p className="text-xs text-gray-500">CO₂ Saved</p>
                          <p className="text-sm font-medium text-green-600">
                            {journey.co2ReducedKg ? `${journey.co2ReducedKg.toFixed(2)} kg` : "Pending"}
                          </p>
                        </div>
                        <div>
                          <p className="text-xs text-gray-500">Credits Earned</p>
                          <p className="text-sm font-medium text-blue-600">
                            {journey.co2ReducedKg ? `${(journey.co2ReducedKg / 1000).toFixed(4)} tCO₂` : "Pending"}
                          </p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-12 border border-dashed border-gray-300 rounded-lg">
                  <span className="text-5xl mb-3 block">🗺️</span>
                  <p className="text-gray-500 text-sm mb-3">No journeys recorded yet</p>
                </div>
              )}
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
