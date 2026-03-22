import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import axios from '../../api/axios';
import { API_BASE_URL } from '../../api/axios';

export default function DigitalIdCard() {
  const [card, setCard] = useState(null);
  const [err, setErr] = useState('');
  const [hideProfilePhoto, setHideProfilePhoto] = useState(false);

  const generate = async () => {
    setErr('');
    setHideProfilePhoto(false);
    try {
      const res = await axios.post('/qr/generate');
      setCard(res?.data?.data || null);
    } catch (e) {
      setErr(e?.response?.data?.message || 'Could not generate digital ID');
    }
  };

  const profilePhotoSrc = card?.profilePhotoUrl
    ? `${API_BASE_URL.replace(/\/api$/, '')}/uploads/${encodeURIComponent(card.profilePhotoUrl)}`
    : '';

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="max-w-3xl mx-auto">
        <div className="flex justify-between items-center mb-4">
          <h1 className="text-2xl font-bold">Digital ID Card</h1>
          <Link to="/dashboard" className="text-blue-600 hover:underline">Back to Dashboard</Link>
        </div>

        <button onClick={generate} className="px-4 py-2 bg-blue-600 text-white rounded">Generate Card</button>
        {err && <p className="mt-3 text-red-600">{err}</p>}

        {card && (
          <div className="mt-6 bg-white border rounded-xl p-6 shadow-sm">
            <div className="grid grid-cols-[1fr_180px] gap-4">
              <div>
                <h2 className="text-xl font-semibold">{card.fullName}</h2>
                <p className="text-slate-600">Identity No: {card.identityNumber || 'Pending'}</p>
                <p className="text-slate-600">DOB: {card.dob || '-'}</p>
                <p className="text-slate-600">Status: {card.status}</p>
                <p className="text-slate-600">Issue Date: {card.issueDate || '-'}</p>
                <a className="text-blue-600 text-sm" href={card.verificationUrl} target="_blank" rel="noreferrer">Public Verify Link</a>
              </div>
              <div className="text-center">
                {!hideProfilePhoto && profilePhotoSrc && (
                  <img
                    alt="profile"
                    src={profilePhotoSrc}
                    className="w-20 h-20 object-cover rounded-full mx-auto mb-2"
                    onError={() => setHideProfilePhoto(true)}
                  />
                )}
                {card.qrCodeBase64 && <img alt="qr" src={card.qrCodeBase64} className="w-40 h-40 mx-auto" />}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
