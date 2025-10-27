import React from "react";
import { Bell } from "lucide-react";

export default function VerifierHeader() {
  return (
    <header className="bg-white border-b border-gray-200 flex items-center justify-between px-8 py-4 shadow-sm">
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
      <div className="flex items-center gap-6">
        {/* Notification icon */}
        <div className="relative cursor-pointer">
          <Bell className="w-5 h-5 text-gray-600 hover:text-gray-800 transition" />
          <span className="absolute -top-1.5 -right-1.5 bg-red-500 text-white text-xs w-4 h-4 flex items-center justify-center rounded-full">
            3
          </span>
        </div>

        {/* User info */}
        <div className="flex items-center gap-2 bg-gray-50 px-3 py-1.5 rounded-full border border-gray-200">
          <div className="w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center text-sm font-medium text-gray-600">
            CKT
          </div>
          <span className="text-sm text-gray-800 font-medium">
            Tổ chức Kiểm toán XYZ
          </span>
        </div>
      </div>
    </header>
  );
}
