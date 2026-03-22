import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import axios from '../../api/axios';

export default function PublicShareLink() {
  const { token } = useParams();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [docs, setDocs] = useState([]);

  useEffect(() => {
    const run = async () => {
      setLoading(true);
      setError('');
      try {
        const response = await axios.get(`/share/links/${token}`);
        setDocs(response?.data?.data || []);
      } catch (e) {
        setError(e?.response?.data?.message || 'Invalid or expired share link');
      } finally {
        setLoading(false);
      }
    };
    run();
  }, [token]);

  return (
    <div className="min-h-screen bg-slate-100 p-6 flex items-center justify-center">
      <div className="w-full max-w-2xl bg-white border rounded-xl p-6 shadow-sm">
        <h1 className="text-2xl font-bold mb-2">Shared Documents</h1>
        <p className="text-sm text-slate-500 mb-6">Secure consent-based shared document view.</p>

        {loading && <p className="text-slate-600">Loading shared documents...</p>}
        {!loading && error && <p className="text-red-600">{error}</p>}

        {!loading && !error && docs.length === 0 && (
          <p className="text-slate-600">No documents in this share link.</p>
        )}

        <div className="space-y-3">
          {docs.map((doc) => (
            <div key={doc.id} className="border rounded p-3 bg-slate-50">
              <p className="font-semibold">{doc.documentName}</p>
              <p className="text-xs text-slate-500">{doc.documentType} · {doc.status}</p>
            </div>
          ))}
        </div>

        <div className="mt-6">
          <Link to="/login" className="text-blue-600 hover:underline">Back to login</Link>
        </div>
      </div>
    </div>
  );
}
