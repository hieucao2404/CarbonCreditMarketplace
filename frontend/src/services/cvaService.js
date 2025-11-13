// src/services/cvaService.js
import axiosInstance from "../api/axiosInstance";

export const cvaService = {
  // --- Original Functions (These are correct) ---

  // Get pending journeys for verification
  getPendingJourneys: async () => {
    const response = await axiosInstance.get("/cva/pending-journeys");
    return response.data;
  },

  // Get pending listings for verification
  getPendingListings: async (page = 0, size = 50) => {
    const response = await axiosInstance.get("/cva/pending-listings", {
      params: { page, size },
    });
    return response.data;
  },

  // Reject journey (from New Journeys tab)
  rejectJourney: async (id, reason) => {
    const response = await axiosInstance.post(
      `/cva/journey/${id}/reject`,
      null,
      {
        params: { reason },
      }
    );
    return response.data;
  },

  // Approve listing
  approveListing: async (listingId) => {
    const response = await axiosInstance.post(
      `/cva/listing/${listingId}/approve`
    );
    return response.data;
  },

  // Reject listing
  rejectListing: async (listingId, reason) => {
    const response = await axiosInstance.post(
      `/cva/listing/${listingId}/reject`,
      null,
      {
        params: { reason },
      }
    );
    return response.data;
  },

  // --- NEW Functions for Inspection Workflow (Corrected) ---

  /**
   * (CVA) Request a physical inspection for a journey
   * Calls: POST /api/verification/journey/{journeyId}/request-inspection
   */
  requestInspection: async (journeyId) => {
    // --- FIX: Removed leading "/api" ---
    return axiosInstance.post(
      `/verification/journey/${journeyId}/request-inspection`
    );
  },

  /**
   * (CVA) Get all appointments assigned to this CVA
   * Calls: GET /api/verification/my-appointments
   */
  getMyInspections: async () => {
    // --- FIX: Removed leading "/api" (This was line 34) ---
    return axiosInstance.get("/verification/my-appointments");
  },

  /**
   * (CVA) CVA marks the inspection as complete (Approve/Reject).
   * Calls: POST /api/verification/appointment/{appointmentId}/complete
   */
  completeInspection: async (appointmentId, isApproved, notes) => {
    // --- FIX: Removed leading "/api" ---
    return axiosInstance.post(
      `/verification/appointment/${appointmentId}/complete`,
      {
        isApproved,
        notes,
      }
    );
  },

  // --- Other CVA Functions (unchanged) ---

  getApprovedCredits: async (page = 0, size = 100) => {
    const response = await axiosInstance.get("/cva/approved-credits", {
      params: { page, size },
    });
    return response.data;
  },

  getJourneyForReview: async (id) => {
    const response = await axiosInstance.get(`/cva/journey/${id}`);
    return response.data;
  },

  getCVAStatistics: async () => {
    const response = await axiosInstance.get("/cva/statistics");
    return response.data;
  },

  getMyVerifications: async () => {
    const response = await axiosInstance.get("/cva/my-verifications");
    return response.data;
  },

  approveJourney: async (id, notes = "") => {
    const response = await axiosInstance.post(
      `/cva/journey/${id}/approve`,
      null,
      {
        params: { notes },
      }
    );
    return response.data;
  },
  /**
   * (CVA) Download a generated PDF report
   * Calls: POST /cva/reports/download
   * @param {object} reportData - The DTO containing report details
   */
  downloadReportPdf: async (reportData) => {
    const response = await axiosInstance.post(
      "/cva/reports/download",
      reportData,
      {
        responseType: "blob", // Important: tells axios to expect a file
      }
    );
    return response.data; // This will be the Blob data
  },
  /**
   * (CVA) Get full details for a single listing for review
   * Calls: GET /cva/listing/{listingId} 
   * (You will need to create this endpoint on the backend)
   */
  getListingForReview: async (id) => {
    const response = await axiosInstance.get(`/cva/listing/${id}`);
    return response.data;
  },
};
