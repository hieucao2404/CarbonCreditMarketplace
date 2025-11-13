import React, { useEffect, useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import {
  LogOut,
  Settings,
  User,
  ChevronDown,
  Bell,
  Trash2,
} from "lucide-react";

export default function VerifierHeader() {
  const [user, setUser] = useState(null);
  const [showDropdown, setShowDropdown] = useState(false);
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);
  const [notifications, setNotifications] = useState([
    { id: 1, message: "✅ Xác minh hoàn tất: Bạn đã phê duyệt 250 tCO₂ từ hành trình #JD-2024-0851 của EV Owner567.", read: false, time: "15 phút trước" },
    { id: 2, message: "📋 Lịch kiểm tra mới: EV Owner789 đã đặt lịch kiểm tra vào ngày 15/11 lúc 14:00 tại Hà Nội.", read: false, time: "1 giờ trước" },
    { id: 3, message: "❌ Hành trình bị từ chối: Dữ liệu hành trình #JD-2024-0840 không hợp lệ do không có GPS track.", read: false, time: "2 giờ trước" },
    { id: 4, message: "📊 Báo cáo xác minh: Bạn đã xác minh thành công 1.250 tCO₂ trong tuần này.", read: true, time: "Hôm qua" },
    { id: 5, message: "🎯 Nhiệm vụ: Có 8 hành trình chờ xác minh từ lần cuối cùng bạn đăng nhập.", read: true, time: "2 ngày trước" },
  ]);

  const navigate = useNavigate();
  const dropdownRef = useRef(null);
  const notifRef = useRef(null);

  useEffect(() => {
    const storedUser = localStorage.getItem("user");
    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser));
      } catch (e) {
        console.error("Error parsing user from localStorage:", e);
      }
    }
  }, []);

  // Đóng dropdown & thông báo khi click ra ngoài
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setShowDropdown(false);
      }
      if (notifRef.current && !notifRef.current.contains(event.target)) {
        setShowNotifications(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    navigate("/login", { replace: true });
  };

  const handleDeleteNotification = (id) => {
    setNotifications((prev) => prev.filter((n) => n.id !== id));
  };

  const handleDeleteAll = () => {
    setNotifications([]);
  };

  return (
    <header className="flex justify-between items-center bg-white border-b border-gray-200 px-8 py-4 shadow-sm relative">
      {/* Left - Title */}
      <div>
        <h2 className="font-semibold text-lg text-gray-800">
          Carbon Credit Exchange
        </h2>
        <span className="inline-block mt-1 text-sm bg-purple-100 text-purple-700 px-3 py-1 rounded-full">
          Verification Authority
        </span>
      </div>

      {/* Right */}
      <div className="flex items-center gap-6">
        {/* 🔔 Notification */}
        <div className="relative" ref={notifRef}>
          <button
            onClick={() => setShowNotifications(!showNotifications)}
            className="relative hover:text-purple-700 transition flex items-center justify-center"
          >
            <Bell
              className={`w-6 h-6 text-gray-700 transition ${
                notifications.some((n) => !n.read)
                  ? "animate-pulse text-purple-600"
                  : ""
              }`}
            />
            {notifications.some((n) => !n.read) && (
              <span className="absolute -top-1 -right-1 bg-red-500 text-white text-[10px] font-semibold rounded-full w-4 h-4 flex items-center justify-center">
                {notifications.filter((n) => !n.read).length}
              </span>
            )}
          </button>

          {/* Notification Dropdown */}
          {showNotifications && (
            <div className="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-lg border border-gray-200 py-2 z-50 animate-fadeSlideIn">
              {/* Header */}
              <div className="px-4 py-2 border-b border-gray-200 flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <Bell className="w-4 h-4 text-purple-600" />
                  <span className="font-semibold text-gray-800">Thông báo</span>
                </div>
                <div className="flex gap-3">
                  <button
                    onClick={() =>
                      setNotifications((prev) =>
                        prev.map((n) => ({ ...n, read: true }))
                      )
                    }
                    className="text-xs text-purple-600 hover:text-purple-700 hover:underline transition"
                  >
                    Đánh dấu đã đọc
                  </button>
                  <button
                    onClick={handleDeleteAll}
                    className="text-xs text-red-500 hover:text-red-600 hover:underline transition"
                  >
                    Xóa tất cả
                  </button>
                </div>
              </div>

              {/* Danh sách thông báo */}
              <div className="max-h-64 overflow-y-auto">
                {notifications.length === 0 ? (
                  <p className="text-sm text-gray-500 text-center py-4">
                    Không có thông báo mới
                  </p>
                ) : (
                  notifications.map((n) => (
                    <div
                      key={n.id}
                      className={`relative group px-4 py-3 text-sm cursor-pointer transition-all duration-150 border-b border-gray-100 last:border-none ${
                        !n.read ? "bg-purple-50" : "bg-white"
                      } hover:bg-purple-50`}
                      onClick={() =>
                        setNotifications((prev) =>
                          prev.map((item) =>
                            item.id === n.id ? { ...item, read: true } : item
                          )
                        )
                      }
                    >
                      <div className="flex items-start gap-2">
                        {!n.read && (
                          <span className="mt-1 w-2 h-2 bg-purple-500 rounded-full flex-shrink-0"></span>
                        )}
                        <div>
                          <p className="text-gray-700 pr-6">{n.message}</p>
                          <p className="text-xs text-gray-400 mt-1">{n.time}</p>
                        </div>
                      </div>

                      {/* Nút xóa */}
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeleteNotification(n.id);
                        }}
                        className="absolute top-2 right-3 opacity-0 group-hover:opacity-100 text-gray-400 hover:text-red-500 hover:bg-red-50 p-1 rounded transition-all"
                        title="Xóa thông báo"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  ))
                )}
              </div>

              {/* Footer */}
              <div className="border-t border-gray-200 text-center">
                <button
                  onClick={() => {
                    // navigate("/notifications");
                    setShowNotifications(false);
                  }}
                  className="w-full text-sm text-purple-700 py-2 hover:bg-purple-50 transition"
                >
                  Xem tất cả
                </button>
              </div>
            </div>
          )}
        </div>

        {/* 👤 User Dropdown */}
        <div className="relative" ref={dropdownRef}>
          <button
            onClick={() => setShowDropdown(!showDropdown)}
            className="flex items-center gap-2 hover:bg-gray-50 rounded-lg px-3 py-2 transition"
          >
            <div className="bg-purple-100 text-purple-700 font-semibold rounded-full w-9 h-9 flex items-center justify-center">
              {user?.fullName ? user.fullName.charAt(0).toUpperCase() : "V"}
            </div>
            <div className="text-sm text-left">
              <p className="font-medium text-gray-800 leading-tight">
                {user?.fullName || "Verification Authority"}
              </p>
              <p className="text-xs text-gray-500">{user?.role || "Verifier"}</p>
            </div>
            <ChevronDown className="w-4 h-4 text-gray-600" />
          </button>

          {/* Dropdown Menu */}
          {showDropdown && (
            <div className="absolute right-0 mt-2 w-56 bg-white rounded-lg shadow-lg border border-gray-200 py-2 z-50 animate-fadeSlideIn">
              <div className="px-4 py-3 border-b border-gray-200">
                <p className="text-sm font-medium text-gray-800">
                  {user?.fullName || "Verification Authority"}
                </p>
                <p className="text-xs text-gray-500">
                  {user?.email || "verifier@example.com"}
                </p>
              </div>

              <button
                onClick={() => {
                  // navigate("/verifier/profile");
                  setShowDropdown(false);
                }}
                className="w-full flex items-center gap-3 px-4 py-2 hover:bg-gray-50 text-left"
              >
                <User className="w-4 h-4 text-gray-600" />
                <span className="text-sm text-gray-700">My Profile</span>
              </button>

              <button
                onClick={() => setShowDropdown(false)}
                className="w-full flex items-center gap-3 px-4 py-2 hover:bg-gray-50 text-left"
              >
                <Settings className="w-4 h-4 text-gray-600" />
                <span className="text-sm text-gray-700">Settings</span>
              </button>

              <div className="border-t border-gray-200 mt-2 pt-2">
                <button
                  onClick={() => setShowLogoutConfirm(true)}
                  className="w-full flex items-center gap-3 px-4 py-2 hover:bg-red-50 text-left text-red-600"
                >
                  <LogOut className="w-4 h-4" />
                  <span className="text-sm font-medium">Logout</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ✅ Popup xác nhận Logout */}
      {showLogoutConfirm && (
        <div className="fixed inset-0 flex items-center justify-center bg-black/40 z-50">
          <div className="bg-white rounded-2xl shadow-lg p-6 w-80 text-center animate-fadeSlideIn">
            <h2 className="text-lg font-semibold mb-3 text-gray-800">Đăng xuất</h2>
            <p className="text-gray-600 mb-5">
              Bạn có chắc chắn muốn đăng xuất?
            </p>
            <div className="flex justify-center gap-3">
              <button
                onClick={() => setShowLogoutConfirm(false)}
                className="px-4 py-2 rounded-lg bg-gray-200 hover:bg-gray-300 transition"
              >
                Hủy
              </button>
              <button
                onClick={handleLogout}
                className="px-4 py-2 rounded-lg bg-red-500 text-white hover:bg-red-600 transition"
              >
                Đăng xuất
              </button>
            </div>
          </div>
        </div>
      )}
    </header>
  );
}

/* 💫 Animation */
const style = document.createElement("style");
style.innerHTML = `
@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}
.animate-fadeSlideIn {
  animation: fadeSlideIn 0.25s ease-out;
}
`;
document.head.appendChild(style);
