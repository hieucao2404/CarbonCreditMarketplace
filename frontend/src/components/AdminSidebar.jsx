import React from "react";
import { useNavigate, useLocation } from "react-router-dom";
import {
  Home,
  Users,
  FileText,
  Settings,
  BarChart3,
  Leaf,
} from "lucide-react";

export default function AdminSidebar() {
  const navigate = useNavigate();
  const location = useLocation();

  const menu = [
    { icon: <Home size={18} />, text: "Tổng quan", path: "/admin" },
    { icon: <Users size={18} />, text: "Quản lý người dùng", path: "/admin/users" },
    { icon: <FileText size={18} />, text: "Quản lý giao dịch", path: "/admin/transactions" },
    { icon: <BarChart3 size={18} />, text: "Thống kê", path: "/admin/stats" },
    { icon: <Settings size={18} />, text: "Cài đặt hệ thống", path: "/admin/settings" },
  ];

  return (
    <aside className="w-64 bg-white border-r border-gray-200 p-4 shadow-sm h-screen">
      {/* Logo */}
      <div className="flex items-center gap-2 mb-6">
        <div className="bg-green-100 p-2 rounded-full">
          <Leaf size={20} className="text-green-600" />
        </div>
        <div>
          <h1 className="font-semibold text-lg text-gray-800">Carbon</h1>
          <p className="text-sm text-gray-500 -mt-1">Exchange</p>
        </div>
      </div>

      {/* Menu */}
      <nav className="space-y-1">
        {menu.map((item, i) => {
          const isActive = location.pathname === item.path;
          return (
            <button
              key={i}
              onClick={() => navigate(item.path)}
              className={`relative flex items-center gap-2 w-full px-3 py-2 rounded-lg text-left font-medium transition-all duration-300 group
              ${
                isActive
                  ? "bg-green-50 text-green-700 font-semibold"
                  : "text-gray-700 hover:bg-green-50 hover:text-green-700"
              }`}
            >
              {/* Thanh highlight trái */}
              <span
                className={`absolute left-0 top-0 h-full w-1 bg-green-600 rounded-r-sm transition-all duration-300
                ${isActive ? "opacity-100 translate-x-0" : "opacity-0 -translate-x-2"}`}
              ></span>

              {item.icon}
              <span className="text-sm">{item.text}</span>
            </button>
          );
        })}
      </nav>
    </aside>
  );
}
