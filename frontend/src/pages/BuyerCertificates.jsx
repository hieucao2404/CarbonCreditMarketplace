import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import SidebarBuyer from "../components/BuyerSidebar";
import Header from "../components/BuyerHeader";
import { FileDown, Award, RefreshCw, AlertCircle } from "lucide-react";
// Import the new service
import { certificateService } from "../services/certificateService";

export default function BuyerCertificates() {
  const navigate = useNavigate();
  // State for real data
  const [certificates, setCertificates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedCertificate, setSelectedCertificate] = useState(null);
  const [showDetailsModal, setShowDetailsModal] = useState(false);

  // Load certificates when the page mounts
  useEffect(() => {
    loadCertificates();
  }, []);

  const loadCertificates = async () => {
    setLoading(true);
    setError("");
    try {
      const response = await certificateService.getMyCertificates();
      if (response.success) {
        setCertificates(response.data);
      } else {
        setError(response.message || "Could not load certificates.");
      }
    } catch (err) {
      console.error("Error loading certificates:", err);
      setError(err.response?.data?.message || "An error occurred. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  // Function to download the PDF
  const handleDownload = async (certificateId, certificateCode) => {
    try {
      const blob = await certificateService.downloadCertificate(certificateId);
      
      // Create a temporary link to trigger the browser download
      const url = window.URL.createObjectURL(new Blob([blob]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `certificate-${certificateCode}.pdf`);
      document.body.appendChild(link);
      link.click();
      
      // Clean up the temporary link
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);
      
    } catch (err) {
      console.error("Error downloading certificate:", err);
      setError("Failed to download certificate.");
    }
  };
  
  // Function to show certificate details in modal
  const handleViewDetails = (certificate) => {
    setSelectedCertificate(certificate);
    setShowDetailsModal(true);
  };

  const closeDetailsModal = () => {
    setShowDetailsModal(false);
    setSelectedCertificate(null);
  };

  const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleDateString("vi-VN", {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  };

  return (
    <div className="flex min-h-screen bg-[#F9FAFB]">
      <SidebarBuyer />
      <div className="flex flex-col flex-1">
        <Header />

        <main className="p-8 w-full">
          {/* Header */}
          <div className="flex justify-between items-center mb-6">
            <div>
              <h1 className="text-2xl font-semibold text-gray-800">
                Chứng nhận tín chỉ carbon
              </h1>
              <p className="text-gray-500 text-sm mt-1">
                Chứng nhận giảm phát thải CO₂ cho báo cáo bền vững
              </p>
            </div>
            <button
              onClick={loadCertificates}
              disabled={loading}
              className="flex items-center gap-2 px-4 py-2 bg-white text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition disabled:opacity-50"
            >
              <RefreshCw size={16} className={loading ? "animate-spin" : ""} />
              Làm mới
            </button>
          </div>

          {/* Error Message */}
          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-3">
              <AlertCircle className="w-5 h-5 text-red-600" />
              <p className="text-red-700 text-sm">{error}</p>
            </div>
          )}

          {/* Loading State */}
          {loading && (
            <div className="text-center p-12">
              <p className="text-gray-500">Đang tải chứng nhận...</p>
            </div>
          )}

          {/* Certificates list */}
          {!loading && (
            <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
              {certificates.length === 0 ? (
                <p className="text-gray-500 text-center p-8">
                  Bạn chưa có chứng nhận nào.
                </p>
              ) : (
                certificates.map((cert) => (
                  <div
                    key={cert.id}
                    className="flex items-center justify-between border border-gray-200 rounded-lg bg-gray-50 p-4 hover:bg-gray-100 transition"
                  >
                    {/* Left content */}
                    <div className="flex items-start gap-4">
                      <div className="bg-green-100 p-3 rounded-lg">
                        <Award className="text-green-600 w-6 h-6" />
                      </div>

                      <div>
                        <h2 className="text-base font-semibold text-gray-800">
                          Chứng nhận {cert.certificateCode}
                        </h2>
                        {/* Project name needs to be added to the CertificateDTO on the backend.
                          For now, we'll use a placeholder.
                        */}
                        <p className="text-sm text-gray-500">
                          {cert.project || "Dự án Giảm phát thải EV"}
                        </p>

                        <div className="mt-2 grid grid-cols-2 gap-x-8 gap-y-1 text-sm">
                          <p className="text-gray-600">
                            <span className="font-medium">Số lượng tín chỉ:</span>{" "}
                            {cert.co2ReducedKg} tCO₂
                          </p>
                          <p className="text-gray-600">
                            <span className="font-medium">CO₂ offset:</span>{" "}
                            {(cert.co2ReducedKg * 1000).toLocaleString('vi-VN')} kg CO₂
                          </p>
                          <p className="text-gray-600">
                            <span className="font-medium">Ngày phát hành:</span>{" "}
                            {formatDate(cert.issueDate)}
                          </p>
                          {/* Expiry date is not in the database, so it's removed */}
                        </div>
                      </div>
                    </div>

                    {/* Actions */}
                    <div className="flex flex-col items-end gap-2">
                      <button 
                        onClick={() => handleDownload(cert.id, cert.certificateCode)}
                        className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm hover:bg-gray-100 flex items-center gap-1"
                      >
                        <FileDown size={14} />
                        Tải PDF
                      </button>
                      <button 
                        onClick={() => handleViewDetails(cert)}
                        className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm hover:bg-gray-100"
                      >
                        Xem chi tiết
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}
        </main>
      </div>

      {/* Certificate Details Modal */}
      {showDetailsModal && selectedCertificate && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            {/* Modal Header */}
            <div className="bg-gradient-to-r from-green-500 to-green-600 p-6 rounded-t-xl">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="bg-white p-2 rounded-lg">
                    <Award className="w-8 h-8 text-green-600" />
                  </div>
                  <div>
                    <h2 className="text-2xl font-bold text-white">
                      Chi tiết Chứng nhận
                    </h2>
                    <p className="text-green-100 text-sm mt-1">
                      {selectedCertificate.certificateCode}
                    </p>
                  </div>
                </div>
                <button
                  onClick={closeDetailsModal}
                  className="text-white hover:bg-white hover:bg-opacity-20 rounded-lg p-2 transition"
                >
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>

            {/* Modal Body */}
            <div className="p-6 space-y-6">
              {/* Certificate Information */}
              <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                <h3 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
                  <Award className="w-5 h-5 text-green-600" />
                  Thông tin Chứng nhận
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <p className="text-sm text-gray-500">Mã chứng nhận</p>
                    <p className="text-base font-semibold text-gray-800">
                      {selectedCertificate.certificateCode}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-500">Ngày phát hành</p>
                    <p className="text-base font-semibold text-gray-800">
                      {formatDate(selectedCertificate.issueDate)}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-500">Số lượng tín chỉ</p>
                    <p className="text-base font-semibold text-green-600">
                      {selectedCertificate.co2ReducedKg} tCO₂
                    </p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-500">CO₂ đã giảm</p>
                    <p className="text-base font-semibold text-green-600">
                      {(selectedCertificate.co2ReducedKg * 1000).toLocaleString('vi-VN')} kg CO₂
                    </p>
                  </div>
                </div>
              </div>

              {/* Buyer Information */}
              {selectedCertificate.buyerName && (
                <div className="bg-blue-50 rounded-lg p-4 border border-blue-200">
                  <h3 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
                    <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    </svg>
                    Người sở hữu
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <p className="text-sm text-gray-500">Tên</p>
                      <p className="text-base font-semibold text-gray-800">
                        {selectedCertificate.buyerName}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500">Email</p>
                      <p className="text-base font-semibold text-gray-800">
                        {selectedCertificate.buyerEmail || 'N/A'}
                      </p>
                    </div>
                  </div>
                </div>
              )}

              {/* Credit Information */}
              {selectedCertificate.creditId && (
                <div className="bg-green-50 rounded-lg p-4 border border-green-200">
                  <h3 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
                    <svg className="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    Thông tin Tín chỉ Carbon
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <p className="text-sm text-gray-500">ID Tín chỉ</p>
                      <p className="text-base font-mono text-gray-800 break-all">
                        {selectedCertificate.creditId.substring(0, 16)}...
                      </p>
                    </div>
                  </div>
                </div>
              )}

              {/* Transaction Information */}
              {selectedCertificate.transactionId && (
                <div className="bg-purple-50 rounded-lg p-4 border border-purple-200">
                  <h3 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
                    <svg className="w-5 h-5 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                    </svg>
                    Giao dịch liên quan
                  </h3>
                  <div>
                    <p className="text-sm text-gray-500">ID Giao dịch</p>
                    <p className="text-base font-mono text-gray-800 break-all">
                      {selectedCertificate.transactionId.substring(0, 16)}...
                    </p>
                  </div>
                </div>
              )}

              {/* Impact Summary */}
              <div className="bg-gradient-to-r from-green-50 to-blue-50 rounded-lg p-4 border border-green-200">
                <h3 className="text-lg font-semibold text-gray-800 mb-3 flex items-center gap-2">
                  <svg className="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  Tác động Môi trường
                </h3>
                <p className="text-sm text-gray-600 mb-2">
                  Chứng nhận này xác nhận việc giảm phát thải CO₂ tương đương với:
                </p>
                <ul className="space-y-2 text-sm text-gray-700">
                  <li className="flex items-center gap-2">
                    <span className="w-2 h-2 bg-green-500 rounded-full"></span>
                    {(selectedCertificate.co2ReducedKg * 1000 / 411).toFixed(0)} km lái xe
                  </li>
                  <li className="flex items-center gap-2">
                    <span className="w-2 h-2 bg-green-500 rounded-full"></span>
                    {(selectedCertificate.co2ReducedKg * 40).toFixed(0)} cây xanh trồng mới
                  </li>
                  <li className="flex items-center gap-2">
                    <span className="w-2 h-2 bg-green-500 rounded-full"></span>
                    {(selectedCertificate.co2ReducedKg * 1000 / 8.887).toFixed(0)} kWh điện sạch
                  </li>
                </ul>
              </div>
            </div>

            {/* Modal Footer */}
            <div className="bg-gray-50 px-6 py-4 rounded-b-xl flex justify-end gap-3 border-t">
              <button
                onClick={closeDetailsModal}
                className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition"
              >
                Đóng
              </button>
              <button
                onClick={() => handleDownload(selectedCertificate.id, selectedCertificate.certificateCode)}
                className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition flex items-center gap-2"
              >
                <FileDown size={16} />
                Tải PDF
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}