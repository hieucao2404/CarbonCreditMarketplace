import React, { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { Leaf, Eye, EyeOff } from "lucide-react"; // 👈 thêm icon con mắt
import { motion, AnimatePresence } from "framer-motion";

const LoginPage = () => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false); // 👈 trạng thái bật/tắt password
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [showSuccess, setShowSuccess] = useState(false);
  const navigate = useNavigate();

  const API_URL = "http://localhost:8080/api/users/login";

  const handleLogin = async () => {
    setError("");

    if (!username || !password) {
      setError("Vui lòng nhập đầy đủ thông tin đăng nhập!");
      return;
    }

    try {
      setLoading(true);
      const response = await axios.post(API_URL, { username, password });
      const data = response.data;

      if (data.success) {
        const user = data.data.user;
        const token = data.data.token;

        localStorage.setItem("token", token);
        localStorage.setItem("user", JSON.stringify(user));

        setShowSuccess(true);
        setTimeout(() => {
          setShowSuccess(false);
          switch (user.role) {
            case "EV_OWNER":
              navigate("/home");
              break;
            case "BUYER":
              navigate("/buyer");
              break;
            case "CVA":
              navigate("/verifier");
              break;
            case "ADMIN":
              navigate("/admin");
              break;
            default:
              navigate("/");
          }
        }, 2500);
      } else {
        setError(data.message || "Đăng nhập thất bại!");
      }
    } catch (err) {
      console.error("Login error:", err);
      setError(err.response?.data?.message || "Sai tên đăng nhập hoặc mật khẩu!");
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === "Enter") handleLogin();
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-gradient-to-br from-green-50 to-blue-50 p-6 relative overflow-hidden">
      {/* ✅ Popup tick xanh */}
      <AnimatePresence>
        {showSuccess && (
          <motion.div
            className="fixed inset-0 flex items-center justify-center bg-black/40 z-50"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            <motion.div
              className="bg-white rounded-2xl shadow-2xl p-8 flex flex-col items-center"
              initial={{ scale: 0.8, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ type: "spring", stiffness: 200, damping: 15 }}
            >
              <motion.svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 52 52"
                className="w-20 h-20"
              >
                <motion.circle
                  cx="26"
                  cy="26"
                  r="25"
                  fill="none"
                  stroke="#22c55e"
                  strokeWidth="4"
                  strokeDasharray="157"
                  strokeDashoffset="157"
                  initial={{ strokeDashoffset: 157 }}
                  animate={{ strokeDashoffset: 0 }}
                  transition={{ duration: 0.8, ease: "easeOut" }}
                />
                <motion.path
                  fill="none"
                  stroke="#22c55e"
                  strokeWidth="4"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M16 26l7 7 13-13"
                  initial={{ pathLength: 0 }}
                  animate={{ pathLength: 1 }}
                  transition={{ delay: 0.6, duration: 0.6, ease: "easeOut" }}
                />
              </motion.svg>
              <motion.p
                className="text-green-600 font-semibold text-lg mt-4"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 1.0 }}
              >
                Đăng nhập thành công!
              </motion.p>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Logo */}
      <div className="flex flex-col items-center mb-8 text-center">
        <div className="bg-green-100 p-3 rounded-full mb-3">
          <Leaf className="w-6 h-6 text-green-600" />
        </div>
        <h1 className="text-2xl font-semibold text-gray-800">
          Carbon Credit Exchange
        </h1>
        <p className="text-gray-600 text-sm mt-1">
          Nền tảng mua bán tín chỉ carbon từ xe điện
        </p>
      </div>

      {/* Form đăng nhập */}
      <div
        className="bg-white shadow-lg rounded-2xl p-10 w-full max-w-xl border border-gray-100 relative z-10"
        onKeyDown={handleKeyPress}
      >
        <h2 className="text-lg font-semibold text-gray-800 mb-1">
          Đăng nhập hệ thống
        </h2>
        <p className="text-gray-500 text-sm mb-6">
          Nhập thông tin tài khoản của bạn để tiếp tục
        </p>

        {/* Username */}
        <label className="block mb-2 text-gray-700 text-sm font-medium">
          Tên đăng nhập
        </label>
        <input
          type="text"
          placeholder="Nhập username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          className="w-full border border-gray-200 rounded-lg p-3 mb-4 focus:outline-none focus:ring-2 focus:ring-green-400"
        />

        {/* Password với icon con mắt */}
        <label className="block mb-2 text-gray-700 text-sm font-medium">
          Mật khẩu
        </label>
        <div className="relative mb-4">
          <input
            type={showPassword ? "text" : "password"} // 👈 thay đổi loại input
            placeholder="Nhập mật khẩu"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full border border-gray-200 rounded-lg p-3 pr-10 focus:outline-none focus:ring-2 focus:ring-green-400"
          />
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)} // 👈 toggle con mắt
            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-green-600 transition"
          >
            {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        </div>

        {/* Error */}
        {error && (
          <p className="text-red-600 text-sm mb-3 text-center font-medium">
            {error}
          </p>
        )}

        {/* Links */}
        <div className="flex justify-between items-center mb-6 text-sm">
          <button
            onClick={() => navigate("/forgot-password")}
            className="text-green-600 hover:underline font-medium transition"
          >
            Quên mật khẩu?
          </button>
          <button
            onClick={() => navigate("/register")}
            className="text-green-600 hover:underline font-medium transition"
          >
            Đăng ký tài khoản
          </button>
        </div>

        {/* Nút đăng nhập */}
        <button
          onClick={handleLogin}
          disabled={loading}
          className={`w-full py-3 rounded-lg transition font-medium ${
            loading
              ? "bg-gray-400 cursor-not-allowed text-white"
              : "bg-black text-white hover:bg-gray-900"
          }`}
        >
          {loading ? "Đang đăng nhập..." : "Đăng nhập"}
        </button>
      </div>
    </div>
  );
};

export default LoginPage;
