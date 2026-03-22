import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import axios from '../../api/axios';

export default function SessionManagement() {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const response = await axios.get('/sessions');
      setSessions(response?.data?.data || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const logoutOthers = async () => {
    await axios.post('/sessions/logout-others');
    await load();
  };

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="max-w-4xl mx-auto bg-white border rounded-xl p-6 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <h1 className="text-2xl font-bold">Session Management</h1>
          <Link to="/dashboard" className="text-blue-600 hover:underline">Back to Dashboard</Link>
        </div>

        <div className="mb-4">
          <button className="px-4 py-2 bg-red-600 text-white rounded" onClick={logoutOthers}>Logout Other Devices</button>
        </div>

        {loading && <p className="text-gray-500">Loading sessions...</p>}

        <div className="space-y-3">
          {sessions.map((session) => (
            <div key={session.id} className="border rounded p-3 bg-slate-50">
              <p className="font-medium">{session.deviceName || 'Unknown Device'}</p>
              <p className="text-xs text-slate-500">IP: {session.ipAddress || '-'} · Last active: {new Date(session.lastActiveAt).toLocaleString()}</p>
              <p className="text-xs text-slate-500 truncate">{session.userAgent || '-'}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
