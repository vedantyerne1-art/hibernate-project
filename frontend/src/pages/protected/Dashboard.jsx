import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchIdentityProfile } from '../../features/identity/identitySlice';
import { Link, useNavigate } from 'react-router-dom';
import { logout } from '../../features/auth/authSlice';
import axios from '../../api/axios';
import { API_BASE_URL } from '../../api/axios';

const Dashboard = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { user } = useSelector(state => state.auth);
  const { profile, loading } = useSelector(state => state.identity);
  const [qrData, setQrData] = useState(null);
  const [qrLoading, setQrLoading] = useState(false);
  const [qrError, setQrError] = useState('');
  const [hideProfilePhoto, setHideProfilePhoto] = useState(false);
  const [insights, setInsights] = useState(null);

  useEffect(() => {
    dispatch(fetchIdentityProfile());
  }, [dispatch]);

  useEffect(() => {
    const loadInsights = async () => {
      try {
        const response = await axios.get('/identity/insights');
        setInsights(response?.data?.data || null);
      } catch {
        setInsights(null);
      }
    };
    loadInsights();
  }, []);

  useEffect(() => {
    setHideProfilePhoto(false);
  }, [profile?.profilePhotoUrl]);

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  const handleGenerateQr = async () => {
    setQrError('');
    setQrLoading(true);
    try {
      const response = await axios.get('/identity/me/qr-token');
      setQrData(response?.data?.data || null);
    } catch (e) {
      setQrError(e?.response?.data?.message || 'Unable to generate QR right now.');
    } finally {
      setQrLoading(false);
    }
  };

  const isApproved = profile?.status === 'APPROVED';
  const profilePhotoSrc = profile?.profilePhotoUrl
    ? `${API_BASE_URL.replace(/\/api$/, '')}/uploads/${encodeURIComponent(profile.profilePhotoUrl)}`
    : '';
  const verificationQrImageSrc = qrData?.qrCodeBase64
    || (qrData?.verificationUrl
      ? `https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(qrData.verificationUrl)}`
      : '');

  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Sidebar */}
      <aside className="w-64 bg-white border-r h-screen shadow-sm sticky top-0">
        <div className="p-6">
          <h1 className="text-2xl font-bold text-blue-600 tracking-tight">TrustID</h1>
          <p className="text-sm text-gray-500">Digital Identity Platform</p>
        </div>
        <nav className="mt-6 flex flex-col space-y-1 px-4">
          <Link to="/dashboard" className="px-4 py-2 bg-blue-50 text-blue-700 rounded-md font-medium">Dashboard</Link>
          <Link to="/onboarding" className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md">KYC Onboarding</Link>
          <Link to="/locker" className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md">My Locker</Link>
          <Link to="/documents" className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md">Document Vault</Link>
          <Link to="/digital-id" className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md">Digital ID</Link>
          <Link to="/consents" className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md">Linked Apps</Link>
          <Link to="/timeline" className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md">Timeline</Link>
          <Link to="/sessions" className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md">Sessions</Link>
          <Link to="/notifications" className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md">Notifications</Link>
        </nav>
        <div className="absolute bottom-0 w-full p-4 border-t">
          <button onClick={handleLogout} className="w-full text-left px-4 py-2 text-red-600 hover:bg-red-50 rounded-md font-medium">
            Logout
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 p-8">
        <div className="max-w-4xl mx-auto space-y-8">
          <header>
            <h2 className="text-3xl font-bold text-gray-900">Welcome, {user?.fullName || user?.firstName || 'User'}</h2>
            <p className="text-gray-500 mt-1">Manage your digital identity and credentials.</p>
          </header>

          {insights && (
            <section className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
              <div className="grid sm:grid-cols-3 gap-4">
                <div className="rounded-lg border p-3 bg-slate-50">
                  <p className="text-xs text-slate-500 uppercase">Trust Score</p>
                  <p className="text-2xl font-bold text-blue-700">{insights.trustScore}/100</p>
                </div>
                <div className="rounded-lg border p-3 bg-slate-50">
                  <p className="text-xs text-slate-500 uppercase">Identity Level</p>
                  <p className="text-lg font-semibold">{insights.identityLevel}</p>
                </div>
                <div className="rounded-lg border p-3 bg-slate-50">
                  <p className="text-xs text-slate-500 uppercase">Risk</p>
                  <p className={`text-lg font-semibold ${insights.riskLevel === 'HIGH' ? 'text-red-600' : insights.riskLevel === 'MEDIUM' ? 'text-amber-600' : 'text-green-600'}`}>{insights.riskLevel}</p>
                </div>
              </div>

              <div className="mt-4 grid sm:grid-cols-2 gap-4">
                <div>
                  <p className="font-semibold mb-2">Smart Suggestions</p>
                  <ul className="space-y-1 text-sm text-slate-700">
                    {(insights.suggestions || []).map((s, i) => <li key={i}>- {s}</li>)}
                  </ul>
                </div>
                <div>
                  <p className="font-semibold mb-2">Security Alerts</p>
                  <ul className="space-y-1 text-sm text-slate-700">
                    {(insights.alerts || []).length === 0 && <li>- No active alerts</li>}
                    {(insights.alerts || []).map((s, i) => <li key={i}>- {s}</li>)}
                  </ul>
                </div>
              </div>

              <div className="mt-4 flex flex-wrap gap-3 text-sm">
                <Link to="/timeline" className="px-3 py-1 rounded bg-blue-50 text-blue-700 hover:bg-blue-100">View Timeline</Link>
                <Link to="/sessions" className="px-3 py-1 rounded bg-blue-50 text-blue-700 hover:bg-blue-100">Manage Sessions</Link>
                <Link to="/notifications" className="px-3 py-1 rounded bg-blue-50 text-blue-700 hover:bg-blue-100">Notification Center</Link>
              </div>
            </section>
          )}

          <section className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            <h3 className="text-xl font-semibold mb-4">Identity Profile</h3>
            {loading ? (
              <p className="text-gray-500">Loading profile...</p>
            ) : profile ? (
              <div className="grid grid-cols-1 sm:grid-cols-[88px_1fr] gap-4">
                <div>
                  {!hideProfilePhoto && profilePhotoSrc && (
                    <img
                      src={profilePhotoSrc}
                      alt="Profile"
                      className="w-[88px] h-[88px] rounded-full object-cover border border-gray-200"
                      onError={() => setHideProfilePhoto(true)}
                    />
                  )}
                </div>
                <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-sm text-gray-500">TrustID Number</p>
                  <p className="font-medium text-lg text-blue-700">{profile.identityNumber || 'Pending allocation'}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Profile Owner</p>
                  <p className="font-medium">{profile.fullName}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Current Status</p>
                  <span className={`px-2 py-1 text-xs rounded-full font-medium ${
                    isApproved ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'
                  }`}>
                    {profile.status}
                  </span>
                </div>
                </div>
              </div>
            ) : (
               <div className="text-center py-8">
                 <p className="text-gray-600 mb-4">You have not set up your identity profile yet.</p>
                 <Link to="/profile-setup" className="inline-flex px-4 py-2 bg-blue-600 text-white rounded shadow hover:bg-blue-700">
                   Create Profile
                 </Link>
               </div>
            )}
          </section>

          {profile && (
            <section className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
              <h3 className="text-xl font-semibold mb-4 border-b pb-2">Identity QR</h3>
              <p className="text-gray-600 mb-4">Generate a temporary QR that links to your public verification summary.</p>

              <div className="flex items-center gap-4 flex-wrap">
                <button
                  onClick={handleGenerateQr}
                  disabled={qrLoading}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded font-medium shadow-sm disabled:opacity-50"
                >
                  {qrLoading ? 'Generating...' : 'Generate Verification QR'}
                </button>
                {qrData?.expiresAt && <span className="text-sm text-gray-500">Expires at: {new Date(qrData.expiresAt).toLocaleString()}</span>}
              </div>

              {qrError && <p className="mt-4 text-sm text-red-600">{qrError}</p>}

              {qrData?.verificationUrl && (
                <div className="mt-6 grid sm:grid-cols-[180px_1fr] gap-6 items-start">
                  <img
                    alt="Identity QR"
                    className="w-[180px] h-[180px] border rounded"
                    src={verificationQrImageSrc}
                  />
                  <div>
                    <p className="text-sm text-gray-500 mb-2">Verification URL</p>
                    <a href={qrData.verificationUrl} target="_blank" rel="noreferrer" className="break-all text-blue-600 hover:underline">
                      {qrData.verificationUrl}
                    </a>
                  </div>
                </div>
              )}
            </section>
          )}

          {profile && !isApproved && (
             <section className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
               <h3 className="text-xl font-semibold mb-4 border-b pb-2">Actions Required</h3>
               <p className="text-gray-600 mb-4">You need to upload at least one National ID in your Document Vault and submit a KYC Verification request to activate your TrustID.</p>
               <div className="flex space-x-4">
                 <Link to="/documents" className="px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-800 rounded font-medium">Go to Vault</Link>
                 <button onClick={async () => {
                   try {
                     // Get user docs
                     const docsRes = await axios.get('/documents/my');
                     const docs = docsRes?.data?.data || [];
                     
                     if (docs.length === 0) {
                        alert("You must upload documents first!");
                        return;
                     }
                     
                     await axios.post('/verification/submit', { documentIds: docs.map(d => d.id) });
                     
                     alert("Verification submitted successfully. Waiting for admin approval.");
                     window.location.reload();
                   } catch(e) {
                     const message = e?.response?.data?.message || e?.message || 'Failed to submit verification';
                     alert(message);
                     console.error(e);
                   }
                 }} className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded font-medium shadow-sm">Submit KYC for Review</button>
               </div>
             </section>
          )}
        </div>
      </main>
    </div>
  );
};

export default Dashboard;