import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Leaf } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

const RegisterPage = () => {
  const [formData, setFormData] = useState({
    role: "",
    username: "",
    email: "",
    password: "",
    fullName: "",
    phone: "",
  });

  const [loading, setLoading] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);
  const [errors, setErrors] = useState({});
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
    
    // Clear error for this field when user starts typing
    if (errors[name]) {
      setErrors({ ...errors, [name]: "" });
    }
  };

  const validateForm = () => {
    const newErrors = {};
    const { role, username, email, password, fullName, phone } = formData;

    // Required fields
    if (!role) newErrors.role = "Vui lòng chọn vai trò";
    if (!username) newErrors.username = "Vui lòng nhập tên đăng nhập";
    if (!email) newErrors.email = "Vui lòng nhập email";
    if (!password) newErrors.password = "Vui lòng nhập mật khẩu";
    if (!fullName) newErrors.fullName = "Vui lòng nhập họ và tên";
    if (!phone) newErrors.phone = "Vui lòng nhập số điện thoại";

    // Username validation: only letters, numbers, and underscores
    if (username && !/^[a-zA-Z0-9_]+$/.test(username)) {
      newErrors.username = "Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới (_)";
    }

    // Password validation: minimum 8 characters
    if (password && password.length < 8) {
      newErrors.password = "Mật khẩu phải có ít nhất 8 ký tự";
    }

    // Email validation
    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      newErrors.email = "Email không hợp lệ";
    }

    // Phone validation: only numbers and minimum 10 digits
    if (phone && !/^\d{10,}$/.test(phone)) {
      newErrors.phone = "Số điện thoại phải có ít nhất 10 chữ số";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleRegister = async () => {
    // Validate form before submitting
    if (!validateForm()) {
      return;
    }

    setLoading(true);
    try {
      const res = await fetch("http://localhost:8080/api/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(formData),
      });

      const data = await res.json();

      if (!res.ok || !data.success) {
        alert("❌ " + (data.message || "Đăng ký thất bại!"));
        setLoading(false);
        return;
      }

      // ✅ Hiển thị popup thành công
      setShowSuccess(true);

      // ⏳ Sau 2.5 giây chuyển sang trang login
      setTimeout(() => {
        setShowSuccess(false);
        navigate("/login");
      }, 2500);
    } catch (error) {
      console.error("Lỗi đăng ký:", error);
      alert("❌ Không thể kết nối tới máy chủ!");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-gradient-to-br from-green-50 to-blue-50 p-6 relative overflow-hidden">
      {/* ✅ Popup đăng ký thành công */}
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
              {/* Animated Circle and Tick */}
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
                Đăng ký thành công!
              </motion.p>
              <motion.p
                className="text-gray-600 text-sm mt-2 text-center max-w-xs"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 1.2 }}
              >
                📧 Vui lòng kiểm tra email để xác thực tài khoản trước khi đăng nhập
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

      {/* Form */}
      <div className="bg-white shadow-lg rounded-2xl p-10 w-full max-w-xl border border-gray-100">
        <h2 className="text-lg font-semibold text-gray-800 mb-1">
          Đăng ký tài khoản
        </h2>
        <p className="text-gray-500 text-sm mb-6">
          Nhập thông tin để tạo tài khoản mới
        </p>

        <label className="block mb-2 text-gray-700 text-sm font-medium">
          Vai trò
        </label>
        <select
          name="role"
          value={formData.role}
          onChange={handleChange}
          className={`w-full border rounded-lg p-3 mb-1 focus:outline-none focus:ring-2 ${
            errors.role
              ? "border-red-400 focus:ring-red-400"
              : "border-gray-200 focus:ring-green-400"
          }`}
        >
          <option value="">Chọn vai trò</option>
          <option value="EV_OWNER">Chủ sở hữu xe điện</option>
          <option value="BUYER">Người mua tín chỉ carbon</option>
          {/* <option value="CVA">Người xác minh</option>
          <option value="ADMIN">Quản trị viên</option> */}
        </select>
        {errors.role && (
          <p className="text-red-500 text-xs mb-3">{errors.role}</p>
        )}

        <label className="block mb-2 text-gray-700 text-sm font-medium mt-2">
          Tên đăng nhập
        </label>
        <input
          type="text"
          name="username"
          placeholder="vd: user123, john_doe"
          value={formData.username}
          onChange={handleChange}
          className={`w-full border rounded-lg p-3 mb-1 focus:outline-none focus:ring-2 ${
            errors.username
              ? "border-red-400 focus:ring-red-400"
              : "border-gray-200 focus:ring-green-400"
          }`}
        />
        {errors.username && (
          <p className="text-red-500 text-xs mb-3">{errors.username}</p>
        )}
        <p className="text-gray-500 text-xs mb-3">
          Chỉ sử dụng chữ cái, số và dấu gạch dưới (_). Không có khoảng trắng.
        </p>

        <label className="block mb-2 text-gray-700 text-sm font-medium">
          Họ và tên
        </label>
        <input
          type="text"
          name="fullName"
          placeholder="Nhập họ và tên"
          value={formData.fullName}
          onChange={handleChange}
          className={`w-full border rounded-lg p-3 mb-1 focus:outline-none focus:ring-2 ${
            errors.fullName
              ? "border-red-400 focus:ring-red-400"
              : "border-gray-200 focus:ring-green-400"
          }`}
        />
        {errors.fullName && (
          <p className="text-red-500 text-xs mb-3">{errors.fullName}</p>
        )}

        <label className="block mb-2 text-gray-700 text-sm font-medium mt-2">
          Email
        </label>
        <input
          type="email"
          name="email"
          placeholder="Nhập email"
          value={formData.email}
          onChange={handleChange}
          className={`w-full border rounded-lg p-3 mb-1 focus:outline-none focus:ring-2 ${
            errors.email
              ? "border-red-400 focus:ring-red-400"
              : "border-gray-200 focus:ring-green-400"
          }`}
        />
        {errors.email && (
          <p className="text-red-500 text-xs mb-3">{errors.email}</p>
        )}

        <label className="block mb-2 text-gray-700 text-sm font-medium mt-2">
          Mật khẩu
        </label>
        <input
          type="password"
          name="password"
          placeholder="Nhập mật khẩu (tối thiểu 8 ký tự)"
          value={formData.password}
          onChange={handleChange}
          className={`w-full border rounded-lg p-3 mb-1 focus:outline-none focus:ring-2 ${
            errors.password
              ? "border-red-400 focus:ring-red-400"
              : "border-gray-200 focus:ring-green-400"
          }`}
        />
        {errors.password && (
          <p className="text-red-500 text-xs mb-3">{errors.password}</p>
        )}

        <label className="block mb-2 text-gray-700 text-sm font-medium mt-2">
          Số điện thoại
        </label>
        <input
          type="text"
          name="phone"
          placeholder="Nhập số điện thoại (10 chữ số)"
          value={formData.phone}
          onChange={handleChange}
          className={`w-full border rounded-lg p-3 mb-1 focus:outline-none focus:ring-2 ${
            errors.phone
              ? "border-red-400 focus:ring-red-400"
              : "border-gray-200 focus:ring-green-400"
          }`}
        />
        {errors.phone && (
          <p className="text-red-500 text-xs mb-3">{errors.phone}</p>
        )}

        <button
          onClick={handleRegister}
          disabled={loading}
          className={`w-full py-3 rounded-lg transition font-medium text-white mt-6 ${
            loading
              ? "bg-gray-400 cursor-not-allowed"
              : "bg-black hover:bg-gray-900"
          }`}
        >
          {loading ? "Đang đăng ký..." : "Đăng ký"}
        </button>

        <p className="text-center text-sm text-gray-600 mt-4">
          Đã có tài khoản?{" "}
          <span
            className="text-green-600 cursor-pointer hover:underline"
            onClick={() => navigate("/login")}
          >
            Đăng nhập ngay
          </span>
        </p>
      </div>
    </div>
  );
};

export default RegisterPage;
