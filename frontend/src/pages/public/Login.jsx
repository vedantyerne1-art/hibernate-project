import React, { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { login } from '../../features/auth/authSlice';
import { useNavigate, Link } from 'react-router-dom';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { loading, error } = useSelector((state) => state.auth);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const resultAction = await dispatch(login({ email, password }));
    if (login.fulfilled.match(resultAction)) {
      const role = resultAction?.payload?.role;
      if (role === 'ADMIN' || role === 'SUPER_ADMIN') {
        navigate('/admin/dashboard');
      } else {
        navigate('/dashboard');
      }
    }
  };

  return (
    <div className="flex h-screen items-center justify-center bg-gray-50">
      <form onSubmit={handleSubmit} className="w-full max-w-md bg-white p-8 rounded-lg border border-gray-200 shadow-sm">
        <h2 className="text-2xl font-bold mb-6 text-center text-blue-600">TrustID Login</h2>
        
        {error && <div className="bg-red-50 text-red-600 p-3 rounded mb-4 text-sm">{error}</div>}

        <div className="mb-4">
          <label className="block text-sm font-medium mb-1">Email</label>
          <input 
            type="email" 
            className="w-full p-2 border border-gray-300 rounded focus:border-blue-500 outline-none"
            value={email}
            onChange={(e)=>setEmail(e.target.value)}
            required
          />
        </div>
        
        <div className="mb-6">
          <label className="block text-sm font-medium mb-1">Password</label>
          <input 
            type="password" 
            className="w-full p-2 border border-gray-300 rounded focus:border-blue-500 outline-none"
            value={password}
            onChange={(e)=>setPassword(e.target.value)}
            required
          />
        </div>

        <button 
          type="submit" 
          disabled={loading}
          className="w-full bg-blue-600 text-white p-2 rounded hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? 'Logging in...' : 'Login'}
        </button>

        <p className="mt-4 text-center text-sm text-gray-600">
          Don't have an account? <Link to="/register" className="text-blue-600 hover:underline">Register here</Link>
        </p>
      </form>
    </div>
  );
}
