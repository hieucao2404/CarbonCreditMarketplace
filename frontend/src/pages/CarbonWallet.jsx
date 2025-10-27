import React from "react";
import Sidebar from "../components/Sidebar";
import Header from "../components/Header";
import { Wallet, Clock, TrendingUp, Leaf, DollarSign } from "lucide-react";

export default function CarbonWallet() {
  const stats = [
    {
      icon: <Wallet className="text-green-600" size={22} />,
      title: "Số dư hiện tại",
      value: "342.5 tCO₂",
      sub: "Tương đương ~8,562,500 VNĐ",
      color: "text-green-600",
    },
    {
      icon: <Clock className="text-orange-500" size={22} />,
      title: "Đang chờ xác minh",
      value: "89.2 tCO₂",
      sub: "Dự kiến nhận trong 2-3 ngày",
      color: "text-orange-500",
    },
    {
      icon: <TrendingUp className="text-blue-600" size={22} />,
      title: "Tổng đã bán",
      value: "567.8 tCO₂",
      sub: "Tổng doanh thu: 15,680 VNĐ",
      color: "text-blue-600",
    },
  ];

  const transactions = [
    {
      icon: <Leaf className="text-green-600" size={18} />,
      title: "Hành trình Tesla Model 3",
      date: "2024-12-28",
      amount: "+12.5 tCO₂",
      color: "text-green-600",
    },
    {
      icon: <DollarSign className="text-red-500" size={18} />,
      title: "Bán cho Công ty ABC",
      date: "2024-12-27",
      amount: "-25 tCO₂",
      color: "text-red-500",
    },
    {
      icon: <Leaf className="text-green-600" size={18} />,
      title: "Hành trình BYD Seal",
      date: "2024-12-26",
      amount: "+8.3 tCO₂",
      color: "text-green-600",
    },
    {
      icon: <Leaf className="text-green-600" size={18} />,
      title: "Hành trình Tesla Model 3",
      date: "2024-12-25",
      amount: "+15.7 tCO₂",
      color: "text-green-600",
    },
  ];

  return (
    <div className="flex min-h-screen w-screen bg-[#F9FAFB] overflow-hidden">
      <Sidebar />

      <div className="flex flex-col flex-1 min-h-screen w-full">
        <Header />

        <main className="flex-1 p-8 w-full bg-[#F9FAFB] overflow-y-auto">
          <div className="w-full max-w-full mx-auto space-y-8">
            {/* --- Header section --- */}
            <div>
              <h2 className="text-xl font-semibold text-gray-800">Ví carbon</h2>
              <p className="text-gray-500 text-sm">
                Theo dõi số dư và lịch sử giao dịch tín chỉ carbon của bạn
              </p>
            </div>

            {/* --- 3 statistic cards --- */}
            <div className="grid grid-cols-3 gap-6 w-full">
              {stats.map((item, i) => (
                <div
                  key={i}
                  className="bg-white rounded-2xl border border-gray-300 shadow-sm p-6 transition-all hover:shadow-md flex flex-col justify-between"
                >
                  <div className="flex items-center gap-2 mb-2 text-gray-700 font-medium">
                    {item.icon}
                    <span>{item.title}</span>
                  </div>
                  <p className={`text-3xl font-semibold ${item.color}`}>
                    {item.value}
                  </p>
                  <p className="text-sm text-gray-500">{item.sub}</p>
                </div>
              ))}
            </div>

            {/* --- Transaction history --- */}
            <div className="bg-gray-50 rounded-2xl border border-gray-200 shadow-sm p-6">
              <h3 className="font-semibold mb-4 text-gray-700 text-lg">
                Lịch sử giao dịch ví carbon
              </h3>

              <div className="divide-y divide-gray-200">
                {transactions.map((t, i) => (
                  <div
                    key={i}
                    className="flex justify-between items-center py-4 transition-all hover:bg-white hover:shadow-sm rounded-xl px-3"
                  >
                    <div className="flex items-center gap-3">
                      <div className="bg-gray-100 p-2 rounded-full">
                        {t.icon}
                      </div>
                      <div>
                        <p className="font-medium text-gray-800">{t.title}</p>
                        <p className="text-sm text-gray-500">{t.date}</p>
                      </div>
                    </div>
                    <p className={`text-sm font-semibold ${t.color}`}>
                      {t.amount}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
