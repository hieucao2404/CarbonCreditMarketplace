// src/services/transactionService.js
import axiosInstance from "../api/axiosInstance";

export const transactionService = {
  // Purchase credit listing
  initiatePurchase: async (listingId) => {
    const response = await axiosInstance.post(`/transactions/purchase/${listingId}`);
    return response.data;
  },

  // Get my transaction history
  getMyTransactions: async (page = 0, size = 10) => {
    const response = await axiosInstance.get("/transactions/my-history", {
      params: { page, size },
    });
    return response.data;
  },

  // Get transaction by ID
  getTransactionById: async (id) => {
    const response = await axiosInstance.get(`/transactions/${id}`);
    return response.data;
  },

  // Cancel transaction
  cancelTransaction: async (id) => {
    const response = await axiosInstance.post(`/transactions/${id}/cancel`);
    return response.data;
  },

  // Admin: Get all transactions
  getAllTransactions: async (page = 0, size = 20) => {
    const response = await axiosInstance.get("/transactions/admin/all", {
      params: { page, size },
    });
    return response.data;
  },

  // Admin: Get transactions by status
  getTransactionsByStatus: async (status, page = 0, size = 10) => {
    const response = await axiosInstance.get("/transactions/admin/by-status", {
      params: { status, page, size },
    });
    return response.data;
  },
};
