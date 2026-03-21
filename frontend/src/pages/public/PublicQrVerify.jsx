import React, { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import api from '../../api/axios';
import { API_BASE_URL } from '../../api/axios';

export default function PublicQrVerify() {
  const [params] = useSearchParams();
  const token = params.get('token');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  const [photoHidden, setPhotoHidden] = useState(false);

  useEffect(() => {
    setPhotoHidden(false);

    if (!token) {
      setLoading(false);
      setError('Missing QR token.');
      return;
    }

    const resolveToken = async () => {
      try {
        const response = await api.get(`/public/qr/resolve?token=${encodeURIComponent(token)}`);
        setResult(response?.data?.data || null);
      } catch (e) {
        setError(e?.response?.data?.message || 'Invalid or expired QR token.');
      } finally {
        setLoading(false);
      }
    };

    resolveToken();
  }, [params, token]);

  const photoUrl = token ? `${API_BASE_URL}/public/qr/photo?token=${encodeURIComponent(token)}` : '';

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center px-4">
      <div className="w-full max-w-lg bg-white rounded-xl shadow-sm border border-slate-200 p-8">
        <h1 className="text-2xl font-bold text-slate-900 mb-2">TrustID QR Verification</h1>
        <p className="text-sm text-slate-500 mb-6">Scan result for shared identity QR.</p>

        {loading && <p className="text-slate-600">Validating token...</p>}

        {!loading && error && (
          <div className="rounded-md border border-red-200 bg-red-50 p-4 text-red-700">
            {error}
          </div>
        )}

        {!loading && !error && result && (
          <div className="space-y-4">
            {!photoHidden && photoUrl && (
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-500 mb-2">Profile Photo</p>
                <img
                  alt="User profile"
                  src={photoUrl}
                  className="w-24 h-24 rounded-full object-cover border border-slate-200"
                  onError={() => setPhotoHidden(true)}
                />
              </div>
            )}

            <div>
              <p className="text-xs uppercase tracking-wide text-slate-500">Full Name</p>
              <p className="text-lg font-semibold text-slate-900">{result.fullName || 'N/A'}</p>
            </div>

            <div>
              <p className="text-xs uppercase tracking-wide text-slate-500">Identity Number</p>
              <p className="text-slate-900">{result.identityNumber || 'Not issued yet'}</p>
            </div>

            <div>
              <p className="text-xs uppercase tracking-wide text-slate-500">KYC Status</p>
              <span className={`inline-flex px-2 py-1 rounded text-xs font-medium ${result.verified ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'}`}>
                {result.status}
              </span>
            </div>

            <div>
              <p className="text-xs uppercase tracking-wide text-slate-500">Verification Result</p>
              <p className={`font-semibold ${result.verified ? 'text-green-700' : 'text-amber-700'}`}>
                {result.verified ? 'Approved identity' : 'Not yet approved'}
              </p>
            </div>
          </div>
        )}

        <div className="mt-8 border-t pt-4 text-sm">
          <Link to="/login" className="text-blue-600 hover:underline">Back to Login</Link>
        </div>
      </div>
    </div>
  );
}
