import { useState, useEffect, useCallback } from 'react';
import { notificationService } from '../services';

/**
 * Custom hook for managing notifications
 * Fetches notifications from API and provides methods to interact with them
 */
export const useNotifications = (autoRefresh = true, refreshInterval = 30000) => {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Fetch notifications from API
  const fetchNotifications = useCallback(async () => {
    try {
      setLoading(true);
      const response = await notificationService.getNotifications();
      
      if (response.success && response.data) {
        // Transform API response to match component format
        const transformedNotifications = response.data.map(notif => ({
          id: notif.id,
          message: notif.message,
          read: notif.read,
          time: notif.time,
          type: notif.type,
          title: notif.title,
          relatedEntityId: notif.relatedEntityId,
          relatedEntityType: notif.relatedEntityType
        }));
        
        setNotifications(transformedNotifications);
        setError(null);
      }
    } catch (err) {
      console.error('Error fetching notifications:', err);
      setError(err.message);
      // Keep existing notifications on error
    } finally {
      setLoading(false);
    }
  }, []);

  // Initial fetch
  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  // Auto-refresh
  useEffect(() => {
    if (!autoRefresh) return;

    const interval = setInterval(() => {
      fetchNotifications();
    }, refreshInterval);

    return () => clearInterval(interval);
  }, [autoRefresh, refreshInterval, fetchNotifications]);

  // Mark notification as read
  const markAsRead = async (notificationId) => {
    try {
      await notificationService.markAsRead(notificationId);
      setNotifications(prev =>
        prev.map(n => (n.id === notificationId ? { ...n, read: true } : n))
      );
    } catch (err) {
      console.error('Error marking notification as read:', err);
    }
  };

  // Mark all as read
  const markAllAsRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
    } catch (err) {
      console.error('Error marking all as read:', err);
    }
  };

  // Delete notification
  const deleteNotification = async (notificationId) => {
    try {
      await notificationService.deleteNotification(notificationId);
      setNotifications(prev => prev.filter(n => n.id !== notificationId));
    } catch (err) {
      console.error('Error deleting notification:', err);
    }
  };

  // Delete all notifications
  const deleteAllNotifications = async () => {
    try {
      await notificationService.deleteAllNotifications();
      setNotifications([]);
    } catch (err) {
      console.error('Error deleting all notifications:', err);
    }
  };

  return {
    notifications,
    loading,
    error,
    fetchNotifications,
    markAsRead,
    markAllAsRead,
    deleteNotification,
    deleteAllNotifications,
  };
};
