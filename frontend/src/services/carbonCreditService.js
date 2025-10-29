import axiosInstance from "../api/axiosInstance";

export const carbonCreditService = {
    //get my carbon credits
    getMyCredits: async (page = 0, size = 10) => {
        const response = await axiosInstance.get("/credits/my-credits", {
            params:{page, size},
        });
        return response.data;
    },

    //get credit by ID
    getMyCreditById: async (id) => {
        const response = await axiosInstance.get(`/credits/${id}`);
        return response.data;
    },

    //Admin: get all credits
    getAllCredits: async (page = 0, size = 10) => {
        const response = await axiosInstance.get("/credits/admin/all",{
            params: {page, size},
        });
        return response.data;
    },

    //Admin: Get credits by status
    getCreditsByStatus: async (status, page = 0, size = 10) => {
        const response = await axiosInstance.get("/credits/admin/by-status", {
            params: {status, page, size},
        });
        return response.data;
    }
}