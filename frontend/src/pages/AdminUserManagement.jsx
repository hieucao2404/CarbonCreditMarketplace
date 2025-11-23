import React, { useState, useEffect } from "react";
import { Search, Plus, Trash2, Edit3, RefreshCw, AlertCircle, UserX } from "lucide-react";
import AdminSidebar from "../components/AdminSidebar";
import AdminHeader from "../components/AdminHeader";
import axiosInstance from "../api/axiosInstance";

export default function AdminUserManagement() {
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState("ALL");
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [deleteConfirm, setDeleteConfirm] = useState(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [showDetailModal, setShowDetailModal] = useState(null);
  const [addLoading, setAddLoading] = useState(false);
  const [updateLoading, setUpdateLoading] = useState(false);
  const [vehiclesLoading, setVehiclesLoading] = useState(false);
  const [userVehicles, setUserVehicles] = useState([]);
  const [addFormData, setAddFormData] = useState({
    username: "",
    email: "",
    password: "",
    fullName: "",
    phone: "",
    role: "CVA",
  });
  const [addError, setAddError] = useState("");
  const [editFormData, setEditFormData] = useState({
    email: "",
    fullName: "",
    phone: "",
  });
  const [editError, setEditError] = useState("");

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    setLoading(true);
    setError("");

    try {
      const response = await axiosInstance.get("/users");
      console.log("👥 Users Response:", response.data);

      if (response.data?.data) {
        setUsers(response.data.data);
      }
    } catch (e) {
      console.error("❌ Error loading users:", e.response?.data || e.message);
      setError(e.response?.data?.message || "Không thể tải danh sách người dùng.");
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteUser = async (userId, username) => {
    try {
      await axiosInstance.delete(`/users/${userId}`);
      alert(`Đã xóa người dùng ${username} thành công!`);
      setDeleteConfirm(null);
      loadUsers(); // Reload the list
    } catch (e) {
      console.error("❌ Error deleting user:", e.response?.data || e.message);
      alert(e.response?.data?.message || "Không thể xóa người dùng.");
    }
  };

  const handleAddFormChange = (e) => {
    const { name, value } = e.target;
    setAddFormData({ ...addFormData, [name]: value });
  };

  const validateAddForm = () => {
    const { username, email, password, fullName, phone, role } = addFormData;

    if (!username.trim()) {
      setAddError("Vui lòng nhập tên đăng nhập");
      return false;
    }

    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
      setAddError("Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới");
      return false;
    }

    if (!email.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      setAddError("Vui lòng nhập email hợp lệ");
      return false;
    }

    if (!password.trim() || password.length < 8) {
      setAddError("Mật khẩu phải có ít nhất 8 ký tự");
      return false;
    }

    if (!fullName.trim()) {
      setAddError("Vui lòng nhập họ và tên");
      return false;
    }

    if (!phone.trim() || !/^\d{10,}$/.test(phone)) {
      setAddError("Số điện thoại phải có ít nhất 10 chữ số");
      return false;
    }

    if (!role) {
      setAddError("Vui lòng chọn vai trò");
      return false;
    }

    return true;
  };

  const handleAddUser = async (e) => {
    e.preventDefault();
    setAddError("");

    if (!validateAddForm()) {
      return;
    }

    setAddLoading(true);

    try {
      const response = await axiosInstance.post("/users/register", addFormData);
      console.log("✅ User created:", response.data);

      alert(`Tạo người dùng ${addFormData.username} thành công!`);
      setShowAddModal(false);
      setAddFormData({
        username: "",
        email: "",
        password: "",
        fullName: "",
        phone: "",
        role: "CVA",
      });
      loadUsers(); // Reload the list
    } catch (e) {
      console.error("❌ Error creating user:", e.response?.data || e.message);
      setAddError(
        e.response?.data?.message || "Không thể tạo người dùng. Vui lòng thử lại."
      );
    } finally {
      setAddLoading(false);
    }
  };

  const handleOpenDetail = (user) => {
    setShowDetailModal(user);
    setEditFormData({
      email: user.email || "",
      fullName: user.fullName || "",
      phone: user.phone || "",
    });
    setEditError("");
    setUserVehicles([]);

    // Load vehicles if user is EV_OWNER
    if (user.role === "EV_OWNER") {
      loadUserVehicles(user.id);
    }
  };

  const loadUserVehicles = async (userId) => {
    setVehiclesLoading(true);
    try {
      const response = await axiosInstance.get(`/vehicles/admin/user/${userId}`);
      console.log("🚗 User Vehicles:", response.data);

      if (response.data?.data) {
        setUserVehicles(response.data.data);
      }
    } catch (e) {
      console.error("❌ Error loading vehicles:", e.response?.data || e.message);
      setUserVehicles([]);
    } finally {
      setVehiclesLoading(false);
    }
  };

  const handleEditFormChange = (e) => {
    const { name, value } = e.target;
    setEditFormData({ ...editFormData, [name]: value });
  };

  const validateEditForm = () => {
    const { email, fullName, phone } = editFormData;

    if (!email.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      setEditError("Vui lòng nhập email hợp lệ");
      return false;
    }

    if (!fullName.trim()) {
      setEditError("Vui lòng nhập họ và tên");
      return false;
    }

    if (!phone.trim() || !/^\d{10,}$/.test(phone)) {
      setEditError("Số điện thoại phải có ít nhất 10 chữ số");
      return false;
    }

    return true;
  };

  const handleUpdateUser = async (e) => {
    e.preventDefault();
    setEditError("");

    if (!validateEditForm() || !showDetailModal) {
      return;
    }

    setUpdateLoading(true);

    try {
      const response = await axiosInstance.put(
        `/users/${showDetailModal.id}`,
        editFormData
      );
      console.log("✅ User updated:", response.data);

      alert(`Cập nhật người dùng ${showDetailModal.username} thành công!`);
      setShowDetailModal(null);
      loadUsers(); // Reload the list
    } catch (e) {
      console.error("❌ Error updating user:", e.response?.data || e.message);
      setEditError(
        e.response?.data?.message || "Không thể cập nhật người dùng. Vui lòng thử lại."
      );
    } finally {
      setUpdateLoading(false);
    }
  };

  const getRoleLabel = (role) => {
    const roleMap = {
      EV_OWNER: "Chủ xe điện",
      BUYER: "Người mua",
      CVA: "Kiểm toán viên",
      ADMIN: "Quản trị viên"
    };
    return roleMap[role] || role;
  };

  const getRoleColor = (role) => {
    const colorMap = {
      EV_OWNER: "bg-blue-100 text-blue-700",
      BUYER: "bg-green-100 text-green-700",
      CVA: "bg-purple-100 text-purple-700",
      ADMIN: "bg-orange-100 text-orange-700"
    };
    return colorMap[role] || "bg-gray-100 text-gray-700";
  };

  const getStatusLabel = (status) => {
    const statusMap = {
      ACTIVE: "Hoạt động",
      INACTIVE: "Không hoạt động",
      SUSPENDED: "Đình chỉ",
      BANNED: "Bị cấm"
    };
    return statusMap[status] || status || "Hoạt động";
  };

  const getStatusColor = (status) => {
    const colorMap = {
      ACTIVE: "bg-green-100 text-green-700",
      INACTIVE: "bg-gray-100 text-gray-700",
      SUSPENDED: "bg-yellow-100 text-yellow-700",
      BANNED: "bg-red-100 text-red-700"
    };
    return colorMap[status] || "bg-green-100 text-green-700";
  };

  const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleDateString("vi-VN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit"
    });
  };

  // Filter users
  const filteredUsers = users.filter((user) => {
    const matchesSearch = 
      user.username?.toLowerCase().includes(search.toLowerCase()) ||
      user.email?.toLowerCase().includes(search.toLowerCase()) ||
      user.fullName?.toLowerCase().includes(search.toLowerCase());
    
    const matchesRole = roleFilter === "ALL" || user.role === roleFilter;
    
    return matchesSearch && matchesRole;
  });

  if (loading) {
    return (
      <div className="flex min-h-screen bg-gray-50">
        <AdminSidebar activePage="users" />
        <div className="flex-1 flex flex-col">
          <AdminHeader title="Quản lý người dùng" />
          <main className="flex-1 flex items-center justify-center">
            <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Đang tải danh sách người dùng...</p>
            </div>
          </main>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-gray-50">
      <AdminSidebar activePage="users" />

      <div className="flex-1 flex flex-col">
        <AdminHeader title="Quản lý người dùng" />

        <main className="flex-1 p-6">
          {/* Title & Stats */}
          <div className="mb-6 flex items-center justify-between">
            <div>
              <h1 className="text-xl font-semibold text-gray-800">Quản lý người dùng</h1>
              <p className="text-gray-500 text-sm">
                Xem và quản lý tất cả người dùng trên nền tảng ({filteredUsers.length} người dùng)
              </p>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setShowAddModal(true)}
                className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition"
              >
                <Plus className="w-4 h-4" />
                <span className="text-sm">Thêm người dùng</span>
              </button>
              <button
                onClick={loadUsers}
                className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition"
              >
                <RefreshCw className="w-4 h-4" />
                <span className="text-sm">Tải lại</span>
              </button>
            </div>
          </div>

          {/* Error Message */}
          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2">
              <AlertCircle className="text-red-600 w-5 h-5" />
              <span className="text-sm text-red-700">{error}</span>
              <button
                onClick={loadUsers}
                className="ml-auto text-sm text-red-600 hover:text-red-700 underline"
              >
                Thử lại
              </button>
            </div>
          )}

          {/* Search & Filter */}
          <div className="flex items-center gap-3 mb-5">
            <div className="relative flex-1">
              <Search size={16} className="absolute left-3 top-2.5 text-gray-400" />
              <input
                type="text"
                placeholder="Tìm kiếm người dùng theo tên, email..."
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
              <option value="ALL">Tất cả vai trò</option>
              <option value="EV_OWNER">Chủ xe điện</option>
              <option value="BUYER">Người mua</option>
              <option value="CVA">Kiểm toán viên</option>
              <option value="ADMIN">Quản trị viên</option>
            </select>
          </div>

          {/* User List */}
          {filteredUsers.length === 0 ? (
            <div className="text-center py-12 bg-white border border-gray-200 rounded-xl">
              <UserX className="w-16 h-16 text-gray-400 mx-auto mb-3" />
              <p className="text-gray-500">Không tìm thấy người dùng</p>
              <p className="text-gray-400 text-sm mt-1">Thử thay đổi bộ lọc của bạn</p>
            </div>
          ) : (
            <div className="space-y-3">
              {filteredUsers.map((user) => (
                <div
                  key={user.id}
                  className="flex justify-between items-center border border-gray-200 rounded-lg px-4 py-3 bg-white hover:shadow-sm transition"
                >
                  {/* Left: User Info */}
                  <div className="flex items-center gap-3 flex-1">
                    <div className="w-10 h-10 bg-gradient-to-br from-blue-400 to-blue-600 rounded-full flex items-center justify-center">
                      <span className="text-white font-semibold text-sm">
                        {user.username?.[0]?.toUpperCase() || "U"}
                      </span>
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <h2 className="font-medium text-gray-800">{user.username}</h2>
                        {user.fullName && (
                          <span className="text-xs text-gray-500">({user.fullName})</span>
                        )}
                      </div>
                      <p className="text-gray-500 text-sm truncate">{user.email}</p>
                      {user.phoneNumber && (
                        <p className="text-gray-400 text-xs">📞 {user.phoneNumber}</p>
                      )}
                    </div>
                  </div>

                  {/* Right: Role, Status, Actions */}
                  <div className="flex items-center gap-3">
                    {/* Role Badge */}
                    <span className={`text-xs font-medium px-2 py-1 rounded-full ${getRoleColor(user.role)}`}>
                      {getRoleLabel(user.role)}
                    </span>

                    {/* Status Badge */}
                    <span className={`text-xs font-medium px-2 py-1 rounded-full ${getStatusColor(user.status)}`}>
                      {getStatusLabel(user.status)}
                    </span>

                    {/* Info */}
                    <div className="text-right text-sm text-gray-600 min-w-[120px]">
                      <p className="text-xs">Tham gia: {formatDate(user.createdAt)}</p>
                      <p className="text-xs text-gray-400">ID: {user.id.toString().substring(0, 8)}...</p>
                    </div>

                    {/* Actions */}
                    <div className="flex gap-2 ml-3">
                      <button
                        onClick={() => handleOpenDetail(user)}
                        className="p-1.5 rounded-lg hover:bg-blue-50 transition"
                        title="Xem chi tiết và chỉnh sửa"
                      >
                        <Edit3 size={16} className="text-blue-600" />
                      </button>
                      <button
                        onClick={() => setDeleteConfirm(user)}
                        className="p-1.5 rounded-lg hover:bg-red-50 transition"
                        title="Xóa"
                      >
                        <Trash2 size={16} className="text-red-600" />
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </main>
      </div>

      {/* Add User Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-2xl max-w-md w-full mx-4 p-6 max-h-[90vh] overflow-y-auto">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">Thêm người dùng mới</h3>

            {addError && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
                <p className="text-sm text-red-700">{addError}</p>
              </div>
            )}

            <form onSubmit={handleAddUser} className="space-y-4">
              {/* Username */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Tên đăng nhập
                </label>
                <input
                  type="text"
                  name="username"
                  value={addFormData.username}
                  onChange={handleAddFormChange}
                  placeholder="user_name"
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                />
                <p className="text-xs text-gray-500 mt-1">
                  Chỉ chữ cái, số và dấu gạch dưới
                </p>
              </div>

              {/* Email */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Email
                </label>
                <input
                  type="email"
                  name="email"
                  value={addFormData.email}
                  onChange={handleAddFormChange}
                  placeholder="user@example.com"
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                />
              </div>

              {/* Password */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Mật khẩu
                </label>
                <input
                  type="password"
                  name="password"
                  value={addFormData.password}
                  onChange={handleAddFormChange}
                  placeholder="Tối thiểu 8 ký tự"
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                />
              </div>

              {/* Full Name */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Họ và tên
                </label>
                <input
                  type="text"
                  name="fullName"
                  value={addFormData.fullName}
                  onChange={handleAddFormChange}
                  placeholder="Nguyễn Văn A"
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                />
              </div>

              {/* Phone */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Số điện thoại
                </label>
                <input
                  type="tel"
                  name="phone"
                  value={addFormData.phone}
                  onChange={handleAddFormChange}
                  placeholder="0123456789"
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                />
              </div>

              {/* Role */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Vai trò
                </label>
                <select
                  name="role"
                  value={addFormData.role}
                  onChange={handleAddFormChange}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                >
                  <option value="CVA">Kiểm toán viên (CVA)</option>
                  <option value="ADMIN">Quản trị viên (ADMIN)</option>
                </select>
              </div>

              {/* Actions */}
              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => {
                    setShowAddModal(false);
                    setAddError("");
                  }}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={addLoading}
                  className="flex-1 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition disabled:bg-gray-400"
                >
                  {addLoading ? "Đang tạo..." : "Tạo người dùng"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* User Detail & Edit Modal */}
      {showDetailModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-2xl max-w-2xl w-full mx-4 p-6 max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-lg font-semibold text-gray-800">
                Chi tiết người dùng - {showDetailModal.username}
              </h3>
              <button
                onClick={() => setShowDetailModal(null)}
                className="text-gray-500 hover:text-gray-700"
              >
                ✕
              </button>
            </div>

            {editError && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
                <p className="text-sm text-red-700">{editError}</p>
              </div>
            )}

            {/* User Information Grid */}
            <div className="grid grid-cols-2 gap-4 mb-6 p-4 bg-gray-50 rounded-lg">
              {/* Read-only Info */}
              <div>
                <p className="text-xs text-gray-500 mb-1">User ID</p>
                <p className="text-sm font-medium text-gray-800">{showDetailModal.id}</p>
              </div>

              <div>
                <p className="text-xs text-gray-500 mb-1">Tên đăng nhập</p>
                <p className="text-sm font-medium text-gray-800">{showDetailModal.username}</p>
              </div>

              <div>
                <p className="text-xs text-gray-500 mb-1">Vai trò</p>
                <span className={`text-xs font-medium px-2 py-1 rounded-full ${getRoleColor(showDetailModal.role)}`}>
                  {getRoleLabel(showDetailModal.role)}
                </span>
              </div>

              <div>
                <p className="text-xs text-gray-500 mb-1">Trạng thái</p>
                <span className={`text-xs font-medium px-2 py-1 rounded-full ${getStatusColor(showDetailModal.status)}`}>
                  {getStatusLabel(showDetailModal.status)}
                </span>
              </div>

              <div>
                <p className="text-xs text-gray-500 mb-1">Ngày tạo</p>
                <p className="text-sm font-medium text-gray-800">{formatDate(showDetailModal.createdAt)}</p>
              </div>

              <div>
                <p className="text-xs text-gray-500 mb-1">Cập nhật lần cuối</p>
                <p className="text-sm font-medium text-gray-800">{formatDate(showDetailModal.updatedAt)}</p>
              </div>
            </div>

            {/* Editable Form */}
            <form onSubmit={handleUpdateUser} className="space-y-4">
              <h4 className="font-semibold text-gray-700 mb-3">Thông tin có thể chỉnh sửa</h4>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Email
                </label>
                <input
                  type="email"
                  name="email"
                  value={editFormData.email}
                  onChange={handleEditFormChange}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Họ và tên
                </label>
                <input
                  type="text"
                  name="fullName"
                  value={editFormData.fullName}
                  onChange={handleEditFormChange}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Số điện thoại
                </label>
                <input
                  type="tel"
                  name="phone"
                  value={editFormData.phone}
                  onChange={handleEditFormChange}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              {/* Vehicles Section - Only for EV_OWNER */}
              {showDetailModal.role === "EV_OWNER" && (
                <div className="mt-6 pt-6 border-t">
                  <h4 className="font-semibold text-gray-700 mb-3">🚗 Xe điện đã đăng ký</h4>

                  {vehiclesLoading ? (
                    <div className="text-center py-4">
                      <div className="inline-block animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600"></div>
                      <p className="text-sm text-gray-500 mt-2">Đang tải thông tin xe...</p>
                    </div>
                  ) : userVehicles.length === 0 ? (
                    <p className="text-sm text-gray-500 py-4 text-center">Người dùng này chưa đăng ký xe nào</p>
                  ) : (
                    <div className="space-y-3">
                      {userVehicles.map((vehicle) => (
                        <div
                          key={vehicle.id}
                          className="border border-gray-200 rounded-lg p-3 bg-gray-50 hover:bg-gray-100 transition"
                        >
                          <div className="flex items-start gap-3">
                            <div className="text-2xl">🚗</div>
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center justify-between">
                                <h5 className="font-medium text-gray-800">{vehicle.model}</h5>
                                <span className="text-xs bg-blue-100 text-blue-700 px-2 py-1 rounded-full">
                                  {vehicle.journeyCount} hành trình
                                </span>
                              </div>
                              <p className="text-xs text-gray-600 mt-1">
                                <strong>VIN:</strong> {vehicle.vin}
                              </p>
                              <p className="text-xs text-gray-600">
                                <strong>Đăng ký:</strong> {new Date(vehicle.registrationDate).toLocaleDateString("vi-VN")}
                              </p>
                              <p className="text-xs text-gray-500 mt-1">
                                <strong>ID:</strong> {vehicle.id.toString().substring(0, 12)}...
                              </p>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* Actions */}
              <div className="flex gap-3 pt-4 border-t">
                <button
                  type="button"
                  onClick={() => setShowDetailModal(null)}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition"
                >
                  Đóng
                </button>
                <button
                  type="submit"
                  disabled={updateLoading}
                  className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:bg-gray-400"
                >
                  {updateLoading ? "Đang cập nhật..." : "Lưu thay đổi"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deleteConfirm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-2xl max-w-md w-full mx-4 p-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center">
                <Trash2 className="w-6 h-6 text-red-600" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-gray-800">Xác nhận xóa</h3>
                <p className="text-sm text-gray-500">Hành động này không thể hoàn tác</p>
              </div>
            </div>

            <div className="bg-gray-50 rounded-lg p-4 mb-6">
              <p className="text-sm text-gray-700 mb-2">
                Bạn có chắc chắn muốn xóa người dùng này?
              </p>
              <div className="flex items-center gap-2 text-sm">
                <span className="font-medium text-gray-800">{deleteConfirm.username}</span>
                <span className="text-gray-500">({deleteConfirm.email})</span>
              </div>
              <span className={`inline-block mt-2 text-xs font-medium px-2 py-1 rounded-full ${getRoleColor(deleteConfirm.role)}`}>
                {getRoleLabel(deleteConfirm.role)}
              </span>
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => setDeleteConfirm(null)}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition"
              >
                Hủy
              </button>
              <button
                onClick={() => handleDeleteUser(deleteConfirm.id, deleteConfirm.username)}
                className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition"
              >
                Xóa người dùng
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}