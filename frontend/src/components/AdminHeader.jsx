import React from "react";

export default function AdminHeader() {
  return (
    <header className="flex justify-between items-center bg-white border-b border-gray-200 px-8 py-4 shadow-sm">
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
      <div className="flex items-center gap-6">
        <button className="relative hover:text-orange-700 transition">
          🔔
          <span className="absolute -top-1 -right-2 bg-red-500 text-white text-xs rounded-full px-1.5">
            3
          </span>
        </button>

        <div className="flex items-center gap-2">
          <div className="bg-orange-100 text-orange-700 font-semibold rounded-full w-9 h-9 flex items-center justify-center">
            Q
          </div>
          <div className="text-sm">
            <p className="font-medium text-gray-800 leading-tight">Quản trị viên</p>
            <p className="text-xs text-gray-500">Admin</p>
          </div>
        </div>
      </div>
    </header>
  );
}
