import React from "react";
import { useNavigate } from "react-router-dom";
import { Leaf, Car, ShoppingBag, Shield, Settings } from "lucide-react";
import { motion } from "framer-motion";

export default function HomePage1() {
  const navigate = useNavigate();

  const roles = [
    {
      title: "Chủ sở hữu xe điện (EV Owner)",
      icon: <Car className="w-10 h-10 text-green-600" />,
      description: [
        "Kết nối và đồng bộ dữ liệu hành trình từ xe điện.",
        "Tính toán lượng CO₂ giảm phát thải, quy đổi sang tín chỉ carbon.",
        "Quản lý ví carbon và niêm yết tín chỉ để bán (bán trực tiếp / đấu giá).",
        "Theo dõi giao dịch, rút tiền và xem báo cáo cá nhân.",
        "AI gợi ý giá bán tín chỉ dựa trên dữ liệu thị trường.",
      ],
    },
    {
      title: "Người mua tín chỉ carbon (Buyer)",
      icon: <ShoppingBag className="w-10 h-10 text-blue-600" />,
      description: [
        "Tìm kiếm và lọc tín chỉ theo giá, khu vực, số lượng.",
        "Mua tín chỉ trực tiếp hoặc tham gia đấu giá.",
        "Thanh toán online (e-wallet, banking...).",
        "Nhận chứng nhận tín chỉ carbon (certificate).",
        "Theo dõi và quản lý lịch sử mua tín chỉ.",
      ],
    },
    {
      title: "Cơ quan xác minh (CVA)",
      icon: <Shield className="w-10 h-10 text-amber-600" />,
      description: [
        "Kiểm tra và xác minh tính hợp lệ của dữ liệu hành trình.",
        "Phê duyệt tín chỉ carbon đủ điều kiện giao dịch.",
        "Cấp chứng nhận cho người mua và người bán.",
      ],
    },
    {
      title: "Quản trị viên hệ thống (Admin)",
      icon: <Settings className="w-10 h-10 text-gray-700" />,
      description: [
        "Quản lý người dùng, giao dịch, và báo cáo tổng thể.",
        "Theo dõi hệ thống, đảm bảo an toàn và hiệu năng.",
        "Điều phối hoạt động và xử lý khiếu nại.",
      ],
    },
  ];

  return (
    <div className="min-h-screen flex flex-col bg-gray-50">
      {/* 🌿 Header */}
      <header className="flex justify-between items-center px-10 py-5 bg-white shadow-sm border-b border-gray-200">
        <div className="flex items-center gap-2">
          <div className="bg-green-100 p-2 rounded-full">
            <Leaf className="w-6 h-6 text-green-600" />
          </div>
          <h1 className="font-semibold text-xl text-gray-800">
            Carbon Credit Exchange
          </h1>
        </div>

        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate("/login")}
            className="px-5 py-2 text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-100 transition"
          >
            Đăng nhập
          </button>
          <button
            onClick={() => navigate("/register")}
            className="px-5 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition"
          >
            Đăng ký
          </button>
        </div>
      </header>

      {/* 🌍 Hero Section (đã bỏ slider, thay bằng banner tĩnh) */}
      <section className="relative w-full h-[400px] bg-gradient-to-r from-green-600 to-emerald-500 flex flex-col items-center justify-center text-center text-white px-6">
        <motion.h2
          className="text-3xl md:text-4xl font-bold mb-3"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
        >
          Nền tảng giao dịch tín chỉ Carbon thông minh
        </motion.h2>
        <motion.p
          className="text-lg text-green-100 max-w-2xl"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6 }}
        >
          Kết nối chủ xe điện, người mua tín chỉ và cơ quan xác minh — hướng đến
          tương lai xanh bền vững.
        </motion.p>
      </section>

      {/* 🌱 Roles Section */}
      <main className="flex-1 px-10 py-16 bg-gray-50">
        <h2 className="text-3xl font-semibold text-gray-800 text-center mb-12">
          Các vai trò trong hệ thống
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-8">
          {roles.map((role, i) => (
            <motion.div
              key={i}
              whileHover={{ y: -8, scale: 1.02 }}
              className="bg-white border border-gray-200 shadow-sm rounded-2xl p-6 hover:shadow-md transition"
            >
              <div className="flex items-center gap-3 mb-4">
                <div className="p-3 bg-green-50 rounded-xl">{role.icon}</div>
                <h3 className="font-semibold text-lg text-gray-800 leading-snug">
                  {role.title}
                </h3>
              </div>

              <ul className="space-y-2 text-gray-600 text-sm list-disc list-inside">
                {role.description.map((desc, j) => (
                  <li key={j}>{desc}</li>
                ))}
              </ul>
            </motion.div>
          ))}
        </div>
      </main>

      {/* 🌾 Footer */}
      <footer className="bg-white border-t border-gray-200 py-6 text-center text-gray-500 text-sm">
        © 2025 Carbon Credit Exchange — Hướng tới phát triển xanh bền vững 🌿
      </footer>
    </div>
  );
}
