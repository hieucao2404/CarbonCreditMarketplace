// src/services/walletService.js
import axiosInstance from "../api/axiosInstance";

export const walletService = {
  // Get my wallet
  getMyWallet: async () => {
    const response = await axiosInstance.get("/wallets/my-wallet");
    return response.data;
  },

  // Deposit via VNPay
  depositViaVNPay: async (amountUsd) => {
    const response = await axiosInstance.post("/wallets/deposit/vnpay", {
      amountUsd,
    });
    return response.data;
  },

  // Deposit via MoMo
  depositViaMoMo: async (amountUsd) => {
    const response = await axiosInstance.post("/wallets/deposit/momo", {
      amountUsd,
    });
    return response.data;
  },

  // Deposit funds (generic)
  depositFunds: async (amount, paymentMethodId) => {
    const response = await axiosInstance.post("/wallets/deposit", {
      amount,
      paymentMethodId,
    });
    return response.data;
  },

  // Withdraw funds
  withdrawFunds: async (amount, bankAccountInfo) => {
    const response = await axiosInstance.post("/wallets/withdraw", {
      amount,
      bankAccountInfo,
    });
    return response.data;
  },
  // Deposit via MoMo
  depositViaMoMo: async (amountUsd) => {
    const response = await axiosInstance.post("/wallets/deposit/momo", {
      amountUsd,
    });
    return response.data;
  },

  // Get wallet transactions
  getWalletTransactions: async (page = 0, size = 10) => {
    const response = await axiosInstance.get("/wallets/transactions", {
      params: { page, size },
    });
    return response.data;
  },

  // Admin: Get user's wallet
  getUserWallet: async (userId) => {
    const response = await axiosInstance.get(`/wallets/admin/user/${userId}`);
    return response.data;
  },

  // Admin: Update user balance
  updateUserBalance: async (userId, creditAmount, cashAmount, reason) => {
    const response = await axiosInstance.put(
      `/wallets/admin/user/${userId}/balance`,
      null,
      {
        params: { creditAmount, cashAmount, reason },
      }
    );
    return response.data;
  },

  // Test complete MoMo payment
  testCompleteMoMo: async (orderId) => {
    const response = await axiosInstance.get("/wallets/test-complete-momo", {
      params: { orderId },
    });
    return response.data;
  },
};
