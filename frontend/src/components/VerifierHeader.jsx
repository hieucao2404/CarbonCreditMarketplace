// src/components/VerifierHeader.jsx
import React, { useEffect, useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { Bell, LogOut, Settings, User, ChevronDown, FileCheck } from "lucide-react";

export default function VerifierHeader() {
  const [user, setUser] = useState(null);
  const [showDropdown, setShowDropdown] = useState(false);
  const navigate = useNavigate();
  const dropdownRef = useRef(null);

  useEffect(() => {
    const storedUser = localStorage.getItem("user");
    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser));
      } catch (e) {
        console.error("Error parsing user:", e);
      }
    }
  }, []);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleLogout = () => {
    if (window.confirm("Are you sure you want to logout?")) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      navigate("/login", { replace: true });
    }
  };

  return (
    <header className="bg-white border-b border-gray-200 flex items-center justify-between px-8 py-4 shadow-sm">
      {/* Left title */}
      <div>
        <h1 className="text-lg font-semibold text-gray-800">
          Carbon Credit Exchange
        </h1>
        <span className="inline-block mt-1 text-sm bg-purple-100 text-purple-700 px-3 py-1 rounded-full">
          Verification Authority
        </span>
      </div>

      {/* Right section */}
      <div className="flex items-center gap-6">
        {/* Notification icon */}
        <button className="relative cursor-pointer">
          <Bell className="w-5 h-5 text-gray-600 hover:text-gray-800 transition" />
          <span className="absolute -top-1.5 -right-1.5 bg-red-500 text-white text-xs w-4 h-4 flex items-center justify-center rounded-full">
            3
          </span>
        </button>

        {/* User Dropdown */}
        <div className="relative" ref={dropdownRef}>
          <button
            onClick={() => setShowDropdown(!showDropdown)}
            className="flex items-center gap-2 bg-gray-50 px-3 py-1.5 rounded-full border border-gray-200 hover:bg-gray-100 transition"
          >
            <div className="w-8 h-8 bg-purple-100 rounded-full flex items-center justify-center text-sm font-medium text-purple-700">
              {user?.fullName ? user.fullName.substring(0, 2).toUpperCase() : "VA"}
            </div>
            <span className="text-sm text-gray-800 font-medium">
              {user?.fullName || "Verification Authority"}
            </span>
            <ChevronDown className="w-4 h-4 text-gray-600" />
          </button>

          {/* Dropdown Menu */}
          {showDropdown && (
            <div className="absolute right-0 mt-2 w-60 bg-white rounded-lg shadow-lg border border-gray-200 py-2 z-50">
              <div className="px-4 py-3 border-b border-gray-200">
                <p className="text-sm font-medium text-gray-800">
                  {user?.fullName || "Verification Authority"}
                </p>
                <p className="text-xs text-gray-500">{user?.email || "verifier@example.com"}</p>
              </div>

              <button
                onClick={() => {
                  navigate("/verifier/profile");
                  setShowDropdown(false);
                }}
                className="w-full flex items-center gap-3 px-4 py-2 hover:bg-gray-50 transition text-left"
              >
                <User className="w-4 h-4 text-gray-600" />
                <span className="text-sm text-gray-700">My Profile</span>
              </button>

              <button
                onClick={() => {
                  navigate("/verifier/reports");
                  setShowDropdown(false);
                }}
                className="w-full flex items-center gap-3 px-4 py-2 hover:bg-gray-50 transition text-left"
              >
                <FileCheck className="w-4 h-4 text-gray-600" />
                <span className="text-sm text-gray-700">My Verifications</span>
              </button>

              <button
                onClick={() => {
                  navigate("/verifier/settings");
                  setShowDropdown(false);
                }}
                className="w-full flex items-center gap-3 px-4 py-2 hover:bg-gray-50 transition text-left"
              >
                <Settings className="w-4 h-4 text-gray-600" />
                <span className="text-sm text-gray-700">Settings</span>
              </button>

              <div className="border-t border-gray-200 mt-2 pt-2">
                <button
                  onClick={handleLogout}
                  className="w-full flex items-center gap-3 px-4 py-2 hover:bg-red-50 transition text-left text-red-600"
                >
                  <LogOut className="w-4 h-4" />
                  <span className="text-sm font-medium">Logout</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
