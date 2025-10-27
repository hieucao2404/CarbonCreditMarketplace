import React from "react";

export default function BuyerHeader() {
  return (
    <header className="flex justify-between items-center bg-white border-b border-gray-200 px-8 py-4 shadow-sm">
      {/* Left section */}
      <div>
        <h2 className="font-semibold text-lg text-gray-800">
          Carbon Credit Exchange
        </h2>
        <span className="inline-block mt-1 text-sm bg-blue-100 text-blue-700 px-3 py-1 rounded-full">
          Người mua tín chỉ
        </span>
      </div>

      {/* Right section */}
      <div className="flex items-center gap-6">
        {/* Notification */}
        <button className="relative hover:text-blue-700 transition">
          <span className="text-xl">🔔</span>
          <span className="absolute -top-1 -right-2 bg-red-500 text-white text-xs rounded-full px-1.5">
            3
          </span>
        </button>

        {/* User info */}
        <div className="flex items-center gap-2">
          <div className="bg-gray-100 text-gray-700 font-semibold rounded-full w-9 h-9 flex items-center justify-center">
            CTA
          </div>
          <p className="font-medium text-gray-800">Công ty ABC</p>
        </div>
      </div>
    </header>
  );
}
