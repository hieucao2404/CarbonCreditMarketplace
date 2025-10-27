import React from "react";
import { Search } from "lucide-react";
import AdminSidebar from "../components/AdminSidebar";
import AdminHeader from "../components/AdminHeader";

const transactions = [
  {
    id: "TX001",
    buyer: "Nguyễn Văn A – Công ty Green Tech",
    amount: "50 tCO₂",
    price: "25,000 VND/tCO₂",
    total: "1,250,000 VND",
    fee: "62,500 VND",
    status: "Hoàn thành",
    date: "2024-12-28",
  },
  {
    id: "TX002",
    buyer: "Lê Văn C – EcoViet Corp",
    amount: "75 tCO₂",
    price: "24,800 VND/tCO₂",
    total: "1,860,000 VND",
    fee: "93,000 VND",
    status: "Hoàn thành",
    date: "2024-12-27",
  },
  {
    id: "TX003",
    buyer: "Phạm Thị D – Clean Energy Ltd",
    amount: "30 tCO₂",
    price: "26,000 VND/tCO₂",
    total: "780,000 VND",
    fee: "39,000 VND",
    status: "Tranh chấp",
    date: "2024-12-26",
  },
  {
    id: "TX004",
    buyer: "Hoàng Văn E – Sustainable Co.",
    amount: "100 tCO₂",
    price: "25,500 VND/tCO₂",
    total: "2,550,000 VND",
    fee: "127,500 VND",
    status: "Đang xử lý",
    date: "2024-12-25",
  },
];

const getStatusColor = (status) => {
  switch (status) {
    case "Hoàn thành":
      return "bg-green-100 text-green-700";
    case "Tranh chấp":
      return "bg-orange-100 text-orange-700";
    case "Đang xử lý":
      return "bg-yellow-100 text-yellow-700";
    default:
      return "bg-gray-100 text-gray-600";
  }
};

export default function AdminTransactionManagement() {
  return (
    <div className="flex min-h-screen bg-gray-50">
      {/* Sidebar */}
      <AdminSidebar />

      {/* Main content */}
      <div className="flex-1 flex flex-col">
        <AdminHeader />

        <main className="p-8">
          <div className="bg-white rounded-2xl shadow-sm p-6">
            <h2 className="text-lg font-semibold mb-4 text-gray-800">
              Quản lý giao dịch
            </h2>
            <p className="text-gray-500 mb-6">
              Theo dõi và xử lý tất cả giao dịch trên nền tảng
            </p>

            {/* Search */}
            <div className="flex items-center bg-gray-100 rounded-xl px-3 py-2 mb-6">
              <Search className="text-gray-400" size={18} />
              <input
                type="text"
                placeholder="Tìm kiếm giao dịch..."
                className="bg-transparent flex-1 ml-2 text-sm outline-none text-gray-700"
              />
            </div>

            {/* Transaction list */}
            <div className="space-y-4">
              {transactions.map((tx) => (
                <div
                  key={tx.id}
                  className="flex items-center justify-between border border-gray-200 rounded-xl p-4 hover:shadow-sm transition bg-white"
                >
                  <div>
                    <p className="font-semibold text-gray-800">{tx.id}</p>
                    <p className="text-gray-500 text-sm">{tx.buyer}</p>
                    <p className="text-sm mt-1">
                      <span className="font-medium">{tx.amount}</span> ·{" "}
                      <span className="text-gray-600">{tx.price}</span>
                    </p>
                  </div>

                  <div className="text-right">
                    <p className="font-semibold text-gray-800">{tx.total}</p>
                    <p className="text-xs text-gray-400">Phí: {tx.fee}</p>
                  </div>

                  <div className="flex items-center gap-2">
                    <span
                      className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(
                        tx.status
                      )}`}
                    >
                      {tx.status}
                    </span>
                    <span className="text-xs text-gray-400">{tx.date}</span>
                  </div>

                  <div className="flex gap-2">
                    <button className="border border-gray-300 text-gray-700 px-3 py-1.5 rounded-lg text-sm hover:bg-gray-100">
                      Chi tiết
                    </button>
                    {tx.status !== "Hoàn thành" && (
                      <button className="bg-black text-white px-3 py-1.5 rounded-lg text-sm hover:bg-gray-800">
                        Xử lý
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
