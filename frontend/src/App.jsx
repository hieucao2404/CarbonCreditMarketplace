// src/App.jsx
import { Routes, Route, Navigate } from "react-router-dom";
import ProtectedRoute from "./components/ProtectedRoute";

// Public pages
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import ForgotPasswordPage from "./pages/ForgotPasswordPage";
import UnauthorizedPage from "./pages/UnauthorizedPage";


// EV Owner pages
import HomePage from "./pages/HomePage";
import VehicleManagement from "./pages/VehicleManagement";
import CarbonWallet from "./pages/CarbonWallet";
import CarbonListing from "./pages/CarbonListing";
import Transactions from "./pages/Transactions";
import Report from "./pages/Report";
import AddJourney from "./pages/JourneyManagement";
// Buyer pages
import BuyerDashboard from "./pages/BuyerDashboard";
import BuyerMarket from "./pages/BuyerMarket";
import BuyerTransactions from "./pages/BuyerTransactions";
import BuyerCertificates from "./pages/BuyerCertificates";
import BuyerHistory from "./pages/BuyerHistory";

// Verifier/CVA pages
import VerifierDashboard from "./pages/VerifierDashboard";
import VerifierPending from "./pages/VerifierPending";
import VerifierApproved from "./pages/VerifierApproved";
import VerifierReports from "./pages/VerifierReports";

// Admin pages
import AdminDashboard from "./pages/AdminDashboard";
import AdminUserManagement from "./pages/AdminUserManagement";
import AdminTransactionManagement from "./pages/AdminTransactionManagement";
import AdminStatistics from "./pages/AdminStatistics";
import AdminSystemSettings from "./pages/AdminSystemSettings";

function App() {
  return (
    <Routes>
      {/* ========== PUBLIC ROUTES ========== */}
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/unauthorized" element={<UnauthorizedPage />} />

      {/* ========== EV OWNER ROUTES ========== */}
      <Route
        path="/home"
        element={
          <ProtectedRoute allowedRoles={["EV_OWNER"]}>
            <HomePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/journeys"
        element={
          <ProtectedRoute allowedRoles={["EV_OWNER"]}>
            <AddJourney />
          </ProtectedRoute>
        }
      />
      <Route
        path="/vehicles"
        element={
          <ProtectedRoute allowedRoles={["EV_OWNER"]}>
            <VehicleManagement />
          </ProtectedRoute>
        }
      />
      <Route
        path="/wallet"
        element={
          <ProtectedRoute allowedRoles={["EV_OWNER"]}>
            <CarbonWallet />
          </ProtectedRoute>
        }
      />
      <Route
        path="/listing"
        element={
          <ProtectedRoute allowedRoles={["EV_OWNER"]}>
            <CarbonListing />
          </ProtectedRoute>
        }
      />
      <Route
        path="/transactions"
        element={
          <ProtectedRoute allowedRoles={["EV_OWNER"]}>
            <Transactions />
          </ProtectedRoute>
        }
      />
      <Route
        path="/reports"
        element={
          <ProtectedRoute allowedRoles={["EV_OWNER"]}>
            <Report />
          </ProtectedRoute>
        }
      />

      {/* ========== BUYER ROUTES ========== */}
      <Route
        path="/buyer"
        element={
          <ProtectedRoute allowedRoles={["BUYER"]}>
            <BuyerDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/buyer/market"
        element={
          <ProtectedRoute allowedRoles={["BUYER"]}>
            <BuyerMarket />
          </ProtectedRoute>
        }
      />
      <Route
        path="/buyer/transactions"
        element={
          <ProtectedRoute allowedRoles={["BUYER"]}>
            <BuyerTransactions />
          </ProtectedRoute>
        }
      />
      <Route
        path="/buyer/certificates"
        element={
          <ProtectedRoute allowedRoles={["BUYER"]}>
            <BuyerCertificates />
          </ProtectedRoute>
        }
      />
      <Route
        path="/buyer/history"
        element={
          <ProtectedRoute allowedRoles={["BUYER"]}>
            <BuyerHistory />
          </ProtectedRoute>
        }
      />

      {/* ========== CVA/VERIFIER ROUTES ========== */}
      <Route
        path="/verifier"
        element={
          <ProtectedRoute allowedRoles={["CVA"]}>
            <VerifierDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/verifier/pending"
        element={
          <ProtectedRoute allowedRoles={["CVA"]}>
            <VerifierPending />
          </ProtectedRoute>
        }
      />
      <Route
        path="/verifier/approved"
        element={
          <ProtectedRoute allowedRoles={["CVA"]}>
            <VerifierApproved />
          </ProtectedRoute>
        }
      />
      <Route
        path="/verifier/reports"
        element={
          <ProtectedRoute allowedRoles={["CVA"]}>
            <VerifierReports />
          </ProtectedRoute>
        }
      />

      {/* ========== ADMIN ROUTES ========== */}
      <Route
        path="/admin"
        element={
          <ProtectedRoute allowedRoles={["ADMIN"]}>
            <AdminDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/users"
        element={
          <ProtectedRoute allowedRoles={["ADMIN"]}>
            <AdminUserManagement />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/transactions"
        element={
          <ProtectedRoute allowedRoles={["ADMIN"]}>
            <AdminTransactionManagement />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/stats"
        element={
          <ProtectedRoute allowedRoles={["ADMIN"]}>
            <AdminStatistics />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/settings"
        element={
          <ProtectedRoute allowedRoles={["ADMIN"]}>
            <AdminSystemSettings />
          </ProtectedRoute>
        }
      />

      {/* ========== CATCH-ALL ROUTE ========== */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

export default App;
