import React from "react";

export default function StatCard({ title, value, sub }) {
  return (
    <div className="bg-white p-4 rounded-xl shadow-sm">
      <h3 className="text-sm text-gray-500 mb-1">{title}</h3>
      <p className="text-xl font-semibold">{value}</p>
      <p className="text-xs text-gray-500 mt-1">{sub}</p>
    </div>
  );
}
