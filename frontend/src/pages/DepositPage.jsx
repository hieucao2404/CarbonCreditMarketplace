import React, { useState } from "react";
import axios from "axios";
import BuyerSidebar from "../components/BuyerSidebar";
import BuyerHeader from "../components/BuyerHeader";
import { CreditCard } from "lucide-react";

export default function DepositPage() {
  const [amount, setAmount] = useState("");
  const [loading, setLoading] = useState(false);
  const [qrUrl, setQrUrl] = useState("");
  const [error, setError] = useState("");

  // ✅ Hàm chỉ cho phép nhập số
  const handleChangeAmount = (e) => {
    const value = e.target.value.replace(/[^0-9]/g, ""); // chỉ giữ lại số
    setAmount(value);
  };

  // ✅ Hàm xử lý nạp tiền
  const handleDeposit = async () => {
    if (!amount || isNaN(amount) || parseFloat(amount) <= 0) {
      setError("Vui lòng nhập số tiền hợp lệ");
      return;
    }

    setLoading(true);
    setError("");
    setQrUrl("");

    try {
      const token = localStorage.getItem("token");

      // ✅ Gọi đúng endpoint và đúng key theo backend
      const res = await axios.post(
        "http://localhost:8080/api/wallets/deposit/vnpay",
        { amountUsd: parseFloat(amount) },
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      // ✅ Backend trả về paymentUrl (hoặc có thể nằm trong res.data.data)
      const paymentUrl = res.data?.paymentUrl || res.data?.data?.paymentUrl;

      if (paymentUrl) {
        setQrUrl(paymentUrl);
      } else {
        console.error("Phản hồi API:", res.data);
        setError("Không thể tạo mã thanh toán. Kiểm tra backend.");
      }
    } catch (err) {
      console.error("Deposit error:", err.response || err.message);
      setError("Đã xảy ra lỗi khi tạo thanh toán.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen bg-gray-50">
      {/* Sidebar */}
      <BuyerSidebar />

      {/* Main content */}
      <div className="flex-1 flex flex-col">
        <BuyerHeader />

        <main className="flex-1 p-8">
          <h1 className="text-2xl font-semibold text-gray-800 mb-6 flex items-center gap-2">
            <CreditCard className="w-6 h-6 text-blue-600" />
            Nạp tiền vào ví
          </h1>

          <div className="bg-white shadow-md rounded-xl p-6 max-w-2xl border border-gray-200">
            <div className="mb-4">
              <label className="block text-gray-700 mb-2 font-medium">
                Số tiền (USD)
              </label>
              <input
                type="text"
                value={amount}
                onChange={handleChangeAmount}
                className="w-full border border-gray-300 rounded-lg px-4 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                placeholder="Nhập số tiền muốn nạp (USD)..."
              />
            </div>

            {error && (
              <p className="text-red-500 text-sm mb-3 text-center">{error}</p>
            )}

            <button
              onClick={handleDeposit}
              disabled={loading}
              className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 rounded-lg transition"
            >
              {loading ? "Đang xử lý..." : "Tạo mã thanh toán"}
            </button>

            {/* Hiển thị QR hoặc link thanh toán */}
            {qrUrl && (
              <div className="mt-6 text-center">
                <p className="text-gray-700 mb-2">
                  Quét mã QR hoặc nhấn vào liên kết bên dưới:
                </p>
                <img
                  src={`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(
                    qrUrl
                  )}`}
                  alt="QR code"
                  className="mx-auto mb-3 border rounded-lg"
                />
                <a
                  href={qrUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-blue-600 underline"
                >
                  Mở liên kết thanh toán
                </a>
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}
