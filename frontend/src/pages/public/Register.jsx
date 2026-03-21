import React, { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { register } from '../../features/auth/authSlice';
import { useNavigate, Link } from 'react-router-dom';

export default function Register() {
  const [formData, setFormData] = useState({ fullName: '', email: '', password: '' });
  
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { loading, error } = useSelector((state) => state.auth);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const resultAction = await dispatch(register(formData));
    if (register.fulfilled.match(resultAction)) {
      navigate('/verify-email', { state: { email: formData.email } });
    }
  };

  const handleChange = (e) => {
    setFormData({...formData, [e.target.name]: e.target.value});
  }

  return (
    <div className="flex h-screen items-center justify-center bg-gray-50">
      <form onSubmit={handleSubmit} className="w-full max-w-md bg-white p-8 rounded-lg border border-gray-200 shadow-sm">
        <h2 className="text-2xl font-bold mb-6 text-center text-blue-600">Create TrustID</h2>
        
        {error && <div className="bg-red-50 text-red-600 p-3 rounded mb-4 text-sm">{error}</div>}

        <div className="mb-4">
          <label className="block text-sm font-medium mb-1">Full Name</label>
          <input 
            type="text" 
            name="fullName"
            className="w-full p-2 border border-gray-300 rounded focus:border-blue-500 outline-none"
            value={formData.fullName}
            onChange={handleChange}
            required
          />
        </div>

        <div className="mb-4">
          <label className="block text-sm font-medium mb-1">Email</label>
          <input 
            type="email" 
            name="email"
            className="w-full p-2 border border-gray-300 rounded focus:border-blue-500 outline-none"
            value={formData.email}
            onChange={handleChange}
            required
          />
        </div>
        
        <div className="mb-6">
          <label className="block text-sm font-medium mb-1">Password</label>
          <input 
            type="password" 
            name="password"
            className="w-full p-2 border border-gray-300 rounded focus:border-blue-500 outline-none"
            value={formData.password}
            onChange={handleChange}
            required
          />
        </div>

        <button 
          type="submit" 
          disabled={loading}
          className="w-full bg-blue-600 text-white p-2 rounded hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? 'Creating account...' : 'Register'}
        </button>

        <p className="mt-4 text-center text-sm text-gray-600">
          Already have an account? <Link to="/login" className="text-blue-600 hover:underline">Login</Link>
        </p>
      </form>
    </div>
  );
}
