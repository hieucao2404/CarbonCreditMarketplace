import React, { useState, useEffect } from "react";
import Sidebar from "../components/Sidebar";
import Header from "../components/Header";
import { Leaf, DollarSign, BarChart3, RefreshCw } from "lucide-react";
import { journeyService } from "../services/journeyService";
import { carbonCreditService } from "../services/carbonCreditService";
import { transactionService } from "../services/transactionService";

export default function Report() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [stats, setStats] = useState({
    totalCO2Saved: 0,
    monthCO2: 0,
    weekCO2: 0,
    totalRevenue: 0,
    monthRevenue: 0,
    weekRevenue: 0,
    totalDistance: 0,
    creditsCreated: 0,
    creditsSold: 0,
    completedTransactions: 0,
  });

  useEffect(() => {
    loadReportData();
  }, []);

  const loadReportData = async () => {
    setLoading(true);
    setError("");

    try {
      const user = JSON.parse(localStorage.getItem("user"));
      const currentUserId = user?.id;

      // Load all data in parallel
      const [journeysRes, creditsRes, transactionsRes] = await Promise.allSettled([
        journeyService.getMyJourneys(0, 1000),
        carbonCreditService.getMyCredits(0, 1000),
        transactionService.getMyTransactions(0, 100),
      ]);

      // Extract data arrays
      const journeys = journeysRes.status === "fulfilled"
        ? (journeysRes.value?.data?.content || journeysRes.value?.data || [])
        : [];

      const credits = creditsRes.status === "fulfilled"
        ? (creditsRes.value?.data?.content || creditsRes.value?.data || [])
        : [];

      const transactions = transactionsRes.status === "fulfilled"
        ? (transactionsRes.value?.data?.content || transactionsRes.value?.data || [])
        : [];
      // Log if transactions failed
      if (transactionsRes.status === "rejected" || !transactionsRes.value?.success) {
        console.warn("Could not load transactions, showing data from journeys and credits only");
      }

      // Calculate stats
      const now = new Date();
      const oneWeekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
      const oneMonthAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);

      // CO2 Savings (from journeys)
      const totalCO2Saved = journeys.reduce((sum, j) =>
        sum + (j.co2ReducedKg || j.creditsEarned || 0), 0
      );

      const monthCO2 = journeys
        .filter(j => new Date(j.journeyDate) >= oneMonthAgo)
        .reduce((sum, j) => sum + (j.co2ReducedKg || j.creditsEarned || 0), 0);

      const weekCO2 = journeys
        .filter(j => new Date(j.journeyDate) >= oneWeekAgo)
        .reduce((sum, j) => sum + (j.co2ReducedKg || j.creditsEarned || 0), 0);

      // Revenue (from completed sales where user is seller)
      const completedSales = transactions.filter(
        t => t.status === "COMPLETED" && t.seller?.id === currentUserId
      );

      const totalRevenue = completedSales.reduce(
        (sum, t) => sum + (t.totalPrice || 0), 0
      );

      const monthRevenue = completedSales
        .filter(t => new Date(t.createdAt) >= oneMonthAgo)
        .reduce((sum, t) => sum + (t.totalPrice || 0), 0);

      const weekRevenue = completedSales
        .filter(t => new Date(t.createdAt) >= oneWeekAgo)
        .reduce((sum, t) => sum + (t.totalPrice || 0), 0);

      // Total distance
      const totalDistance = journeys.reduce((sum, j) =>
        sum + (j.distanceKm || 0), 0
      );

      // Credits created (all verified credits)
      const creditsCreated = credits
        .filter(c => c.status === "VERIFIED")
        .reduce((sum, c) => sum + (c.creditAmount || 0), 0);

      // Credits sold
      const creditsSold = completedSales.reduce(
        (sum, t) => sum + (t.carbonCreditsAmount || 0), 0
      );

      // Completed transactions count
      const completedTransactions = completedSales.length;

      setStats({
        totalCO2Saved,
        monthCO2,
        weekCO2,
        totalRevenue,
        monthRevenue,
        weekRevenue,
        totalDistance,
        creditsCreated,
        creditsSold,
        completedTransactions,
      });
    } catch (err) {
      console.error("Error loading report data:", err);
      setError("Failed to load report data");
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-screen w-screen bg-[#F9FAFB] overflow-hidden">
        <Sidebar />
        <div className="flex flex-col flex-1 min-h-screen w-full">
          <Header />
          <main className="flex-1 flex items-center justify-center">
            <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Loading report...</p>
            </div>
          </main>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen w-screen bg-[#F9FAFB] overflow-hidden">
      <Sidebar />

      <div className="flex flex-col flex-1 min-h-screen w-full">
        <Header />

        <main className="flex-1 p-8 w-full bg-[#F9FAFB] overflow-y-auto">
          <div className="w-full max-w-7xl mx-auto space-y-8">
            {/* Header Section */}
            <div className="flex justify-between items-center">
              <div>
                <h2 className="text-xl font-semibold text-gray-800">Report</h2>
                <p className="text-gray-500 text-sm">
                  Summary of your CO₂ savings and carbon credit revenue
                </p>
              </div>
              <button
                onClick={loadReportData}
                className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition"
              >
                <RefreshCw className="w-4 h-4" />
                <span className="text-sm">Refresh</span>
              </button>
            </div>

            {/* Error Alert */}
            {error && (
              <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
                <p className="text-red-600 text-sm">{error}</p>
              </div>
            )}

            {/* Row 1: CO₂ + Revenue */}
            <div className="grid grid-cols-2 gap-6 w-full">
              {/* Card 1: CO₂ Savings Report */}
              <div className="bg-white border border-gray-300 rounded-2xl p-6 shadow-sm">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <Leaf className="text-green-600" size={22} />
                    <h3 className="font-semibold text-gray-700">
                      CO₂ Savings Report
                    </h3>
                  </div>
                </div>

                <div className="text-center space-y-1 mb-6">
                  <p className="text-3xl font-semibold text-green-600">
                    {stats.totalCO2Saved.toFixed(2)} kg
                  </p>
                  <p className="text-sm text-gray-500">
                    Total CO₂ saved (All time)
                  </p>
                </div>

                <div className="flex justify-between text-center border-t border-gray-200 pt-4">
                  <div className="flex-1">
                    <p className="text-xl font-semibold text-gray-800">
                      {stats.monthCO2.toFixed(2)} kg
                    </p>
                    <p className="text-sm text-gray-500">This month</p>
                  </div>
                  <div className="w-px bg-gray-200 mx-6" />
                  <div className="flex-1">
                    <p className="text-xl font-semibold text-gray-800">
                      {stats.weekCO2.toFixed(2)} kg
                    </p>
                    <p className="text-sm text-gray-500">This week</p>
                  </div>
                </div>
              </div>

              {/* Card 2: Revenue from Carbon Credits */}
              <div className="bg-white border border-gray-300 rounded-2xl p-6 shadow-sm">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <DollarSign className="text-blue-600" size={22} />
                    <h3 className="font-semibold text-gray-700">
                      Revenue from Carbon Credits
                    </h3>
                  </div>
                </div>

                <div className="text-center space-y-1 mb-6">
                  <p className="text-3xl font-semibold text-blue-600">
                    {stats.totalRevenue.toLocaleString()} $
                  </p>
                  <p className="text-sm text-gray-500">
                    Total revenue (All time)
                  </p>
                </div>

                <div className="flex justify-between text-center border-t border-gray-200 pt-4">
                  <div className="flex-1">
                    <p className="text-xl font-semibold text-gray-800">
                      {stats.monthRevenue.toLocaleString()} $
                    </p>
                    <p className="text-sm text-gray-500">This month</p>
                  </div>
                  <div className="w-px bg-gray-200 mx-6" />
                  <div className="flex-1">
                    <p className="text-xl font-semibold text-gray-800">
                      {stats.weekRevenue.toLocaleString()} $
                    </p>
                    <p className="text-sm text-gray-500">This week</p>
                  </div>
                </div>
              </div>
            </div>

            {/* Row 2: Activity Overview */}
            <div className="bg-white border border-gray-300 rounded-2xl p-6 shadow-sm">
              <div className="flex items-center gap-2 mb-6">
                <BarChart3 className="text-green-600" size={22} />
                <h3 className="font-semibold text-gray-700">Activity Overview</h3>
              </div>

              <div className="grid grid-cols-2 md:grid-cols-4 gap-6 text-center border-t border-gray-200 pt-4">
                <div>
                  <p className="text-2xl font-semibold text-gray-800">
                    {stats.totalDistance.toFixed(1)} km
                  </p>
                  <p className="text-sm text-gray-500">Total distance</p>
                </div>
                <div className="w-px bg-gray-200 hidden md:block" />
                <div>
                  <p className="text-2xl font-semibold text-gray-800">
                    {stats.creditsCreated.toFixed(2)} tCO₂
                  </p>
                  <p className="text-sm text-gray-500">Credits created</p>
                </div>
                <div className="w-px bg-gray-200 hidden md:block" />
                <div>
                  <p className="text-2xl font-semibold text-gray-800">
                    {stats.creditsSold.toFixed(2)} tCO₂
                  </p>
                  <p className="text-sm text-gray-500">Credits sold</p>
                </div>
                <div className="w-px bg-gray-200 hidden md:block" />
                <div>
                  <p className="text-2xl font-semibold text-gray-800">
                    {stats.completedTransactions}
                  </p>
                  <p className="text-sm text-gray-500">Completed transactions</p>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
