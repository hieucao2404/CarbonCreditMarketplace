import React, { useState } from "react";
import { Search, Plus, Trash2, Edit3 } from "lucide-react";
import AdminSidebar from "../components/AdminSidebar";
import AdminHeader from "../components/AdminHeader";

export default function AdminUserManagement() {
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState("Tất cả vai trò");

  const users = [
    { name: "Nguyễn Văn A", email: "nguyenvana@email.com", role: "Chủ xe điện", status: "Hoạt động", joinDate: "2024-01-15", info: "245.5 tCO₂ | 6,125,000 VND" },
    { name: "Công ty Green Tech", email: "contact@greentech.vn", role: "Người mua", status: "Hoạt động", joinDate: "2024-02-20", info: "1250 tCO₂ | 31,250,000 VND" },
    { name: "Tổ chức Kiểm toán Carbon", email: "audit@carbonverify.vn", role: "Kiểm toán viên", status: "Hoạt động", joinDate: "2024-01-10", info: "2847 đã duyệt | 5.2% từ chối" },
    { name: "Trần Thị B", email: "tranthib@email.com", role: "Chủ xe điện", status: "Đình chỉ", joinDate: "2024-03-05", info: "89.3 tCO₂ | 2,232,500 VND" },
  ];

  return (
    <div className="flex min-h-screen bg-gray-50">
      {/* Sidebar */}
      <AdminSidebar activePage="users" />

      {/* Phần nội dung */}
      <div className="flex-1 flex flex-col">
        <AdminHeader title="Quản lý người dùng" />

        <main className="flex-1 p-6">
          {/* Tiêu đề */}
          <div className="mb-6">
            <h1 className="text-xl font-semibold text-gray-800">Quản lý người dùng</h1>
            <p className="text-gray-500 text-sm">
              Xem và quản lý tất cả người dùng trên nền tảng
            </p>
          </div>

          {/* Thanh tìm kiếm + Lọc + Nút thêm */}
          <div className="flex items-center gap-3 mb-5">
            <div className="relative flex-1">
              <Search size={16} className="absolute left-3 top-2.5 text-gray-400" />
              <input
                type="text"
                placeholder="Tìm kiếm người dùng..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full border border-gray-200 rounded-lg pl-9 pr-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-200"
              />
            </div>

            <select
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value)}
              className="border border-gray-200 rounded-lg px-3 py-2 text-sm text-gray-700 focus:ring-2 focus:ring-green-200"
            >
              <option>Tất cả vai trò</option>
              <option>Chủ xe điện</option>
              <option>Người mua</option>
              <option>Kiểm toán viên</option>
            </select>

            <button className="flex items-center gap-2 bg-gray-900 text-white px-4 py-2 rounded-lg hover:bg-gray-800 transition">
              <Plus size={16} />
              <span className="text-sm font-medium">Thêm người dùng</span>
            </button>
          </div>

          {/* Danh sách người dùng */}
          <div className="space-y-3">
            {users.map((u, i) => (
              <div key={i} className="flex justify-between items-center border border-gray-200 rounded-lg px-4 py-3 bg-white hover:shadow-sm transition">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center">
                    <span className="text-gray-500 font-semibold text-sm">{u.name[0]}</span>
                  </div>
                  <div>
                    <h2 className="font-medium text-gray-800">{u.name}</h2>
                    <p className="text-gray-500 text-sm">{u.email}</p>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <span className="text-xs font-medium px-2 py-1 rounded-full bg-gray-100 text-gray-700">
                    {u.role}
                  </span>

                  <span
                    className={`text-xs font-medium px-2 py-1 rounded-full ${
                      u.status === "Hoạt động"
                        ? "bg-green-100 text-green-700"
                        : "bg-red-100 text-red-700"
                    }`}
                  >
                    {u.status}
                  </span>

                  <div className="text-right text-sm text-gray-600">
                    <p className="text-xs">Tham gia: {u.joinDate}</p>
                    <p className="text-xs">{u.info}</p>
                  </div>

                  <div className="flex gap-2 ml-3">
                    <button className="p-1.5 rounded-lg hover:bg-gray-100">
                      <Edit3 size={16} className="text-gray-600" />
                    </button>
                    <button className="p-1.5 rounded-lg hover:bg-gray-100">
                      <Trash2 size={16} className="text-gray-600" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </main>
      </div>
    </div>
  );
}
