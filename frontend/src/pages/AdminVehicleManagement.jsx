import React, { useState, useEffect } from "react";
import { Search, Trash2, RefreshCw, AlertCircle, Truck, Eye } from "lucide-react";
import AdminSidebar from "../components/AdminSidebar";
import AdminHeader from "../components/AdminHeader";
import axiosInstance from "../api/axiosInstance";

export default function AdminVehicleManagement() {
  const [search, setSearch] = useState("");
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [deleteConfirm, setDeleteConfirm] = useState(null);
  const [showDetailModal, setShowDetailModal] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState("");

  useEffect(() => {
    loadVehicles();
  }, []);

  const loadVehicles = async () => {
    setLoading(true);
    setError("");

    try {
      const response = await axiosInstance.get("/vehicles/admin/all");
      console.log("🚗 Vehicles Response:", response.data);

      if (response.data?.data) {
        setVehicles(response.data.data);
      }
    } catch (e) {
      console.error("❌ Error loading vehicles:", e.response?.data || e.message);
      setError(e.response?.data?.message || "Không thể tải danh sách xe điện.");
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDetail = async (vehicle) => {
    setShowDetailModal(vehicle);
    setDetailLoading(false);
    setDetailError("");
  };

  const handleDeleteVehicle = async (vehicleId, vehicleModel) => {
    try {
      await axiosInstance.delete(`/vehicles/${vehicleId}`);
      alert(`Đã xóa xe ${vehicleModel} thành công!`);
      setDeleteConfirm(null);
      loadVehicles();
    } catch (e) {
      console.error("❌ Error deleting vehicle:", e.response?.data || e.message);
      alert(e.response?.data?.message || "Không thể xóa xe điện.");
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleDateString("vi-VN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit"
    });
  };

  // Filter vehicles
  const filteredVehicles = vehicles.filter((vehicle) => {
    const matchesSearch =
      vehicle.model?.toLowerCase().includes(search.toLowerCase()) ||
      vehicle.vin?.toLowerCase().includes(search.toLowerCase()) ||
      vehicle.username?.toLowerCase().includes(search.toLowerCase());

    return matchesSearch;
  });

  if (loading) {
    return (
      <div className="flex min-h-screen bg-gray-50">
        <AdminSidebar activePage="vehicles" />
        <div className="flex-1 flex flex-col">
          <AdminHeader title="Quản lý xe điện" />
          <main className="flex-1 flex items-center justify-center">
            <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Đang tải danh sách xe điện...</p>
            </div>
          </main>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-gray-50">
      <AdminSidebar activePage="vehicles" />

      <div className="flex-1 flex flex-col">
        <AdminHeader title="Quản lý xe điện" />

        <main className="flex-1 p-6">
          {/* Title & Stats */}
          <div className="mb-6 flex items-center justify-between">
            <div>
              <h1 className="text-xl font-semibold text-gray-800">Quản lý xe điện</h1>
              <p className="text-gray-500 text-sm">
                Xem và quản lý tất cả xe điện trên nền tảng ({filteredVehicles.length} xe)
              </p>
            </div>
            <button
              onClick={loadVehicles}
              className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition"
            >
              <RefreshCw className="w-4 h-4" />
              <span className="text-sm">Tải lại</span>
            </button>
          </div>

          {/* Error Message */}
          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2">
              <AlertCircle className="text-red-600 w-5 h-5" />
              <span className="text-sm text-red-700">{error}</span>
              <button
                onClick={loadVehicles}
                className="ml-auto text-sm text-red-600 hover:text-red-700 underline"
              >
                Thử lại
              </button>
            </div>
          )}

          {/* Search Bar */}
          <div className="relative flex-1 mb-5">
            <Search size={16} className="absolute left-3 top-2.5 text-gray-400" />
            <input
              type="text"
              placeholder="Tìm kiếm xe theo model, VIN, hoặc chủ sở hữu..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full border border-gray-200 rounded-lg pl-9 pr-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-200"
            />
          </div>

          {/* Vehicle List */}
          {filteredVehicles.length === 0 ? (
            <div className="text-center py-12 bg-white border border-gray-200 rounded-xl">
              <Truck className="w-16 h-16 text-gray-400 mx-auto mb-3" />
              <p className="text-gray-500">Không tìm thấy xe điện</p>
              <p className="text-gray-400 text-sm mt-1">Không có xe nào được đăng ký trên hệ thống</p>
            </div>
          ) : (
            <div className="space-y-3">
              {filteredVehicles.map((vehicle) => (
                <div
                  key={vehicle.id}
                  className="flex justify-between items-center border border-gray-200 rounded-lg px-4 py-3 bg-white hover:shadow-sm transition"
                >
                  {/* Left: Vehicle Info */}
                  <div className="flex items-center gap-3 flex-1">
                    <div className="w-10 h-10 bg-gradient-to-br from-green-400 to-green-600 rounded-full flex items-center justify-center">
                      <span className="text-white font-semibold text-sm">🚗</span>
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <h2 className="font-medium text-gray-800">{vehicle.model}</h2>
                      </div>
                      <p className="text-gray-500 text-sm">VIN: {vehicle.vin}</p>
                      {vehicle.username && (
                        <p className="text-gray-400 text-xs">👤 Chủ sở hữu: {vehicle.username}</p>
                      )}
                    </div>
                  </div>

                  {/* Right: Details & Actions */}
                  <div className="flex items-center gap-3">
                    {/* Stats */}
                    <div className="text-right text-sm text-gray-600 min-w-[140px]">
                      <p className="text-xs">Đăng ký: {formatDate(vehicle.registrationDate)}</p>
                      <p className="text-xs text-gray-400">Hành trình: {vehicle.journeyCount || 0}</p>
                      <p className="text-xs text-gray-400">ID: {vehicle.id.toString().substring(0, 8)}...</p>
                    </div>

                    {/* Actions */}
                    <div className="flex gap-2 ml-3">
                      <button
                        onClick={() => handleOpenDetail(vehicle)}
                        className="p-1.5 rounded-lg hover:bg-blue-50 transition"
                        title="Xem chi tiết"
                      >
                        <Eye size={16} className="text-blue-600" />
                      </button>
                      <button
                        onClick={() => setDeleteConfirm(vehicle)}
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

      {/* Vehicle Detail Modal */}
      {showDetailModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-2xl max-w-2xl w-full mx-4 p-6 max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-lg font-semibold text-gray-800">
                Chi tiết xe - {showDetailModal.model}
              </h3>
              <button
                onClick={() => setShowDetailModal(null)}
                className="text-gray-500 hover:text-gray-700"
              >
                ✕
              </button>
            </div>

            {detailError && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
                <p className="text-sm text-red-700">{detailError}</p>
              </div>
            )}

            {/* Vehicle Information Grid */}
            <div className="grid grid-cols-2 gap-4 mb-6 p-4 bg-gray-50 rounded-lg">
              {/* Vehicle Details */}
              <div>
                <p className="text-xs text-gray-500 mb-1">ID Xe</p>
                <p className="text-sm font-medium text-gray-800">{showDetailModal.id}</p>
              </div>

              <div>
                <p className="text-xs text-gray-500 mb-1">Model</p>
                <p className="text-sm font-medium text-gray-800">{showDetailModal.model}</p>
              </div>

              <div>
                <p className="text-xs text-gray-500 mb-1">VIN</p>
                <p className="text-sm font-medium text-gray-800 break-all">{showDetailModal.vin}</p>
              </div>

              <div>
                <p className="text-xs text-gray-500 mb-1">Chủ sở hữu</p>
                <p className="text-sm font-medium text-gray-800">{showDetailModal.username || "N/A"}</p>
              </div>

              <div>
                <p className="text-xs text-gray-500 mb-1">Ngày đăng ký</p>
                <p className="text-sm font-medium text-gray-800">
                  {formatDate(showDetailModal.registrationDate)}
                </p>
              </div>

              <div>
                <p className="text-xs text-gray-500 mb-1">Số hành trình</p>
                <p className="text-sm font-medium text-gray-800">{showDetailModal.journeyCount || 0}</p>
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

            {/* Owner Information */}
            {showDetailModal.username && (
              <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                <h4 className="font-semibold text-gray-700 mb-2">👤 Thông tin chủ sở hữu</h4>
                <p className="text-sm text-gray-700">
                  <strong>Tên đăng nhập:</strong> {showDetailModal.username}
                </p>
              </div>
            )}

            {/* Actions */}
            <div className="flex gap-3 pt-4 border-t">
              <button
                onClick={() => setShowDetailModal(null)}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition"
              >
                Đóng
              </button>
              <button
                onClick={() => {
                  setDeleteConfirm(showDetailModal);
                  setShowDetailModal(null);
                }}
                className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition"
              >
                Xóa xe
              </button>
            </div>
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
                Bạn có chắc chắn muốn xóa xe điện này?
              </p>
              <div className="flex items-center gap-2 text-sm">
                <span className="font-medium text-gray-800">{deleteConfirm.model}</span>
                <span className="text-gray-500">(VIN: {deleteConfirm.vin})</span>
              </div>
              {deleteConfirm.username && (
                <p className="text-xs text-gray-600 mt-2">
                  Chủ sở hữu: <strong>{deleteConfirm.username}</strong>
                </p>
              )}
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => setDeleteConfirm(null)}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition"
              >
                Hủy
              </button>
              <button
                onClick={() => handleDeleteVehicle(deleteConfirm.id, deleteConfirm.model)}
                className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition"
              >
                Xóa xe
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
