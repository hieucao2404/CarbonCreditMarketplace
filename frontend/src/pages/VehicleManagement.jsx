import React, { useEffect, useState } from "react";
import axios from "axios";
import Sidebar from "../components/Sidebar";
import Header from "../components/Header";

/**
 * Trang Quản lý xe điện (EV Owner)
 * - Gọi API lấy danh sách xe của user: GET /api/vehicles/my-vehicles
 * - Cho phép thêm xe mới: POST /api/vehicles
 * - Cho phép xóa xe: DELETE /api/vehicles/{id}
 */

export default function VehicleManagement() {
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [newVehicle, setNewVehicle] = useState({
    vin: "",
    model: "",
    registrationDate: "",
  });

  const API_BASE = "http://localhost:8080/api/vehicles";

  // 🧩 Lấy token từ localStorage (được backend trả khi login)
  const token = localStorage.getItem("token");

  // 🧭 Load danh sách xe khi vào trang
  useEffect(() => {
    fetchVehicles();
  }, []);

  const fetchVehicles = async () => {
    setLoading(true);
    try {
      const res = await axios.get(`${API_BASE}/my-vehicles`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.data.success) {
        setVehicles(res.data.data);
      }
    } catch (err) {
      console.error("❌ Lỗi tải danh sách xe:", err);
      alert("Không thể tải danh sách xe. Vui lòng đăng nhập lại.");
    } finally {
      setLoading(false);
    }
  };

  // 🚗 Thêm xe mới
  const handleAddVehicle = async (e) => {
    e.preventDefault();
    if (!newVehicle.vin || !newVehicle.model || !newVehicle.registrationDate) {
      alert("Vui lòng nhập đầy đủ thông tin xe.");
      return;
    }

    try {
      const res = await axios.post(API_BASE, newVehicle, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.data.success) {
        alert("✅ Thêm xe thành công!");
        setVehicles([...vehicles, res.data.data]);
        setNewVehicle({ vin: "", model: "", registrationDate: "" });
      }
    } catch (err) {
      console.error("❌ Lỗi thêm xe:", err);
      alert(err.response?.data?.message || "Lỗi khi thêm xe mới.");
    }
  };

  // 🗑️ Xóa xe
  const handleDelete = async (id) => {
    if (!window.confirm("Bạn có chắc muốn xóa xe này không?")) return;
    try {
      const res = await axios.delete(`${API_BASE}/${id}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.data.success) {
        alert("🗑️ Xóa xe thành công!");
        setVehicles(vehicles.filter((v) => v.id !== id));
      }
    } catch (err) {
      console.error("❌ Lỗi xóa xe:", err);
      alert(err.response?.data?.message || "Không thể xóa xe.");
    }
  };

  return (
    <div className="flex min-h-screen w-screen bg-[#F9FAFB] overflow-hidden">
      {/* Sidebar */}
      <Sidebar />

      {/* Main content */}
      <div className="flex flex-col flex-1 min-h-screen w-full">
        <Header />

        <main className="flex-1 p-8 w-full bg-[#F9FAFB] overflow-y-auto">
          <div className="space-y-8">
            <div>
              <h2 className="text-xl font-semibold">Quản lý xe điện</h2>
              <p className="text-gray-500 text-sm">
                Kết nối và đồng bộ dữ liệu từ xe điện của bạn
              </p>
            </div>

            {/* Form thêm xe */}
            <form
              onSubmit={handleAddVehicle}
              className="bg-white p-6 rounded-xl shadow border border-gray-200 space-y-4"
            >
              <h3 className="font-semibold text-gray-700">➕ Thêm xe mới</h3>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <input
                  type="text"
                  placeholder="VIN"
                  value={newVehicle.vin}
                  onChange={(e) =>
                    setNewVehicle({ ...newVehicle, vin: e.target.value })
                  }
                  className="border border-gray-300 p-2 rounded-lg"
                />
                <input
                  type="text"
                  placeholder="Model xe (VD: Tesla Model 3)"
                  value={newVehicle.model}
                  onChange={(e) =>
                    setNewVehicle({ ...newVehicle, model: e.target.value })
                  }
                  className="border border-gray-300 p-2 rounded-lg"
                />
                <input
                  type="date"
                  value={newVehicle.registrationDate}
                  onChange={(e) =>
                    setNewVehicle({
                      ...newVehicle,
                      registrationDate: e.target.value,
                    })
                  }
                  className="border border-gray-300 p-2 rounded-lg"
                />
              </div>
              <button
                type="submit"
                className="bg-black text-white px-4 py-2 rounded-lg text-sm hover:bg-gray-800"
              >
                🚗 Thêm xe
              </button>
            </form>

            {/* Danh sách xe */}
            <div>
              <h3 className="font-semibold text-gray-700 mb-3">
                Danh sách xe đã đăng ký
              </h3>

              {loading ? (
                <p>Đang tải danh sách xe...</p>
              ) : vehicles.length === 0 ? (
                <p className="text-gray-500">Chưa có xe nào được đăng ký.</p>
              ) : (
                <div className="space-y-4">
                  {vehicles.map((v, i) => (
                    <div
                      key={v.id || i}
                      className="bg-white rounded-xl border border-gray-200 p-4 shadow-sm flex justify-between items-center"
                    >
                      <div className="flex items-center gap-4">
                        <img
                          src="https://img.icons8.com/?size=512&id=59819&format=png"
                          alt={v.model}
                          className="w-20 h-20 rounded-lg object-cover border border-gray-200"
                        />
                        <div>
                          <h4 className="font-semibold">{v.model}</h4>
                          <p className="text-sm text-gray-500">
                            VIN: {v.vin || "Không có"}
                          </p>
                          <p className="text-sm text-gray-500">
                            Ngày đăng ký:{" "}
                            {v.registrationDate
                              ? new Date(v.registrationDate).toLocaleDateString(
                                  "vi-VN"
                                )
                              : "—"}
                          </p>
                        </div>
                      </div>

                      <button
                        onClick={() => handleDelete(v.id)}
                        className="text-red-600 text-sm hover:underline"
                      >
                        Xóa
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
