import apiClient from "./axiosInstance";

export const login = async (role, name, email) => {
  return apiClient.post("/auth/login", { role, name, email });
};