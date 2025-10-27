import React, { useEffect, useState, useRef } from "react";
import axios from "axios";

export default function AdminHeader() {
  const [user, setUser] = useState(null);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const menuRef = useRef(null);

  // Lấy dữ liệu người dùng từ localStorage hoặc API
  useEffect(() => {
    const storedUser = localStorage.getItem("user");
    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser));
      } catch (error) {
        console.error("Lỗi parse user từ localStorage:", error);
      }
    } else {
      axios
        .get("http://localhost:8080/api/users/me")
        .then((res) => setUser(res.data))
        .catch((err) => console.error("Lỗi lấy thông tin người dùng:", err));
    }
  }, []);

  // Đóng dropdown khi click ra ngoài
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) {
        setIsMenuOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Đăng xuất
  const handleLogout = () => {
    localStorage.removeItem("user");
    window.location.href = "/login";
  };

  return (
    <header className="flex justify-between items-center bg-white border-b border-gray-200 px-8 py-4 shadow-sm relative">
      {/* Left */}
      <div>
        <h2 className="font-semibold text-lg text-gray-800">
          Carbon Credit Exchange
        </h2>
        <span className="inline-block mt-1 text-sm bg-orange-100 text-orange-700 px-3 py-1 rounded-full">
          Quản trị viên
        </span>
      </div>

      {/* Right */}
      <div className="flex items-center gap-6" ref={menuRef}>
        {/* Notification */}
        <button className="relative hover:text-orange-700 transition">
          🔔
          <span className="absolute -top-1 -right-2 bg-red-500 text-white text-xs rounded-full px-1.5">
            3
          </span>
        </button>

        {/* User Info + Dropdown */}
        <div className="relative">
          <button
            onClick={() => setIsMenuOpen(!isMenuOpen)}
            className="flex items-center gap-2 hover:bg-gray-100 px-2 py-1 rounded-lg transition"
          >
            <div className="bg-orange-100 text-orange-700 font-semibold rounded-full w-9 h-9 flex items-center justify-center">
              {user?.fullName ? user.fullName.charAt(0).toUpperCase() : "A"}
            </div>
            <div className="text-sm text-left">
              <p className="font-medium text-gray-800 leading-tight">
                {user?.fullName || "Quản trị viên"}
              </p>
              <p className="text-xs text-gray-500">{user?.role || "ADMIN"}</p>
            </div>
            <span className="text-gray-400 ml-1">▾</span>
          </button>

          {/* Dropdown menu */}
          {isMenuOpen && (
            <div className="absolute right-0 mt-2 w-48 bg-white border border-gray-200 rounded-xl shadow-lg z-50 animate-fadeIn">
              <button
                onClick={() => alert("Chuyển đến trang thông tin cá nhân!")}
                className="w-full text-left px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-t-xl"
              >
                Thông tin cá nhân
              </button>
              <button
                onClick={handleLogout}
                className="w-full text-left px-4 py-2 text-red-600 hover:bg-red-50 rounded-b-xl"
              >
                Đăng xuất
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
