// src/services/authService.js
import axiosInstance from "../api/axiosInstance";

export const authService = {
  // Login
  login: async (username, password) => {
    const response = await axiosInstance.post("/users/login", {
      username,
      password,
    });
    return response.data;
  },

  // Register
  register: async (userData) => {
    const response = await axiosInstance.post("/users/register", userData);
    return response.data;
  },

  // Logout
  logout: () => {
    localStorage.removeItem("token");
    window.location.href = "/login";
  },
};
