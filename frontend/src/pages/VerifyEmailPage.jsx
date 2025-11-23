import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { CheckCircle, AlertCircle, Clock, Leaf, Loader } from 'lucide-react';

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState('verifying'); // verifying | success | error
  const [message, setMessage] = useState('');
  const [email, setEmail] = useState('');

  useEffect(() => {
    const token = searchParams.get('token');

    if (!token) {
      setStatus('error');
      setMessage('❌ Token xác thực không tìm thấy. Vui lòng kiểm tra lại liên kết trong email.');
      return;
    }

    verifyEmail(token);
  }, [searchParams]);

  const verifyEmail = async (token) => {
    try {
      setStatus('verifying');
      
      const response = await fetch('http://localhost:8080/api/auth/verify-email?token=' + token, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      const data = await response.json();

      if (response.ok && data?.success) {
        setStatus('success');
        setMessage(data?.message || '✅ Email của bạn đã được xác thực thành công!');

        // Redirect to login after 3 seconds
        setTimeout(() => {
          navigate('/login', {
            state: { 
              successMessage: 'Email đã được xác thực. Bạn có thể đăng nhập ngay!' 
            }
          });
        }, 3000);
      } else {
        setStatus('error');
        setMessage(data?.message || '❌ Xác thực thất bại. Token có thể đã hết hạn hoặc không hợp lệ.');
      }
    } catch (error) {
      console.error('Verification error:', error);
      setStatus('error');
      setMessage('❌ Không thể kết nối đến máy chủ. Vui lòng thử lại sau.');
    }
  };

  const handleResendEmail = async () => {
    if (!email) {
      alert('Vui lòng nhập email của bạn!');
      return;
    }

    try {
      const response = await fetch('http://localhost:8080/api/auth/resend-verification', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email }),
      });

      const data = await response.json();

      if (response.ok && data?.success) {
        alert('✅ Email xác thực đã được gửi lại! Vui lòng kiểm tra hộp thư.');
      } else {
        alert(data?.message || '❌ Không thể gửi lại email. Vui lòng thử lại sau.');
      }
    } catch (error) {
      console.error('Resend email error:', error);
      alert('❌ Không thể kết nối đến máy chủ.');
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
          <p className="text-xs text-gray-500">Xác thực Email</p>
        </div>
      </div>

      <div className="bg-white rounded-2xl shadow-lg p-8 max-w-md w-full text-center">

        {status === 'verifying' && (
          <>
            <div className="relative w-16 h-16 mx-auto mb-4">
              <Clock className="w-16 h-16 text-blue-500 animate-pulse" />
              <Loader className="w-8 h-8 text-blue-600 absolute top-4 left-4 animate-spin" />
            </div>
            <h1 className="text-2xl font-bold text-gray-800 mb-2">Đang xác thực...</h1>
            <p className="text-gray-600">Vui lòng chờ trong giây lát</p>
            <div className="mt-4">
              <div className="animate-pulse flex space-x-2 justify-center">
                <div className="h-2 w-2 bg-blue-500 rounded-full"></div>
                <div className="h-2 w-2 bg-blue-500 rounded-full animation-delay-200"></div>
                <div className="h-2 w-2 bg-blue-500 rounded-full animation-delay-400"></div>
              </div>
            </div>
          </>
        )}

        {status === 'success' && (
          <>
            <div className="relative">
              <CheckCircle className="w-16 h-16 text-green-500 mx-auto mb-4 animate-bounce" />
              <div className="absolute inset-0 flex items-center justify-center">
                <div className="w-20 h-20 bg-green-100 rounded-full animate-ping opacity-75"></div>
              </div>
            </div>
            <h1 className="text-2xl font-bold text-green-600 mb-2">Xác thực thành công! 🎉</h1>
            <p className="text-gray-600 mb-4">{message}</p>
            <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-4">
              <p className="text-sm text-green-700">
                ✅ Email của bạn đã được xác thực<br/>
                🚀 Bạn có thể đăng nhập ngay bây giờ
              </p>
            </div>
            <p className="text-sm text-gray-500 animate-pulse">
              Đang chuyển hướng đến trang đăng nhập...
            </p>
          </>
        )}

        {status === 'error' && (
          <>
            <AlertCircle className="w-16 h-16 text-red-500 mx-auto mb-4" />
            <h1 className="text-2xl font-bold text-red-600 mb-2">Xác thực thất bại</h1>
            <p className="text-gray-600 mb-6">{message}</p>

            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
              <p className="text-sm text-yellow-800 mb-3">
                💡 <strong>Gợi ý:</strong>
              </p>
              <ul className="text-xs text-yellow-700 text-left space-y-1">
                <li>• Token có thể đã hết hạn (24 giờ)</li>
                <li>• Bạn đã xác thực email rồi</li>
                <li>• Liên kết không đúng định dạng</li>
              </ul>
            </div>

            <div className="space-y-3">
              {/* Resend verification form */}
              <div className="text-left">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Gửi lại email xác thực
                </label>
                <div className="flex gap-2">
                  <input
                    type="email"
                    placeholder="Nhập email của bạn"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                  />
                  <button
                    onClick={handleResendEmail}
                    className="px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 transition whitespace-nowrap"
                  >
                    Gửi lại
                  </button>
                </div>
              </div>

              <div className="flex gap-2">
                <button
                  onClick={() => navigate('/register')}
                  className="flex-1 px-4 py-2 bg-gray-100 text-gray-700 text-sm rounded-lg hover:bg-gray-200 transition"
                >
                  Đăng ký lại
                </button>
                <button
                  onClick={() => navigate('/login')}
                  className="flex-1 px-4 py-2 bg-green-600 text-white text-sm rounded-lg hover:bg-green-700 transition"
                >
                  Đến trang đăng nhập
                </button>
              </div>
            </div>
          </>
        )}
      </div>

      {/* Footer */}
      <div className="absolute bottom-4 text-center text-xs text-gray-500">
        <p>Carbon Credit Exchange © 2025</p>
        <p className="mt-1">Nền tảng mua bán tín chỉ carbon từ xe điện</p>
      </div>
    </div>
  );
}
