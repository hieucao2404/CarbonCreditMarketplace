import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Leaf } from "lucide-react";

const ForgotPasswordPage = () => {
  const [email, setEmail] = useState("");
  const navigate = useNavigate();

  const handleSendOTP = async () => {
    if (!email) {
      alert("Vui lòng nhập email!");
      return;
    }

    try {
      const res = await fetch("http://localhost:8080/api/users/forgot-password", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
      });

      const data = await res.json();
      if (!data.success) {
        alert(data.message || "Gửi mã thất bại!");
        return;
      }

      alert("Mã OTP đã được gửi đến email của bạn!");
      navigate("/verify-otp", { state: { email } });
    } catch (error) {
      console.error("Lỗi gửi OTP:", error);
      alert("Không thể kết nối máy chủ!");
    }
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-gradient-to-br from-green-50 to-blue-50 p-6">
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

      <div className="bg-white shadow-lg rounded-2xl p-10 w-full max-w-xl border border-gray-100">
        <h2 className="text-lg font-semibold text-gray-800 mb-1">
          Quên mật khẩu
        </h2>
        <p className="text-gray-500 text-sm mb-6">
          Nhập email để nhận mã xác minh
        </p>

        <label className="block mb-2 text-gray-700 text-sm font-medium">
          Email
        </label>
        <input
          type="email"
          placeholder="Nhập email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="w-full border border-gray-200 rounded-lg p-3 mb-6 focus:outline-none focus:ring-2 focus:ring-green-400"
        />

        <button
          onClick={handleSendOTP}
          className="w-full bg-black text-white py-3 rounded-lg hover:bg-gray-900 transition font-medium"
        >
          Gửi mã xác minh
        </button>

        <p className="text-center text-sm text-gray-600 mt-4">
          Quay lại{" "}
          <span
            className="text-green-600 cursor-pointer hover:underline"
            onClick={() => navigate("/login")}
          >
            Đăng nhập
          </span>
        </p>
      </div>
    </div>
  );
};

export default ForgotPasswordPage;
