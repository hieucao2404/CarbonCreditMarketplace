import axiosInstance from "../api/axiosInstance";

export const vehicleService = {
    // Get all vehicles of current user
    getMyVehicles: async () => {
        const response = await axiosInstance.get("/vehicles/my-vehicles");
        return response.data;
    },
    //Get single vehicle by ID
    getVehicleById: async(id) => {
        const response = await axiosInstance.get(`/vehicles/${id}`);
        return response.data;
    },

    //Create new vehicle
    createVehicle: async(vehicleData) => {
        const response = await axiosInstance.post("/vehicles", vehicleData);
        return response.data;
    },

    //update vehicle
    updateVehicle: async(id, vehicleData) => {
        const response = await axiosInstance.put(`/vehicles/${id}`, vehicleData);
        return response.data;
    },

    //Delete vehicle
    deleteVehicle: async (id) => {
        const response = await axiosInstance.delete(`/vehicles/${id}`);
        return response.data;
    },

    //Admin: Get all vehicles
    getAllVehicles: async () => {
        const response = await axiosInstance.get("/vehicles/admin/all");
        return response.data;
    }

};