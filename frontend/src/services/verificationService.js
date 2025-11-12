// src/services/verificationService.js
import axiosInstance from "../api/axiosInstance";

/**
 * Service for handling the physical verification and
 * appointment scheduling process.
 */
export const verificationService = {
  /**
   * (CVA) Request a physical inspection for a journey
   * Calls: POST /api/verification/journey/{journeyId}/request-inspection
   */
  requestInspection: (journeyId) => {
    return axiosInstance.post(`/verification/journey/${journeyId}/request-inspection`);
  },

  /**
   * (EV_OWNER) Get all active verification stations
   * Calls: GET /api/verification/stations
   */
  getActiveStations: () => {
    return axiosInstance.get("/verification/stations");
  },

  /**
   * (EV_OWNER) Schedule an appointment at a station
   * Calls: POST /api/verification/schedule
   */
  scheduleAppointment: (appointmentId, stationId, appointmentTime) => {
    return axiosInstance.post("/verification/schedule", {
      appointmentId,
      stationId,
      appointmentTime,
    });
  },

  /**
   * (CVA) CVA marks the inspection as complete (Approve/Reject).
   * This is called from a *different* page, after the CVA has
   * completed the physical inspection.
   * Calls: POST /api/verification/appointment/{appointmentId}/complete
   */
  completeInspection: (appointmentId, isApproved, notes) => {
     return axiosInstance.post(`/verification/appointment/${appointmentId}/complete`, {
      isApproved,
      notes
    });
  }
};