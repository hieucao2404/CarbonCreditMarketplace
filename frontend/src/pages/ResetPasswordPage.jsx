import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Lock, Eye, EyeOff, CheckCircle, AlertCircle, Leaf, Loader, Shield } from 'lucide-react';

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  
  const [tokenStatus, setTokenStatus] = useState('verifying'); // verifying | valid | invalid
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState('idle'); // idle | success | error
  const [message, setMessage] = useState('');
  const [passwordStrength, setPasswordStrength] = useState(0);

  const token = searchParams.get('token');

  useEffect(() => {
    if (!token) {
      setTokenStatus('invalid');
      setMessage('❌ Token không tồn tại. Vui lòng yêu cầu đặt lại mật khẩu lại.');
      return;
    }

    verifyToken(token);
  }, [token]);

  useEffect(() => {
    calculatePasswordStrength(password);
  }, [password]);

  const verifyToken = async (token) => {
    try {
      const response = await fetch(`http://localhost:8080/api/auth/verify-reset-token?token=${token}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      const data = await response.json();

      if (response.ok && data?.success) {
        setTokenStatus('valid');
      } else {
        setTokenStatus('invalid');
        setMessage(data?.message || '❌ Token không hợp lệ hoặc đã hết hạn.');
      }
    } catch (error) {
      console.error('Token verification error:', error);
      setTokenStatus('invalid');
      setMessage('❌ Không thể xác thực token. Vui lòng thử lại sau.');
    }
  };

  const calculatePasswordStrength = (pwd) => {
    let strength = 0;
    if (pwd.length >= 8) strength++;
    if (pwd.length >= 12) strength++;
    if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) strength++;
    if (/\d/.test(pwd)) strength++;
    if (/[^a-zA-Z0-9]/.test(pwd)) strength++;
    setPasswordStrength(strength);
  };

  const getPasswordStrengthLabel = () => {
    if (passwordStrength === 0) return { text: '', color: '' };
    if (passwordStrength <= 2) return { text: 'Yếu', color: 'text-red-600' };
    if (passwordStrength <= 3) return { text: 'Trung bình', color: 'text-yellow-600' };
    if (passwordStrength <= 4) return { text: 'Mạnh', color: 'text-green-600' };
    return { text: 'Rất mạnh', color: 'text-green-700' };
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validation
    if (!password.trim() || !confirmPassword.trim()) {
      setStatus('error');
      setMessage('Vui lòng nhập đầy đủ thông tin.');
      return;
    }

    if (password.length < 8) {
      setStatus('error');
      setMessage('Mật khẩu phải có ít nhất 8 ký tự.');
      return;
    }

    if (password !== confirmPassword) {
      setStatus('error');
      setMessage('Mật khẩu xác nhận không khớp.');
      return;
    }

    try {
      setLoading(true);
      setStatus('idle');
      setMessage('');

      const response = await fetch('http://localhost:8080/api/auth/reset-password', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          token: token,
          newPassword: password,
        }),
      });

      const data = await response.json();

      if (response.ok && data?.success) {
        setStatus('success');
        setMessage(data?.message || '✅ Mật khẩu đã được đặt lại thành công!');
        
        // Redirect to login after 3 seconds
        setTimeout(() => {
          navigate('/login', {
            state: { 
              successMessage: 'Mật khẩu đã được đặt lại. Vui lòng đăng nhập với mật khẩu mới!' 
            }
          });
        }, 3000);
      } else {
        setStatus('error');
        setMessage(data?.message || '❌ Không thể đặt lại mật khẩu. Vui lòng thử lại.');
      }
    } catch (error) {
      console.error('Reset password error:', error);
      setStatus('error');
      setMessage('❌ Không thể kết nối đến máy chủ. Vui lòng thử lại sau.');
    } finally {
      setLoading(false);
    }
  };

  // Token Verifying State
  if (tokenStatus === 'verifying') {
    return (
      <div className="min-h-screen bg-gradient-to-br from-green-50 to-blue-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-2xl shadow-lg p-8 max-w-md w-full text-center">
          <div className="relative w-16 h-16 mx-auto mb-4">
            <Shield className="w-16 h-16 text-blue-500 animate-pulse" />
            <Loader className="w-8 h-8 text-blue-600 absolute top-4 left-4 animate-spin" />
          </div>
          <h1 className="text-2xl font-bold text-gray-800 mb-2">Đang xác thực...</h1>
          <p className="text-gray-600">Vui lòng chờ trong giây lát</p>
        </div>
      </div>
    );
  }

  // Invalid Token State
  if (tokenStatus === 'invalid') {
    return (
      <div className="min-h-screen bg-gradient-to-br from-green-50 to-blue-50 flex items-center justify-center p-4">
        <div className="absolute top-8 left-8 flex items-center gap-2">
          <div className="bg-green-100 p-2 rounded-full">
            <Leaf className="w-5 h-5 text-green-600" />
          </div>
          <div>
            <h1 className="text-lg font-semibold text-gray-800">Carbon Credit Exchange</h1>
            <p className="text-xs text-gray-500">Đặt lại mật khẩu</p>
          </div>
        </div>

        <div className="bg-white rounded-2xl shadow-lg p-8 max-w-md w-full text-center">
          <AlertCircle className="w-16 h-16 text-red-500 mx-auto mb-4" />
          <h1 className="text-2xl font-bold text-red-600 mb-2">Token không hợp lệ</h1>
          <p className="text-gray-600 mb-6">{message}</p>

          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
            <p className="text-sm text-yellow-800 mb-2">💡 <strong>Gợi ý:</strong></p>
            <ul className="text-xs text-yellow-700 text-left space-y-1">
              <li>• Token có thể đã hết hạn (1 giờ)</li>
              <li>• Token đã được sử dụng rồi</li>
              <li>• Liên kết không đúng định dạng</li>
            </ul>
          </div>

          <div className="space-y-2">
            <button
              onClick={() => navigate('/forgot-password')}
              className="w-full px-4 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition font-medium"
            >
              Yêu cầu đặt lại mật khẩu lại
            </button>
            <button
              onClick={() => navigate('/login')}
              className="w-full px-4 py-2.5 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition"
            >
              Quay lại đăng nhập
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Valid Token - Show Reset Form
  return (
    <div className="min-h-screen bg-gradient-to-br from-green-50 to-blue-50 flex items-center justify-center p-4">
      {/* Logo Header */}
      <div className="absolute top-8 left-8 flex items-center gap-2">
        <div className="bg-green-100 p-2 rounded-full">
          <Leaf className="w-5 h-5 text-green-600" />
        </div>
        <div>
          <h1 className="text-lg font-semibold text-gray-800">Carbon Credit Exchange</h1>
          <p className="text-xs text-gray-500">Đặt lại mật khẩu</p>
        </div>
      </div>

      <div className="bg-white rounded-2xl shadow-lg p-8 max-w-md w-full">
        {/* Header */}
        <div className="text-center mb-6">
          <div className="bg-green-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
            <Lock className="w-8 h-8 text-green-600" />
          </div>
          <h1 className="text-2xl font-bold text-gray-800 mb-2">Đặt lại mật khẩu</h1>
          <p className="text-gray-600 text-sm">
            Nhập mật khẩu mới cho tài khoản của bạn
          </p>
        </div>

        {/* Success Message */}
        {status === 'success' && (
          <div className="mb-6 bg-green-50 border border-green-200 rounded-lg p-4 flex items-start gap-3">
            <CheckCircle className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
            <div className="text-sm text-green-700">
              <p className="font-medium mb-1">Thành công! 🎉</p>
              <p className="text-xs">{message}</p>
              <p className="text-xs mt-2 animate-pulse">Đang chuyển hướng đến trang đăng nhập...</p>
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
          {/* New Password */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Mật khẩu mới <span className="text-red-500">*</span>
            </label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Ít nhất 8 ký tự"
                disabled={loading || status === 'success'}
                className="w-full pl-10 pr-12 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
              >
                {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
              </button>
            </div>
            
            {/* Password Strength Indicator */}
            {password && (
              <div className="mt-2">
                <div className="flex gap-1 mb-1">
                  {[1, 2, 3, 4, 5].map((level) => (
                    <div
                      key={level}
                      className={`h-1 flex-1 rounded ${
                        level <= passwordStrength
                          ? passwordStrength <= 2
                            ? 'bg-red-500'
                            : passwordStrength <= 3
                            ? 'bg-yellow-500'
                            : 'bg-green-500'
                          : 'bg-gray-200'
                      }`}
                    />
                  ))}
                </div>
                <p className={`text-xs ${getPasswordStrengthLabel().color}`}>
                  Độ mạnh: {getPasswordStrengthLabel().text}
                </p>
              </div>
            )}
          </div>

          {/* Confirm Password */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Xác nhận mật khẩu <span className="text-red-500">*</span>
            </label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type={showConfirmPassword ? 'text' : 'password'}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Nhập lại mật khẩu mới"
                disabled={loading || status === 'success'}
                className="w-full pl-10 pr-12 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
              >
                {showConfirmPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
              </button>
            </div>
            
            {/* Password Match Indicator */}
            {confirmPassword && (
              <p className={`text-xs mt-1 ${password === confirmPassword ? 'text-green-600' : 'text-red-600'}`}>
                {password === confirmPassword ? '✓ Mật khẩu khớp' : '✗ Mật khẩu không khớp'}
              </p>
            )}
          </div>

          {/* Password Requirements */}
          <div className="bg-blue-50 rounded-lg p-3">
            <p className="text-xs text-blue-800 font-medium mb-2">📋 Yêu cầu mật khẩu:</p>
            <ul className="text-xs text-blue-700 space-y-1">
              <li className={password.length >= 8 ? 'text-green-600' : ''}>
                {password.length >= 8 ? '✓' : '•'} Ít nhất 8 ký tự
              </li>
              <li className={/[A-Z]/.test(password) ? 'text-green-600' : ''}>
                {/[A-Z]/.test(password) ? '✓' : '•'} Ít nhất 1 chữ hoa
              </li>
              <li className={/[a-z]/.test(password) ? 'text-green-600' : ''}>
                {/[a-z]/.test(password) ? '✓' : '•'} Ít nhất 1 chữ thường
              </li>
              <li className={/\d/.test(password) ? 'text-green-600' : ''}>
                {/\d/.test(password) ? '✓' : '•'} Ít nhất 1 số
              </li>
            </ul>
          </div>

          <button
            type="submit"
            disabled={loading || status === 'success'}
            className="w-full bg-green-600 text-white py-2.5 rounded-lg hover:bg-green-700 transition disabled:bg-green-400 disabled:cursor-not-allowed flex items-center justify-center gap-2 font-medium"
          >
            {loading ? (
              <>
                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                <span>Đang xử lý...</span>
              </>
            ) : (
              <>
                <CheckCircle className="w-5 h-5" />
                <span>Đặt lại mật khẩu</span>
              </>
            )}
          </button>
        </form>

        {/* Footer Link */}
        <div className="mt-6 text-center">
          <p className="text-sm text-gray-600">
            Nhớ mật khẩu?{' '}
            <button
              onClick={() => navigate('/login')}
              className="text-green-600 hover:text-green-700 font-medium"
            >
              Đăng nhập ngay
            </button>
          </p>
        </div>
      </div>

      {/* Footer */}
      <div className="absolute bottom-4 text-center text-xs text-gray-500">
        <p>Carbon Credit Exchange © 2025</p>
        <p className="mt-1">Bảo mật thông tin • Mã hóa an toàn</p>
      </div>
    </div>
  );
}
