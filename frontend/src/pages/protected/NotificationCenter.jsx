import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import axios from '../../api/axios';

export default function NotificationCenter() {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const response = await axios.get('/notifications');
      setNotifications(response?.data?.data || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const markRead = async (id) => {
    await axios.patch(`/notifications/${id}/read`);
    await load();
  };

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="max-w-4xl mx-auto bg-white border rounded-xl p-6 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <h1 className="text-2xl font-bold">Notification Center</h1>
          <Link to="/dashboard" className="text-blue-600 hover:underline">Back to Dashboard</Link>
        </div>

        {loading && <p className="text-gray-500">Loading notifications...</p>}

        {!loading && notifications.length === 0 && <p className="text-gray-500">No notifications yet.</p>}

        <div className="space-y-3">
          {notifications.map((item) => (
            <div key={item.id} className={`border rounded p-3 ${item.read ? 'bg-slate-50' : 'bg-blue-50 border-blue-200'}`}>
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="font-semibold text-sm">{item.title}</p>
                  <p className="text-xs text-slate-500">{item.type} · {new Date(item.createdAt).toLocaleString()}</p>
                </div>
                {!item.read && (
                  <button className="text-xs px-2 py-1 bg-blue-600 text-white rounded" onClick={() => markRead(item.id)}>
                    Mark read
                  </button>
                )}
              </div>
              <p className="mt-2 text-sm text-slate-700">{item.message}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
