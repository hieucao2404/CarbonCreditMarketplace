import axiosInstance from "../api/axiosInstance";

export const journeyService = {
  //create new journey
  createJourney: async (journeyData) => {
    const response = await axiosInstance.post("/journeys", journeyData);
    return response.data;
  },

  //Update journey
  updateJourney: async (id, journeyData) => {
    const response = await axiosInstance.put(`/journeys/${id}`, journeyData);
    return response.data;
  },

  // Get all journeys for current user
  getMyJourneys: async () => {
    const response = await axiosInstance.get("/journeys/my-journeys");
    return response.data;
  },

  // Get journey by ID
  getJourneyById: async (id) => {
    const response = await axiosInstance.get(`/journeys/${id}`);
    return response.data;
  },

  // Delete journey
  deleteJourney: async (id) => {
    console.log("🔥 Calling DELETE /journeys/" + id);
    const response = await axiosInstance.delete(`/journeys/${id}`);
    console.log("🔥 Delete response:", response);
    return response.data;
  },
  //Admin: Get all journeys
  getAllJourneys: async () => {
    const response = await axiosInstance.get("/journeys/admin/all");
    return response.data;
  },

  // Get journey statistics
  getJourneyStatistics: async () => {
    const response = await axiosInstance.get("/journeys/statistics");
    return response.data;
  },

  //Admin get Journeys by status
  getJourneysByStatus: async (status) => {
    const response = await axiosInstance.get(
      `/journeys/admin/by-status/${status}`
    );
    return response.data;
  },
};
