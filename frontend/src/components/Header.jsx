import React, { useEffect, useState } from "react";

export default function Header() {
  const [user, setUser] = useState(null);

  useEffect(() => {
    // Lấy dữ liệu user từ localStorage
    const storedUser = localStorage.getItem("user");
    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser));
      } catch (e) {
        console.error("Lỗi khi parse user từ localStorage:", e);
      }
    }
  }, []);

  return (
    <header className="flex justify-between items-center bg-white border-b border-gray-200 px-8 py-4 shadow-sm">
      {/* Left */}
      <div>
        <h2 className="font-semibold text-lg text-gray-800">
          Carbon Credit Exchange
        </h2>
        <span className="inline-block mt-1 text-sm bg-green-100 text-green-700 px-3 py-1 rounded-full">
          Chủ sở hữu xe điện
        </span>
      </div>

      {/* Right */}
      <div className="flex items-center gap-6">
        {/* Notification */}
        <button className="relative hover:text-green-700 transition">
          🔔
          <span className="absolute -top-1 -right-2 bg-red-500 text-white text-xs rounded-full px-1.5">
            3
          </span>
        </button>

        {/* User Info */}
        <div className="flex items-center gap-2">
          <div className="bg-green-100 text-green-700 font-semibold rounded-full w-9 h-9 flex items-center justify-center">
            {user?.fullName ? user.fullName.charAt(0).toUpperCase() : "?"}
          </div>
          <div className="text-sm">
            <p className="font-medium text-gray-800 leading-tight">
              {user?.fullName || "Đang tải..."}
            </p>
            <p className="text-xs text-gray-500">{user?.role || "..."}</p>
          </div>
        </div>
      </div>
    </header>
  );
}
