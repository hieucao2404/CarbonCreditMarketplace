import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Mail, ArrowLeft, Leaf, Send, CheckCircle, AlertCircle } from 'lucide-react';

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState('idle'); // idle | success | error
  const [message, setMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validation
    if (!email.trim()) {
      setStatus('error');
      setMessage('Vui lòng nhập địa chỉ email của bạn.');
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      setStatus('error');
      setMessage('Địa chỉ email không hợp lệ. Vui lòng kiểm tra lại.');
      return;
    }

    try {
      setLoading(true);
      setStatus('idle');
      setMessage('');

      const response = await fetch('http://localhost:8080/api/auth/forgot-password', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email }),
      });

      const data = await response.json();

      // Always show success message for security reasons (don't reveal if email exists)
      if (response.ok) {
        setStatus('success');
        setMessage(
          data?.message || 
          '✅ Nếu email này tồn tại trong hệ thống, bạn sẽ nhận được liên kết đặt lại mật khẩu. Vui lòng kiểm tra hộp thư (kể cả thư spam).'
        );
      } else {
        // Still show generic success message for security
        setStatus('success');
        setMessage('✅ Email hướng dẫn đặt lại mật khẩu đã được gửi (nếu email tồn tại trong hệ thống).');
      }
    } catch (error) {
      console.error('Forgot password error:', error);
      setStatus('error');
      setMessage('❌ Không thể kết nối đến máy chủ. Vui lòng thử lại sau.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-50 to-blue-50 flex items-center justify-center p-4">
      {/* Logo Header */}
      <div className="absolute top-8 left-8 flex items-center gap-2">
        <div className="bg-green-100 p-2 rounded-full">
          <Leaf className="w-5 h-5 text-green-600" />
        </div>
        <div>
          <h1 className="text-lg font-semibold text-gray-800">Carbon Credit Exchange</h1>
          <p className="text-xs text-gray-500">Khôi phục mật khẩu</p>
        </div>
      </div>

      <div className="bg-white rounded-2xl shadow-lg p-8 max-w-md w-full">
        {/* Back button */}
        <button
          onClick={() => navigate('/login')}
          className="flex items-center gap-2 text-gray-600 hover:text-gray-800 mb-6 transition"
        >
          <ArrowLeft className="w-4 h-4" />
          <span className="text-sm">Quay lại đăng nhập</span>
        </button>

        {/* Header */}
        <div className="text-center mb-6">
          <div className="bg-blue-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
            <Mail className="w-8 h-8 text-blue-600" />
          </div>
          <h1 className="text-2xl font-bold text-gray-800 mb-2">Quên mật khẩu?</h1>
          <p className="text-gray-600 text-sm">
            Nhập địa chỉ email của bạn và chúng tôi sẽ gửi liên kết đặt lại mật khẩu
          </p>
        </div>

        {/* Success Message */}
        {status === 'success' && (
          <div className="mb-6 bg-green-50 border border-green-200 rounded-lg p-4 flex items-start gap-3">
            <CheckCircle className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
            <div className="text-sm text-green-700">
              <p className="font-medium mb-1">Email đã được gửi! 📧</p>
              <p className="text-xs">{message}</p>
              <div className="mt-3 pt-3 border-t border-green-200">
                <p className="text-xs font-medium mb-1">💡 Lưu ý:</p>
                <ul className="text-xs space-y-1">
                  <li>• Kiểm tra cả thư mục spam/junk</li>
                  <li>• Liên kết có hiệu lực trong 1 giờ</li>
                  <li>• Liên hệ hỗ trợ nếu không nhận được email</li>
                </ul>
              </div>
            </div>
          </div>
        )}

        {/* Error Message */}
        {status === 'error' && (
          <div className="mb-6 bg-red-50 border border-red-200 rounded-lg p-4 flex items-start gap-3">
            <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
            <div className="text-sm text-red-700">
              <p className="font-medium">{message}</p>
            </div>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Địa chỉ Email <span className="text-red-500">*</span>
            </label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="example@email.com"
                disabled={loading}
                className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white py-2.5 rounded-lg hover:bg-blue-700 transition disabled:bg-blue-400 disabled:cursor-not-allowed flex items-center justify-center gap-2 font-medium"
          >
            {loading ? (
              <>
                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                <span>Đang gửi...</span>
              </>
            ) : (
              <>
                <Send className="w-5 h-5" />
                <span>Gửi liên kết đặt lại mật khẩu</span>
              </>
            )}
          </button>
        </form>

        {/* Additional Info */}
        <div className="mt-6 pt-6 border-t border-gray-200">
          <div className="bg-blue-50 rounded-lg p-4">
            <p className="text-sm text-blue-800 font-medium mb-2">🔒 Bảo mật</p>
            <p className="text-xs text-blue-700">
              Chúng tôi không tiết lộ thông tin về việc email có tồn tại trong hệ thống hay không 
              để bảo vệ quyền riêng tư của người dùng.
            </p>
          </div>
        </div>

        {/* Footer Links */}
        <div className="mt-6 text-center space-y-2">
          <p className="text-sm text-gray-600">
            Chưa có tài khoản?{' '}
            <button
              onClick={() => navigate('/register')}
              className="text-blue-600 hover:text-blue-700 font-medium"
            >
              Đăng ký ngay
            </button>
          </p>
          <p className="text-sm text-gray-600">
            Nhớ mật khẩu?{' '}
            <button
              onClick={() => navigate('/login')}
              className="text-blue-600 hover:text-blue-700 font-medium"
            >
              Đăng nhập
            </button>
          </p>
        </div>
      </div>

      {/* Footer */}
      <div className="absolute bottom-4 text-center text-xs text-gray-500">
        <p>Carbon Credit Exchange © 2025</p>
        <p className="mt-1">Hệ thống bảo mật cao • Email xác thực tự động</p>
      </div>
    </div>
  );
}
