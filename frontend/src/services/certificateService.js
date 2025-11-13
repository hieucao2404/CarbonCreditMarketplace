// src/services/certificateService.js
import axiosInstance from "../api/axiosInstance";

/**
 * Service for fetching and downloading buyer certificates.
 * This connects to the CertificateController on the backend.
 */
export const certificateService = {
  /**
   * Get all certificates for the logged-in buyer
   * Calls: GET /api/certificates/my-certificates
   */
  getMyCertificates: async () => {
    // We assume the backend DTO is named CertificateDTO
    // and it matches the fields we need.
    const response = await axiosInstance.get("/certificates/my-certificates");
    return response.data;
  },

  /**
   * Download a single certificate PDF
   * Calls: GET /api/certificates/{id}/download
   */
  downloadCertificate: async (certificateId) => {
    const response = await axiosInstance.get(
      `/certificates/${certificateId}/download`,
      {
        responseType: 'blob', // IMPORTANT: Tell Axios to expect a file
      }
    );
    return response.data; // This will be the binary PDF data
  },
};