import React, { useEffect, useState } from "react";
import axios from "axios";
import BuyerSidebar from "../components/BuyerSidebar";
import BuyerHeader from "../components/BuyerHeader";
import { User, Mail, Phone, Calendar, Shield } from "lucide-react";

export default function BuyerProfile() {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) return;

    axios
      .get("http://localhost:8080/api/users/me", {
        headers: { Authorization: `Bearer ${token}` },
      })
      .then((res) => {
        setProfile(res.data.data);
        setLoading(false);
      })
      .catch((err) => {
        console.error("Failed to load profile:", err);
        setLoading(false);
      });
  }, []);

  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen text-gray-600">
        Đang tải hồ sơ...
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-gray-50">
      {/* Sidebar */}
      <BuyerSidebar />

      {/* Main content */}
      <div className="flex-1 flex flex-col">
        <BuyerHeader />

        <main className="flex-1 p-8">
          <h1 className="text-2xl font-semibold text-gray-800 mb-6">
            Hồ sơ của tôi
          </h1>

          <div className="bg-white shadow-md rounded-xl p-6 max-w-2xl border border-gray-200">
            <div className="flex items-center gap-4 border-b pb-4 mb-4">
              <div className="bg-blue-100 text-blue-700 font-bold text-xl w-14 h-14 flex items-center justify-center rounded-full">
                {profile?.fullName?.charAt(0).toUpperCase() || "?"}
              </div>
              <div>
                <h2 className="text-xl font-medium text-gray-800">
                  {profile?.fullName}
                </h2>
                <p className="text-sm text-gray-500">Buyer</p>
              </div>
            </div>

            <div className="space-y-3">
              <div className="flex items-center gap-3 text-gray-700">
                <User className="w-4 h-4 text-gray-500" />
                <span className="font-medium w-32">Username:</span>
                <span>{profile?.username}</span>
              </div>

              <div className="flex items-center gap-3 text-gray-700">
                <Mail className="w-4 h-4 text-gray-500" />
                <span className="font-medium w-32">Email:</span>
                <span>{profile?.email}</span>
              </div>

              <div className="flex items-center gap-3 text-gray-700">
                <Phone className="w-4 h-4 text-gray-500" />
                <span className="font-medium w-32">Phone:</span>
                <span>{profile?.phone || "—"}</span>
              </div>

              <div className="flex items-center gap-3 text-gray-700">
                <Shield className="w-4 h-4 text-gray-500" />
                <span className="font-medium w-32">Role:</span>
                <span className="uppercase text-blue-600 font-semibold">
                  {profile?.role}
                </span>
              </div>

              <div className="flex items-center gap-3 text-gray-700">
                <Calendar className="w-4 h-4 text-gray-500" />
                <span className="font-medium w-32">Created At:</span>
                <span>
                  {new Date(profile?.createdAt).toLocaleDateString("en-GB")}
                </span>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
