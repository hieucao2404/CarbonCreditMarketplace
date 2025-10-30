// src/pages/CarbonWallet.jsx
import { useState, useEffect } from "react";
import Sidebar from "../components/Sidebar";
import Header from "../components/Header";
import { walletService } from "../services/walletService";
import { carbonCreditService } from "../services/carbonCreditService";
import { transactionService } from "../services/transactionService";
import { creditListingService } from "../services/creditListingService";
import { journeyService } from "../services/journeyService";
import {
  Wallet,
  Clock,
  TrendingUp,
  Leaf,
  DollarSign,
  RefreshCw
} from "lucide-react";

export default function CarbonWallet() {
  const [wallet, setWallet] = useState(null);
  const [credits, setCredits] = useState([]);
  const [listings, setListings] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [journeys, setJourneys] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    loadWalletData();
  }, []);

  const loadWalletData = async () => {
    setLoading(true);
    setError("");

    try {
      const [walletRes, creditsRes, listingsRes, transactionsRes, journeysRes] = await Promise.allSettled([
        walletService.getMyWallet(),
        carbonCreditService.getMyCredits(0, 100),
        creditListingService.getMyListings(0, 100),
        transactionService.getMyTransactions(0, 20),
        journeyService.getMyJourneys(0, 50),
      ]);

      // Wallet
      if (walletRes.status === "fulfilled" && walletRes.value?.success) {
        setWallet(walletRes.value.data);
      }

      // Credits
      if (creditsRes.status === "fulfilled" && creditsRes.value?.success) {
        const creditData = creditsRes.value.data.content || creditsRes.value.data;
        setCredits(Array.isArray(creditData) ? creditData : []);
      }

      // Listings
      if (listingsRes.status === "fulfilled" && listingsRes.value?.success) {
        const listingData = listingsRes.value.data.content || listingsRes.value.data;
        setListings(Array.isArray(listingData) ? listingData : []);
      }

      // Journeys
      if (journeysRes.status === "fulfilled" && journeysRes.value?.success) {
        const journeyData = journeysRes.value.data.content || journeysRes.value.data;
        setJourneys(Array.isArray(journeyData) ? journeyData : []);
      }

      // Transactions
      if (transactionsRes.status === "fulfilled" && transactionsRes.value?.success) {
        const txData = transactionsRes.value.data.content || transactionsRes.value.data;
        setTransactions(Array.isArray(txData) ? txData : []);
      }
    } catch (err) {
      console.error("Error loading wallet data:", err);
      setError("Failed to load wallet data");
    } finally {
      setLoading(false);
    }
  };

  const calculateStats = () => {
    const user = JSON.parse(localStorage.getItem("user"));
    const currentUserId = user?.id;
    // 1️⃣ CURRENT BALANCE: Sum of creditAmount from ACTIVE listings
    const currentBalance = listings
      .filter(l => l.status === "ACTIVE")
      .reduce((sum, l) => sum + (l.creditAmount || 0), 0);

    // 2️⃣ PENDING BALANCE: Sum of creditAmount from PENDING_APPROVAL listings
    const pendingBalance = listings
      .filter(l => l.status === "PENDING_APPROVAL")
      .reduce((sum, l) => sum + (l.creditAmount || 0), 0);

    // 3️⃣ TOTAL SOLD: Completed credit sales
    const completedSales = transactions.filter(
      t => t.status === "COMPLETED" &&
        t.seller?.id === currentUserId
    );

    const totalSold = completedSales.reduce(
      (sum, t) => sum + (t.carbonCreditsAmount || 0), 0
    );

    const totalRevenue = completedSales.reduce(
      (sum, t) => sum + (t.totalPrice || 0), 0
    );

    return {
      currentBalance: currentBalance.toFixed(2),
      pendingBalance: pendingBalance.toFixed(2),
      totalSold: totalSold.toFixed(2),
      totalRevenue: totalRevenue.toFixed(2),
    };
  };

  const stats = calculateStats();

  // Format date
  const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    const date = new Date(dateString);
    return date.toLocaleDateString("vi-VN");
  };

  // Get transaction icon and details
  const getTransactionDisplay = (transaction) => {
    const user = JSON.parse(localStorage.getItem("user"));
    const currentUserId = user?.id;

    const isSale = transaction.seller?.id === currentUserId;
    const isPurchase = transaction.buyer?.id === currentUserId;

    let title = "Unknown Transaction";
    let icon = <Wallet className="text-gray-600" size={18} />;
    let amountColor = "text-gray-600";

    if (isSale) {
      title = `Sold to ${transaction.buyer?.username || "Buyer"}`;
      icon = <DollarSign className="text-red-500" size={18} />;
      amountColor = "text-red-500";
    } else if (isPurchase) {
      title = `Purchased from ${transaction.seller?.username || "Seller"}`;
      icon = <TrendingUp className="text-green-600" size={18} />;
      amountColor = "text-green-600";
    }

    return {
      title,
      icon,
      amountColor,
      amount: `${isPurchase ? "+" : "-"}${(transaction.carbonCreditsAmount || 0).toFixed(2)} tCO₂`
    };
  };


  // Combined activity feed
  const getActivityFeed = () => {
    const activities = [];

    // 1️⃣ JOURNEY ACTIVITIES
    journeys.forEach(journey => {
      let statusIcon = <Leaf className="text-green-600" size={18} />;
      let statusColor = "text-green-600";
      let statusText = "Completed";

      if (journey.verificationStatus === "PENDING") {
        statusIcon = <Clock className="text-orange-500" size={18} />;
        statusColor = "text-orange-500";
        statusText = "Pending Verification";
      } else if (journey.verificationStatus === "REJECTED") {
        statusIcon = <Leaf className="text-red-500" size={18} />;
        statusColor = "text-red-500";
        statusText = "Rejected";
      }

      activities.push({
        type: "journey",
        date: journey.journeyDate || journey.createdAt,
        title: `Journey: ${(journey.distanceKm || 0).toFixed(1)} km`,
        subtitle: `${journey.vehicle?.model || "EV"} • ${statusText}`,
        amount: journey.verificationStatus === "VERIFIED"
          ? `+${(journey.creditsEarned || journey.co2ReducedKg || 0).toFixed(2)} tCO₂`
          : "Processing",
        color: statusColor,
        icon: statusIcon,
      });
    });

    // 2️⃣ CREDIT ACTIVITIES (Verification status changes)
    credits.forEach(credit => {
      if (credit.verifiedAt) {
        activities.push({
          type: "credit",
          date: credit.verifiedAt,
          title: `Credit Verified`,
          subtitle: `${(credit.creditAmount || 0).toFixed(2)} tCO₂ verified by CVA`,
          amount: `+${(credit.creditAmount || 0).toFixed(2)} tCO₂`,
          color: "text-green-600",
          icon: <Leaf className="text-green-600" size={18} />,
        });
      }
    });

    // 3️⃣ LISTING ACTIVITIES
    listings.forEach(listing => {
      let listingIcon = <TrendingUp className="text-blue-600" size={18} />;
      let listingColor = "text-blue-600";
      let listingStatus = "Created";

      if (listing.status === "ACTIVE") {
        listingIcon = <TrendingUp className="text-green-600" size={18} />;
        listingColor = "text-green-600";
        listingStatus = "Listing Approved";
      } else if (listing.status === "PENDING_APPROVAL") {
        listingIcon = <Clock className="text-orange-500" size={18} />;
        listingColor = "text-orange-500";
        listingStatus = "Awaiting CVA Approval";
      } else if (listing.status === "REJECTED") {
        listingIcon = <TrendingUp className="text-red-500" size={18} />;
        listingColor = "text-red-500";
        listingStatus = "Listing Rejected";
      } else if (listing.status === "CANCELLED") {
        listingIcon = <TrendingUp className="text-gray-500" size={18} />;
        listingColor = "text-gray-500";
        listingStatus = "Listing Cancelled";
      } else if (listing.status === "CLOSED") {
        listingIcon = <DollarSign className="text-purple-600" size={18} />;
        listingColor = "text-purple-600";
        listingStatus = "Listing Closed (Sold)";
      }

      activities.push({
        type: "listing",
        date: listing.updatedAt || listing.createdAt,
        title: listingStatus,
        subtitle: `${(listing.creditAmount || 0).toFixed(2)} tCO₂ @ ${listing.price} VND/tCO₂`,
        amount: listing.status === "CLOSED"
          ? `${(listing.creditAmount * listing.price).toLocaleString()} VND`
          : `${(listing.creditAmount || 0).toFixed(2)} tCO₂`,
        color: listingColor,
        icon: listingIcon,
      });
    });

    // 4️⃣ TRANSACTION ACTIVITIES
    transactions.forEach(tx => {
      const display = getTransactionDisplay(tx);
      activities.push({
        type: "transaction",
        date: tx.createdAt,
        title: display.title,
        subtitle: `Status: ${tx.status}`,
        amount: display.amount,
        color: display.amountColor,
        icon: display.icon,
      });
    });

    // Sort by date (newest first) and take last 20
    return activities
      .sort((a, b) => new Date(b.date) - new Date(a.date))
      .slice(0, 50);
  };

  const activities = getActivityFeed();

  const statsData = [
    {
      icon: <Wallet className="text-green-600" size={22} />,
      title: "Available for Sale",
      value: `${(wallet?.creditBalance || 0).toFixed(2)} tCO₂`,
      sub: "Active listings on marketplace",
      color: "text-green-600",
    },
    {
      icon: <Clock className="text-orange-500" size={22} />,
      title: "Pending CVA Approval",
      value: `${stats.pendingBalance} tCO₂`,
      sub: "Awaiting listing approval",
      color: "text-orange-500",
    },
    {
      icon: <TrendingUp className="text-blue-600" size={22} />,
      title: "Total Sold",
      value: `${stats.totalSold} tCO₂`,
      sub: `Revenue: ${stats.totalRevenue} VND`,
      color: "text-blue-600",
    },
  ];

  if (loading) {
    return (
      <div className="flex min-h-screen w-screen bg-[#F9FAFB]">
        <Sidebar />
        <div className="flex flex-col flex-1">
          <Header />
          <main className="flex-1 flex items-center justify-center">
            <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Loading wallet...</p>
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
          <div className="w-full max-w-full mx-auto space-y-8">
            {/* Header section */}
            <div className="flex justify-between items-center">
              <div>
                <h2 className="text-xl font-semibold text-gray-800">Carbon Wallet</h2>
                <p className="text-gray-500 text-sm">
                  Track your carbon credit balance and transaction history
                </p>
              </div>
              <button
                onClick={loadWalletData}
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

            {/* 3 statistic cards */}
            <div className="grid grid-cols-3 gap-6 w-full">
              {statsData.map((item, i) => (
                <div
                  key={i}
                  className="bg-white rounded-2xl border border-gray-300 shadow-sm p-6 transition-all hover:shadow-md flex flex-col justify-between"
                >
                  <div className="flex items-center gap-2 mb-2 text-gray-700 font-medium">
                    {item.icon}
                    <span>{item.title}</span>
                  </div>
                  <p className={`text-3xl font-semibold ${item.color}`}>
                    {item.value}
                  </p>
                  <p className="text-sm text-gray-500">{item.sub}</p>
                </div>
              ))}
            </div>

            {/* Activity Feed with Scrollable Container */}
            <div className="bg-gray-50 rounded-2xl border border-gray-200 shadow-sm p-6">
              <div className="flex justify-between items-center mb-4">
                <h3 className="font-semibold text-gray-700 text-lg">
                  Recent Activity ({activities.length})
                </h3>
                <div className="flex gap-2 text-xs">
                  <span className="px-2 py-1 bg-green-100 text-green-700 rounded">Journeys</span>
                  <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded">Listings</span>
                  <span className="px-2 py-1 bg-purple-100 text-purple-700 rounded">Sales</span>
                </div>
              </div>

              {activities.length === 0 ? (
                <div className="text-center py-12">
                  <Wallet className="w-12 h-12 text-gray-400 mx-auto mb-3" />
                  <p className="text-gray-500 text-sm">No activity yet</p>
                  <p className="text-gray-400 text-xs mt-1">
                    Your activities will appear here
                  </p>
                </div>
              ) : (
                <div className="max-h-96 overflow-y-auto pr-2 space-y-2">
                  {activities.map((activity, i) => (
                    <div
                      key={i}
                      className="flex justify-between items-center py-3 px-4 transition-all hover:bg-white hover:shadow-sm rounded-xl"
                    >
                      <div className="flex items-center gap-3">
                        <div className="bg-gray-100 p-2 rounded-full">
                          {activity.icon}
                        </div>
                        <div>
                          <p className="font-medium text-gray-800 text-sm">
                            {activity.title}
                          </p>
                          <p className="text-xs text-gray-500">{activity.subtitle}</p>
                          <p className="text-xs text-gray-400 mt-1">
                            {formatDate(activity.date)}
                          </p>
                        </div>
                      </div>
                      <p className={`text-sm font-semibold ${activity.color} whitespace-nowrap ml-4`}>
                        {activity.amount}
                      </p>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Quick Actions */}
            <div className="grid grid-cols-2 gap-4">
              <button
                onClick={() => window.location.href = "/listing"}
                className="p-6 bg-white border border-gray-200 rounded-xl hover:shadow-md transition text-left"
              >
                <TrendingUp className="w-8 h-8 text-green-600 mb-3" />
                <h4 className="font-semibold text-gray-800 mb-1">Sell Credits</h4>
                <p className="text-sm text-gray-500">
                  List your carbon credits on the marketplace
                </p>
              </button>

              <button
                onClick={() => window.location.href = "/journeys"}
                className="p-6 bg-white border border-gray-200 rounded-xl hover:shadow-md transition text-left"
              >
                <Leaf className="w-8 h-8 text-blue-600 mb-3" />
                <h4 className="font-semibold text-gray-800 mb-1">Earn More Credits</h4>
                <p className="text-sm text-gray-500">
                  Record new EV journeys to earn carbon credits
                </p>
              </button>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
