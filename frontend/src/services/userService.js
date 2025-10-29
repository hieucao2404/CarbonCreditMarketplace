import axiosInstance from "../api/axiosInstance";

export const userService = {
  //Get current user profile
  getCurrentUser: async () => {
    const response = await axiosInstance.get("/user/me");
    return response.data;
  },

  //Get user by ID
  getUserById: async (id) => {
    const response = await axiosInstance.get(`/users/${id}`);
    return response.data;
  },

  //Update user by username
  getUserByUsername: async (username) => {
    const response = await axiosInstance.get(`/users/username/${username}`);
    return response.data;
  },

  //Admin: get all users
  getAllUsers: async () => {
    const response = await axiosInstance.get("/users");
    return response.data;
  },

  //Admin: get users by role
  getUsersByRole: async (role) => {
    const response = await axiosInstance.get(`/users/roles/${role}`);
    return response.data;
  }, 

  //Admin: Delete user
  deleteUser: async (id) => {
    const response = await axiosInstance.delete(`/users/${id}`);
    return response.data;
  }
};
