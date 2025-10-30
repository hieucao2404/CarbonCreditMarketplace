// src/services/cvaService.js
import axiosInstance from "../api/axiosInstance";

export const cvaService = {
  // Get pending journeys for verification
  getPendingJourneys: async () => {
    const response = await axiosInstance.get("/cva/pending-journeys");
    return response.data;
  },

  // ✅ NEW: Get pending listings for verification
  getPendingListings: async (page = 0, size = 50) => {
    const response = await axiosInstance.get("/cva/pending-listings", {
      params: { page, size }
    });
    return response.data;
  },

  // ✅ NEW: Get approved/rejected credits
  getApprovedCredits: async (page = 0, size = 100) => {
    const response = await axiosInstance.get("/cva/approved-credits", {
      params: { page, size }
    });
    return response.data;
  },

  // Get journey for review
  getJourneyForReview: async (id) => {
    const response = await axiosInstance.get(`/cva/journey/${id}`);
    return response.data;
  },

  // Approve journey
  approveJourney: async (id, notes = "") => {
    const response = await axiosInstance.post(`/cva/journey/${id}/approve`, null, {
      params: { notes },
    });
    return response.data;
  },

  // Reject journey
  rejectJourney: async (id, reason) => {
    const response = await axiosInstance.post(`/cva/journey/${id}/reject`, null, {
      params: { reason },
    });
    return response.data;
  },

  // ✅ NEW: Approve listing
  approveListing: async (listingId) => {
    const response = await axiosInstance.post(`/cva/listing/${listingId}/approve`);
    return response.data;
  },

  // ✅ NEW: Reject listing
  rejectListing: async (listingId, reason) => {
    const response = await axiosInstance.post(`/cva/listing/${listingId}/reject`, null, {
      params: { reason },
    });
    return response.data;
  },

  // Get CVA statistics
  getCVAStatistics: async () => {
    const response = await axiosInstance.get("/cva/statistics");
    return response.data;
  },

  // Get my verifications
  getMyVerifications: async () => {
    const response = await axiosInstance.get("/cva/my-verifications");
    return response.data;
  },
};
