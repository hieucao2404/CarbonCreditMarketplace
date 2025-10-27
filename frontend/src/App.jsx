import { Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import ForgotPasswordPage from "./pages/ForgotPasswordPage";

import HomePage from "./pages/HomePage";
import VehicleManagement from "./pages/VehicleManagement";
import CarbonWallet from "./pages/CarbonWallet";
import CarbonListing from "./pages/CarbonListing";
import Transactions from "./pages/Transactions";
import Report from "./pages/Report";

import BuyerDashboard from "./pages/BuyerDashboard";
import BuyerMarket from "./pages/BuyerMarket";
import BuyerTransactions from "./pages/BuyerTransactions";
import BuyerCertificates from "./pages/BuyerCertificates";
import BuyerHistory from "./pages/BuyerHistory";

import VerifierDashboard from "./pages/VerifierDashboard";
import VerifierPending from "./pages/VerifierPending";
import VerifierApproved from "./pages/VerifierApproved";
import VerifierReports from "./pages/VerifierReports";

import AdminDashboard from "./pages/AdminDashboard";
import AdminUserManagement from "./pages/AdminUserManagement";
import AdminTransactionManagement from "./pages/AdminTransactionManagement";
import AdminStatistics from "./pages/AdminStatistics";
import AdminSystemSettings from "./pages/AdminSystemSettings";

function App() {
  return (
    <Routes>
      {/* Trang đăng nhập */}
      <Route path="/" element={<Navigate to="/login" />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />

      {/* Giao diện chính sau khi login */}
      <Route path="/home" element={<HomePage />} />
      <Route path="/vehicles" element={<VehicleManagement />} />
      <Route path="/wallet" element={<CarbonWallet />} />
      <Route path="/listing" element={<CarbonListing />} />
      <Route path="/transactions" element={<Transactions />} />
      <Route path="/reports" element={<Report />} />
      
      <Route path="/buyer" element={<BuyerDashboard />} />
      <Route path="/buyer/market" element={<BuyerMarket />} />
      <Route path="/buyer/transactions" element={<BuyerTransactions />} />
      <Route path="/buyer/certificates" element={<BuyerCertificates />} />
      <Route path="/buyer/history" element={<BuyerHistory />} />

      <Route path="/verifier" element={<VerifierDashboard />} />
      <Route path="/verifier/pending" element={<VerifierPending />} />
      <Route path="/verifier/approved" element={<VerifierApproved />} />
      <Route path="/verifier/reports" element={<VerifierReports />} />

      <Route path="/admin" element={<AdminDashboard />} />
      <Route path="/admin/users" element={<AdminUserManagement />} />
      <Route path="/admin/transactions" element={<AdminTransactionManagement />} />
      <Route path="/admin/stats" element={<AdminStatistics />} />
      <Route path="/admin/settings" element={<AdminSystemSettings />} />
    </Routes>
  );
}

export default App;
