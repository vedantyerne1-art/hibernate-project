import React, { useState } from 'react';
import axios from '../../api/axios';
import { useLocation } from 'react-router-dom';

const VerifyEmail = () => {
  const location = useLocation();
  const navEmail = location.state?.email || '';
  const storedUser = (() => {
    try {
      return JSON.parse(localStorage.getItem('user') || 'null');
    } catch {
      return null;
    }
  })();
  const [email, setEmail] = useState(navEmail || storedUser?.email || '');
  const [otp, setOtp] = useState('');
  const [step, setStep] = useState(1);
  const [msg, setMsg] = useState('');

  const sendOtp = async () => {
    try {
      await axios.post('/auth/send-verification', { email });
      setStep(2);
      setMsg('OTP sent to your email. If Gmail is not configured, check Mailpit at http://localhost:8025');
    } catch (e) {
      setMsg(e.response?.data?.message || e.response?.data?.error || 'Error occurred');
    }
  };

  const verifyOtp = async () => {
    try {
      await axios.post('/auth/verify-email', { email, otp });
      setMsg('Email successfully verified. You can now login.');
      setStep(3);
    } catch (e) {
      setMsg(e.response?.data?.message || e.response?.data?.error || 'Error occurred');
    }
  };

  return (
    <div className="flex justify-center items-center h-screen space-y-4">
      <div className="p-8 bg-white shadow-lg rounded max-w-md w-full">
        <h2 className="text-2xl mb-4">Verify Email</h2>
        {msg && <p className="text-sm text-green-600 mb-4">{msg}</p>}
        {step === 1 && (
          <div>
            <input 
              type="email" 
              className="border p-2 w-full mb-4" 
              placeholder="Email" 
              value={email} 
              onChange={e => setEmail(e.target.value)} 
            />
            <button className="bg-blue-600 text-white w-full py-2" onClick={sendOtp}>Send OTP</button>
          </div>
        )}
        {step === 2 && (
          <div>
            <input 
              type="text" 
              className="border p-2 w-full mb-4" 
              placeholder="OTP Code" 
              value={otp} 
              onChange={e => setOtp(e.target.value)} 
            />
            <button className="bg-green-600 text-white w-full py-2" onClick={verifyOtp}>Verify Email</button>
          </div>
        )}
        {step === 3 && (
          <a href="/login" className="text-blue-500 text-center block w-full">Go to Login</a>
        )}
      </div>
    </div>
  );
};
export default VerifyEmail;