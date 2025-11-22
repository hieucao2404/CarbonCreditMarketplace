import React, { useEffect, useState } from "react";
import axios from "axios";
import Sidebar from "../components/Sidebar";
import Header from "../components/Header";

export default function VehicleManagement() {
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);

  const [newVehicle, setNewVehicle] = useState({
    vin: "",
    model: "",
    registrationDate: "",
  });

  const API_BASE = "http://localhost:8080/api/vehicles";
  const token = localStorage.getItem("token");

  // ---- Popup States ----
  const [popup, setPopup] = useState({
    show: false,
    title: "",
    message: "",
    type: "info", // success | error | confirm
    onConfirm: null,
  });

  // OPEN POPUP
  const openPopup = (title, message, type = "info", onConfirm = null) => {
    setPopup({
      show: true,
      title,
      message,
      type,
      onConfirm,
    });
  };

  // CLOSE POPUP
  const closePopup = () => {
    setPopup({ ...popup, show: false });
  };

  // ---- Load danh sách xe ----
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
      openPopup("Lỗi", "Không thể tải danh sách xe. Vui lòng đăng nhập lại.", "error");
    } finally {
      setLoading(false);
    }
  };

  // ---- Thêm xe ----
  const handleAddVehicle = async (e) => {
    e.preventDefault();

    if (!newVehicle.vin || !newVehicle.model || !newVehicle.registrationDate) {
      openPopup("Thiếu thông tin", "Vui lòng nhập đầy đủ thông tin xe.", "error");
      return;
    }

    try {
      const res = await axios.post(API_BASE, newVehicle, {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (res.data.success) {
        setVehicles([...vehicles, res.data.data]);
        setNewVehicle({ vin: "", model: "", registrationDate: "" });

        openPopup("Thành công", "Thêm xe thành công!", "success");
      }
    } catch (err) {
      openPopup("Lỗi thêm xe", err.response?.data?.message || "Không thể thêm xe.", "error");
    }
  };

  // ---- Xóa xe ----
  const handleDelete = (id) => {
    openPopup(
      "Xóa xe",
      "Bạn có chắc muốn xóa xe này không?",
      "confirm",
      () => confirmDelete(id)
    );
  };

  const confirmDelete = async (id) => {
    try {
      const res = await axios.delete(`${API_BASE}/${id}`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (res.data.success) {
        setVehicles(vehicles.filter((v) => v.id !== id));
        openPopup("Thành công", "Xe đã được xóa thành công!", "success");
      }
    } catch (err) {
      openPopup("Lỗi xóa xe", err.response?.data?.message || "Không thể xóa xe.", "error");
    }
  };

  // ==================================================
  //                     UI
  // ==================================================
  return (
    <div className="flex min-h-screen w-screen bg-[#F9FAFB] overflow-hidden">
      <Sidebar />

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

            {/* ------------ Form thêm xe ---------------- */}
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
                  placeholder="Model xe"
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

            {/* ------------ Danh sách xe ---------------- */}
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
                          className="w-20 h-20 rounded-lg object-cover border border-gray-200"
                        />
                        <div>
                          <h4 className="font-semibold">{v.model}</h4>
                          <p className="text-sm text-gray-500">VIN: {v.vin}</p>
                          <p className="text-sm text-gray-500">
                            Ngày đăng ký:{" "}
                            {new Date(v.registrationDate).toLocaleDateString("vi-VN")}
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

      {/* ==================================================
                 Popup (giống Header.jsx)
      ================================================== */}
      {popup.show && (
        <div className="fixed inset-0 flex items-center justify-center bg-black/40 z-50">
          <div className="bg-white rounded-2xl shadow-lg p-6 w-80 text-center animate-fadeSlideIn">
            <h2 className="text-lg font-semibold mb-3 text-gray-800">
              {popup.title}
            </h2>

            <p className="text-gray-600 mb-5">{popup.message}</p>

            <div className="flex justify-center gap-3">
              {/* Confirm popup: Hiện 2 nút */}
              {popup.type === "confirm" ? (
                <>
                  <button
                    onClick={closePopup}
                    className="px-4 py-2 rounded-lg bg-gray-200 hover:bg-gray-300 transition"
                  >
                    Hủy
                  </button>

                  <button
                    onClick={() => {
                      closePopup();
                      popup.onConfirm && popup.onConfirm();
                    }}
                    className="px-4 py-2 rounded-lg bg-red-500 text-white hover:bg-red-600 transition"
                  >
                    Xóa
                  </button>
                </>
              ) : (
                /* Success or Error popup: Chỉ có nút Đóng */
                <button
                  onClick={closePopup}
                  className="px-4 py-2 rounded-lg bg-green-500 text-white hover:bg-green-600 transition"
                >
                  Đóng
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

/* Animation */
const style = document.createElement("style");
style.innerHTML = `
@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}
.animate-fadeSlideIn {
  animation: fadeSlideIn 0.25s ease-out;
}
`;
document.head.appendChild(style);
