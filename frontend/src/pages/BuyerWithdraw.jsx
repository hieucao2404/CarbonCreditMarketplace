import React, { useEffect, useState } from "react";
import axios from "axios";
import Sidebar from "../components/BuyerSidebar";
import BuyerHeader from "../components/BuyerHeader";

export default function BuyerWithdraw() {
  const [wallet, setWallet] = useState(null);
  const [loading, setLoading] = useState(true);
  const [amount, setAmount] = useState("");
  const [bankAccountInfo, setBankAccountInfo] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);

  const token = localStorage.getItem("token");

  useEffect(() => {
    if (!token) {
      setError("Bạn chưa đăng nhập");
      setLoading(false);
      return;
    }
    fetchWallet();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const fetchWallet = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await axios.get("http://localhost:8080/api/wallets/my-wallet", {
        headers: { Authorization: `Bearer ${token}` },
      });
      const walletData = res.data?.data ?? res.data;
      setWallet(walletData);
    } catch (err) {
      console.error("Failed to load wallet:", err);
      setError("Không thể tải thông tin ví");
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage(null);
    setError(null);

    const numericAmount = parseFloat(amount);
    if (isNaN(numericAmount) || numericAmount <= 0) {
      setError("Số tiền không hợp lệ");
      return;
    }

    if (wallet && wallet.cashBalance != null) {
      const cash = parseFloat(wallet.cashBalance);
      if (numericAmount > cash) {
        setError("Số dư không đủ");
        return;
      }
    }

    setSubmitting(true);
    try {
      // Optional server-side balance check
      try {
        const check = await axios.get(
          `http://localhost:8080/api/wallets/balance-check?amount=${numericAmount}&balanceType=CASH`,
          { headers: { Authorization: `Bearer ${token}` } }
        );
        if (check.status === 200 && check.data === false) {
          setError("Server: số dư không đủ");
          setSubmitting(false);
          return;
        }
      } catch (ignored) {
        // continue
      }

      const body = {
        amount: numericAmount,
        bankAccountInfo: bankAccountInfo || "",
      };

      const res = await axios.post("http://localhost:8080/api/wallets/withdraw", body, {
        headers: { Authorization: `Bearer ${token}` },
      });

      const updatedWallet = res.data?.data ?? res.data;
      setWallet(updatedWallet);
      setMessage("Yêu cầu rút tiền thành công");
      setAmount("");
      setBankAccountInfo("");
    } catch (err) {
      console.error("Withdraw failed:", err);
      const serverMsg =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        err?.response?.data?.data ||
        null;
      setError(serverMsg || "Rút tiền thất bại");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen text-gray-600">
        Đang tải thông tin ví...
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-gray-50">
      <Sidebar />
      <div className="flex-1 flex flex-col">
        <BuyerHeader />

        <main className="flex-1 p-8">
          <h1 className="text-2xl font-semibold text-gray-800 mb-6">Rút tiền (Buyer)</h1>

          <div className="bg-white shadow-md rounded-xl p-6 max-w-2xl border border-gray-200">
            <div className="flex items-center gap-4 border-b pb-4 mb-4">
              <div className="bg-blue-100 text-blue-700 font-bold text-xl w-14 h-14 flex items-center justify-center rounded-full">
                {wallet?.username?.charAt(0)?.toUpperCase?.() || "?"}
              </div>
              <div>
                <h2 className="text-xl font-medium text-gray-800">{wallet?.username}</h2>
                <p className="text-sm text-gray-500">
                  Số dư khả dụng:{" "}
                  <span className="font-semibold text-blue-600">
                    {Number(wallet?.cashBalance ?? 0).toLocaleString()} USD
                  </span>
                </p>
              </div>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Số tiền (USD)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  className="w-full border border-gray-200 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-200"
                  placeholder="Nhập số tiền muốn rút"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Ngân hàng / STK / Thông tin nhận</label>
                <textarea
                  value={bankAccountInfo}
                  onChange={(e) => setBankAccountInfo(e.target.value)}
                  className="w-full border border-gray-200 rounded-lg px-3 py-2 h-24 focus:outline-none focus:ring-2 focus:ring-blue-200"
                  placeholder="Tên ngân hàng, số tài khoản, chủ tài khoản, ghi chú..."
                />
              </div>

              {error && <p className="text-sm text-red-600">{error}</p>}
              {message && <p className="text-sm text-blue-600">{message}</p>}

              <div className="flex gap-3">
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 rounded-lg bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-60"
                >
                  {submitting ? "Đang gửi..." : "Xác nhận rút tiền"}
                </button>

                <button
                  type="button"
                  onClick={fetchWallet}
                  className="px-4 py-2 rounded-lg bg-gray-100 hover:bg-gray-200"
                >
                  Làm mới số dư
                </button>
              </div>
            </form>
          </div>
        </main>
      </div>
    </div>
  );
}
