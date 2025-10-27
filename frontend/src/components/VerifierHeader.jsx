import React, { useEffect, useState, useRef } from "react";
import axios from "axios";
import { Bell } from "lucide-react";

export default function VerifierHeader() {
  const [user, setUser] = useState(null);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const menuRef = useRef(null);

  // Lấy dữ liệu người dùng
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
    <header className="bg-white border-b border-gray-200 flex items-center justify-between px-8 py-4 shadow-sm relative">
      {/* Left title */}
      <div>
        <h1 className="text-lg font-semibold text-gray-800">
          Carbon Credit Exchange
        </h1>
        <span className="inline-block mt-1 text-sm bg-purple-100 text-purple-700 px-3 py-1 rounded-full">
          Tổ chức kiểm toán
        </span>
      </div>

      {/* Right section */}
      <div className="flex items-center gap-6" ref={menuRef}>
        {/* Notification */}
        <button className="relative hover:text-purple-700 transition">
          <Bell className="w-5 h-5 text-gray-600 hover:text-purple-700 transition" />
          <span className="absolute -top-1.5 -right-1.5 bg-red-500 text-white text-xs w-4 h-4 flex items-center justify-center rounded-full">
            3
          </span>
        </button>

        {/* User Info + Dropdown */}
        <div className="relative">
          <button
            onClick={() => setIsMenuOpen(!isMenuOpen)}
            className="flex items-center gap-2 hover:bg-gray-100 px-2 py-1 rounded-lg transition"
          >
            <div className="bg-purple-100 text-purple-700 font-semibold rounded-full w-9 h-9 flex items-center justify-center">
              {user?.fullName ? user.fullName.charAt(0).toUpperCase() : "V"}
            </div>
            <div className="text-sm text-left">
              <p className="font-medium text-gray-800 leading-tight">
                {user?.fullName || "Tổ chức kiểm toán"}
              </p>
              <p className="text-xs text-gray-500">{user?.role || "VERIFIER"}</p>
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
