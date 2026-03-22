import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import axios from '../../api/axios';

export default function ActivityTimeline() {
  const [timeline, setTimeline] = useState([]);
  const [accessLogs, setAccessLogs] = useState([]);

  useEffect(() => {
    const load = async () => {
      const [timelineRes, accessRes] = await Promise.all([
        axios.get('/identity/insights/timeline'),
        axios.get('/identity/insights/access-logs'),
      ]);
      setTimeline(timelineRes?.data?.data || []);
      setAccessLogs(accessRes?.data?.data || []);
    };
    load();
  }, []);

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="max-w-5xl mx-auto space-y-6">
        <div className="bg-white border rounded-xl p-6 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h1 className="text-2xl font-bold">Activity Timeline</h1>
            <Link to="/dashboard" className="text-blue-600 hover:underline">Back to Dashboard</Link>
          </div>

          <div className="space-y-3">
            {timeline.map((event, idx) => (
              <div key={`${event.eventType}-${idx}`} className="border-l-4 border-blue-300 pl-3 py-2 bg-slate-50 rounded">
                <p className="font-semibold text-sm">{event.eventType}</p>
                <p className="text-sm text-slate-700">{event.description || 'No description'}</p>
                <p className="text-xs text-slate-500">{new Date(event.occurredAt).toLocaleString()}</p>
              </div>
            ))}
          </div>
        </div>

        <div className="bg-white border rounded-xl p-6 shadow-sm">
          <h2 className="text-xl font-semibold mb-4">Access Logs</h2>
          <div className="space-y-3">
            {accessLogs.map((log) => (
              <div key={log.id} className="border rounded p-3 bg-slate-50">
                <p className="text-sm font-medium">{log.accessType}</p>
                <p className="text-xs text-slate-500">Accessor: {log.accessorEmail || '-'} · Document: {log.documentId || '-'}</p>
                <p className="text-xs text-slate-500">{new Date(log.createdAt).toLocaleString()}</p>
              </div>
            ))}
            {accessLogs.length === 0 && <p className="text-slate-500 text-sm">No access logs yet.</p>}
          </div>
        </div>
      </div>
    </div>
  );
}
