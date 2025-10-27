import axios from "axios";

const apiClient = axios.create({
  baseURL: "http://localhost:8080/api", // Spring Boot backend
  headers: { "Content-Type": "application/json" },
});

export default apiClient;