import axiosInstance from '../api/axiosInstance';

const notificationService = {
  /**
   * Get all notifications for current user
   */
  getNotifications: async () => {
    try {
      const response = await axiosInstance.get('/notifications');
      return response.data;
    } catch (error) {
      console.error('Error fetching notifications:', error);
      throw error;
    }
  },

  /**
   * Get unread notifications only
   */
  getUnreadNotifications: async () => {
    try {
      const response = await axiosInstance.get('/notifications/unread');
      return response.data;
    } catch (error) {
      console.error('Error fetching unread notifications:', error);
      throw error;
    }
  },

  /**
   * Get unread count
   */
  getUnreadCount: async () => {
    try {
      const response = await axiosInstance.get('/notifications/unread/count');
      return response.data;
    } catch (error) {
      console.error('Error fetching unread count:', error);
      throw error;
    }
  },

  /**
   * Mark a notification as read
   */
  markAsRead: async (notificationId) => {
    try {
      const response = await axiosInstance.put(`/notifications/${notificationId}/read`);
      return response.data;
    } catch (error) {
      console.error('Error marking notification as read:', error);
      throw error;
    }
  },

  /**
   * Mark all notifications as read
   */
  markAllAsRead: async () => {
    try {
      const response = await axiosInstance.put('/notifications/read-all');
      return response.data;
    } catch (error) {
      console.error('Error marking all as read:', error);
      throw error;
    }
  },

  /**
   * Delete a notification
   */
  deleteNotification: async (notificationId) => {
    try {
      const response = await axiosInstance.delete(`/notifications/${notificationId}`);
      return response.data;
    } catch (error) {
      console.error('Error deleting notification:', error);
      throw error;
    }
  },

  /**
   * Delete all notifications
   */
  deleteAllNotifications: async () => {
    try {
      const response = await axiosInstance.delete('/notifications/all');
      return response.data;
    } catch (error) {
      console.error('Error deleting all notifications:', error);
      throw error;
    }
  }
};

export default notificationService;
