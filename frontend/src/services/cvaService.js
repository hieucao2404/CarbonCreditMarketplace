// src/services/cvaService.js
import axiosInstance from "../api/axiosInstance";

export const cvaService = {
  // Get pending journeys for verification
  getPendingJourneys: async () => {
    const response = await axiosInstance.get("/cva/pending-journeys");
    return response.data;
  },

  // Get journey for review
  getJourneyForReview: async (id) => {
    const response = await axiosInstance.get(`/cva/journey/${id}`);
    return response.data;
  },

  // Approve journey
  approveJourney: async (id, notes = "Approved by CVA") => {
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
