import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import axios from '../../api/axios';
import { API_BASE_URL } from '../../api/axios';

export default function PublicVerifyToken() {
  const { token } = useParams();
  const [state, setState] = useState({ loading: true, err: '', data: null });
  const [photoHidden, setPhotoHidden] = useState(false);

  useEffect(() => {
    setPhotoHidden(false);
    const run = async () => {
      try {
        const res = await axios.get(`/public/verify/${token}`);
        setState({ loading: false, err: '', data: res?.data?.data || null });
      } catch (e) {
        setState({ loading: false, err: e?.response?.data?.message || 'Invalid token', data: null });
      }
    };
    run();
  }, [token]);

  const photoUrl = token ? `${API_BASE_URL}/public/verify/${encodeURIComponent(token)}/photo` : '';

  return (
    <div className="min-h-screen bg-slate-100 flex items-center justify-center p-6">
      <div className="max-w-md w-full bg-white rounded-xl border p-6 shadow-sm">
        <h1 className="text-xl font-bold mb-3">Public Verification</h1>
        {state.loading && <p>Checking token...</p>}
        {state.err && <p className="text-red-600">{state.err}</p>}
        {state.data && (
          <div className="space-y-2 text-sm">
            {!photoHidden && photoUrl && (
              <img
                alt="User profile"
                src={photoUrl}
                className="w-20 h-20 rounded-full object-cover border border-slate-200"
                onError={() => setPhotoHidden(true)}
              />
            )}
            <p><b>Name:</b> {state.data.fullName}</p>
            <p><b>Identity No:</b> {state.data.identityNumber || 'Pending'}</p>
            <p><b>Status:</b> {state.data.status}</p>
            <p><b>Issue Date:</b> {state.data.issueDate || '-'}</p>
          </div>
        )}
        <div className="mt-4"><Link to="/login" className="text-blue-600 hover:underline">Back to login</Link></div>
      </div>
    </div>
  );
}
