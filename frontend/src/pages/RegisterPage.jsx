import React, { useState } from "react";
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
  const navigate = useNavigate();

  // ✅ Cập nhật dữ liệu khi người dùng nhập
  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  // ✅ Hàm xử lý đăng ký
  const handleRegister = async () => {
    const { role, username, email, password, fullName, phone } = formData;
    if (!role || !username || !email || !password || !fullName || !phone) {
      alert("⚠️ Vui lòng nhập đầy đủ thông tin!");
      return;
    }

    setLoading(true);
    try {
      const res = await fetch("http://localhost:8080/api/users/register", {
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

  // ✅ Bắt sự kiện nhấn Enter để submit
  const handleKeyDown = (e) => {
    if (e.key === "Enter") {
      handleRegister();
    }
  };

  return (
    <div
      className="min-h-screen flex flex-col items-center justify-center bg-gradient-to-br from-green-50 to-blue-50 p-6 relative overflow-hidden"
      onKeyDown={handleKeyDown}
    >
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
          className="w-full border border-gray-200 rounded-lg p-3 mb-4 focus:outline-none focus:ring-2 focus:ring-green-400"
        >
          <option value="">Chọn vai trò</option>
          <option value="EV_OWNER">Chủ sở hữu xe điện</option>
          <option value="BUYER">Người mua tín chỉ carbon</option>
          <option value="CVA">Người xác minh</option>
          <option value="ADMIN">Quản trị viên</option>
        </select>

        <label className="block mb-2 text-gray-700 text-sm font-medium">
          Tên đăng nhập
        </label>
        <input
          type="text"
          name="username"
          placeholder="Nhập tên đăng nhập"
          value={formData.username}
          onChange={handleChange}
          className="w-full border border-gray-200 rounded-lg p-3 mb-4 focus:outline-none focus:ring-2 focus:ring-green-400"
        />

        <label className="block mb-2 text-gray-700 text-sm font-medium">
          Họ và tên
        </label>
        <input
          type="text"
          name="fullName"
          placeholder="Nhập họ và tên"
          value={formData.fullName}
          onChange={handleChange}
          className="w-full border border-gray-200 rounded-lg p-3 mb-4 focus:outline-none focus:ring-2 focus:ring-green-400"
        />

        <label className="block mb-2 text-gray-700 text-sm font-medium">
          Email
        </label>
        <input
          type="email"
          name="email"
          placeholder="Nhập email"
          value={formData.email}
          onChange={handleChange}
          className="w-full border border-gray-200 rounded-lg p-3 mb-4 focus:outline-none focus:ring-2 focus:ring-green-400"
        />

        <label className="block mb-2 text-gray-700 text-sm font-medium">
          Mật khẩu
        </label>
        <input
          type="password"
          name="password"
          placeholder="Nhập mật khẩu (tối thiểu 8 ký tự)"
          value={formData.password}
          onChange={handleChange}
          className="w-full border border-gray-200 rounded-lg p-3 mb-4 focus:outline-none focus:ring-2 focus:ring-green-400"
        />

        <label className="block mb-2 text-gray-700 text-sm font-medium">
          Số điện thoại
        </label>
        <input
          type="text"
          name="phone"
          placeholder="Nhập số điện thoại"
          value={formData.phone}
          onChange={handleChange}
          className="w-full border border-gray-200 rounded-lg p-3 mb-6 focus:outline-none focus:ring-2 focus:ring-green-400"
        />

        <button
          onClick={handleRegister}
          disabled={loading}
          className={`w-full py-3 rounded-lg transition font-medium text-white ${
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
