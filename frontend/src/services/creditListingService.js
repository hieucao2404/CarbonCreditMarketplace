// src/services/creditListingService.js
import axiosInstance from "../api/axiosInstance";

export const creditListingService = {
  /**
   * Create a new fixed-price listing
   * POST /api/listings
   * Request body: { creditId: UUID, price: number }
   */
  createListing: async (creditId, price) => {
    console.log("🔥 Creating listing:", { creditId, price });
    const response = await axiosInstance.post("/listings", {
      creditId,
      price,
    });
    console.log("✅ Listing created:", response.data);
    return response.data;
  },

  /**
   * Get all active listings (public)
   * GET /api/listings?page=0&size=10&sortBy=newest
   */
  getActiveListings: async (page = 0, size = 10, sortBy = "newest") => {
    const response = await axiosInstance.get("/listings", {
      params: { page, size, sortBy },
    });
    return response.data;
  },

  /**
   * Get user's own active listings
   * GET /api/listings/user/{userId}
   */
  getUserListings: async (userId, page = 0, size = 10) => {
    const response = await axiosInstance.get(`/listings/user/${userId}`, {
      params: { page, size },
    });
    return response.data;
  },

  /**
   * Get a single listing by ID
   * GET /api/listings/{id}
   */
  getListingById: async (id) => {
    const response = await axiosInstance.get(`/listings/${id}`);
    return response.data;
  },

  /**
   * Update listing price
   * PUT /api/listings/{id}/price
   * Request body: { newPrice: number }
   */
  updateListingPrice: async (listingId, newPrice) => {
    const response = await axiosInstance.put(`/listings/${listingId}/price`, {
      newPrice,
    });
    return response.data;
  },

  // Get ALL listings for the current user (authenticated)
  getMyListings: async (page = 0, size = 10) => {
    const response = await axiosInstance.get("/listings/my-listings", {
      params: { page, size },
    });
    return response.data;
  },

  /**
   * Cancel a listing
   * DELETE /api/listings/{id}
   */
  cancelListing: async (listingId) => {
    const response = await axiosInstance.delete(`/listings/${listingId}`);
    return response.data;
  },

  /**
   * Search listings by price range
   * GET /api/listings/search?minPrice=&maxPrice=
   */
  searchByPriceRange: async (minPrice, maxPrice, page = 0, size = 10) => {
    const response = await axiosInstance.get("/listings/search", {
      params: { minPrice, maxPrice, page, size },
    });
    return response.data;
  },

  /**
   * Get marketplace statistics
   * GET /api/listings/stats
   */
  getMarketplaceStats: async () => {
    const response = await axiosInstance.get("/listings/stats");
    return response.data;
  },
};
