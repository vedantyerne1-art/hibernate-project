import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchPendingRequests, fetchRiskUsers, reviewRequest } from '../../features/admin/adminSlice';
import { useNavigate } from 'react-router-dom';
import { logout } from '../../features/auth/authSlice';
import axios from '../../api/axios';

const AdminDashboard = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { requests, riskUsers, loading } = useSelector(state => state.admin);
  const [selectedReq, setSelectedReq] = useState(null);
  const [riskLevel, setRiskLevel] = useState('HIGH');

  useEffect(() => {
    dispatch(fetchPendingRequests('PENDING'));
    dispatch(fetchRiskUsers('HIGH'));
  }, [dispatch]);

  useEffect(() => {
    dispatch(fetchRiskUsers(riskLevel));
  }, [dispatch, riskLevel]);

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  const handleAction = async (requestId, status) => {
    if (window.confirm(`Are you sure you want to ${status} this request?`)) {
      const resubmissionReason = status === 'RESUBMISSION_REQUIRED'
        ? window.prompt('Enter resubmission reason for user:', 'Please re-upload clearer document copies')
        : null;

      const result = await dispatch(reviewRequest({
        requestId,
        data: {
          status,
          notes: status === 'APPROVED' ? 'KYC approved by admin' : 'KYC rejected by admin',
          rejectionReason: status === 'REJECTED' ? 'KYC details do not match uploaded proof' : null,
          resubmissionReason,
        }
      }));

      if (reviewRequest.rejected.match(result)) {
        alert(result?.payload || 'Failed to update request status');
        return;
      }

      setSelectedReq(null);
    }
  };

  const handleViewUpload = async (docId) => {
    try {
      const response = await axios.get(`/documents/${docId}/download`, {
        responseType: 'blob',
      });

      const blobUrl = URL.createObjectURL(response.data);
      window.open(blobUrl, '_blank', 'noopener,noreferrer');

      // Revoke later so the browser can finish loading the blob in the new tab.
      setTimeout(() => URL.revokeObjectURL(blobUrl), 60_000);
    } catch (error) {
      alert(error?.response?.data?.message || 'Unable to open document.');
    }
  };

  return (
    <div className="min-h-screen bg-slate-100 flex">
      {/* Sidebar */}
      <aside className="w-64 bg-slate-900 text-white h-screen shadow-sm sticky top-0">
        <div className="p-6">
          <h1 className="text-2xl font-bold tracking-tight">TrustID Admin</h1>
        </div>
        <nav className="mt-6 flex flex-col px-4">
          <a href="/admin/dashboard" className="px-4 py-2 bg-slate-800 rounded font-medium">Pending Approvals</a>
        </nav>
        <div className="absolute bottom-0 w-full p-4 border-t border-slate-800">
          <button onClick={handleLogout} className="w-full text-left px-4 py-2 text-red-400 hover:bg-slate-800 rounded font-medium">
            Logout Admin
          </button>
        </div>
      </aside>

      <main className="flex-1 p-8">
        <h2 className="text-3xl font-bold text-slate-800 mb-6">KYC Verification Inbox</h2>
        <div className="grid grid-cols-3 gap-6">
          <div className="col-span-1 border-r pr-6 space-y-4 max-h-[80vh] overflow-y-auto">
            <div className="bg-white rounded-lg border p-3">
              <div className="flex items-center justify-between mb-2">
                <h3 className="font-semibold text-sm">Risk Detection Board</h3>
                <select value={riskLevel} onChange={(e) => setRiskLevel(e.target.value)} className="text-xs border rounded p-1">
                  <option value="HIGH">HIGH</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="LOW">LOW</option>
                </select>
              </div>
              <div className="space-y-2 max-h-40 overflow-y-auto">
                {(riskUsers || []).map((user) => (
                  <div key={user.userId} className="p-2 bg-red-50 border border-red-100 rounded">
                    <p className="text-xs font-semibold">{user.fullName}</p>
                    <p className="text-[11px] text-slate-600">{user.email}</p>
                    <p className="text-[11px] text-slate-600">Trust: {user.trustScore} · {user.identityLevel}</p>
                  </div>
                ))}
                {(riskUsers || []).length === 0 && <p className="text-xs text-slate-500">No users in this risk band.</p>}
              </div>
            </div>

            {loading && <p>Loading requests...</p>}
            {!loading && requests.length === 0 && <p className="text-gray-500">No pending requests.</p>}
            {requests.map(req => (
              <div 
                key={req.id} 
                onClick={() => setSelectedReq(req)}
                className={`p-4 rounded-lg border cursor-pointer transition ${selectedReq?.id === req.id ? 'border-blue-500 bg-blue-50' : 'bg-white hover:bg-slate-50'}`}
              >
                <div className="font-semibold">User #{req.user?.id}</div>
                <div className="text-sm text-gray-500">Submitted: {new Date(req.submittedAt).toLocaleDateString()}</div>
                <span className="inline-block mt-2 px-2 py-0.5 text-xs bg-yellow-100 text-yellow-800 rounded-full">{req.status}</span>
              </div>
            ))}
          </div>

          <div className="col-span-2">
            {selectedReq ? (
              <div className="bg-white p-6 shadow-sm border rounded-lg">
                <h3 className="text-xl font-bold mb-4">Request #{selectedReq.id} Details</h3>
                <div className="grid grid-cols-2 gap-4 mb-6">
                  <div>
                    <label className="text-xs text-gray-500 uppercase font-semibold">User Email</label>
                    <p>{selectedReq.user?.email}</p>
                  </div>
                  <div>
                    <label className="text-xs text-gray-500 uppercase font-semibold">Profile Name</label>
                    <p>{selectedReq.identityProfile?.fullName || selectedReq.user?.fullName}</p>
                  </div>
                  <div>
                     <label className="text-xs text-gray-500 uppercase font-semibold">TrustID No.</label>
                    <p>{selectedReq.identityProfile?.identityNumber || 'Pending allocation'}</p>
                  </div>
                  <div>
                     <label className="text-xs text-gray-500 uppercase font-semibold">DOB</label>
                    <p>{selectedReq.identityProfile?.dob || '-'}</p>
                  </div>
                </div>

                <h4 className="font-semibold mb-3">Attached Documents ({selectedReq.documents?.length || 0})</h4>
                <div className="space-y-3 mb-8">
                  {selectedReq.documents?.map(doc => (
                    <div key={doc.id} className="p-3 border rounded bg-slate-50 flex justify-between items-center">
                      <div>
                        <div className="font-medium text-sm">{doc.documentType}</div>
                        <div className="text-xs text-gray-500">{doc.documentNumber || "No doc number provided"}</div>
                        <div className="text-xs text-gray-500">Version: {doc.versionNumber || 1}</div>
                        {doc.ocrName && <div className="text-xs text-gray-500">OCR Name: {doc.ocrName}</div>}
                        {doc.ocrDob && <div className="text-xs text-gray-500">OCR DOB: {doc.ocrDob}</div>}
                        {doc.comparisonWarning && <div className="text-xs text-amber-700">Warning: {doc.comparisonWarning}</div>}
                      </div>
                      <button
                        type="button"
                        onClick={() => handleViewUpload(doc.id)}
                        className="text-blue-600 text-sm font-medium hover:underline"
                      >
                        View Upload
                      </button>
                    </div>
                  ))}
                </div>

                <div className="flex space-x-4">
                  <button onClick={() => handleAction(selectedReq.id, 'APPROVED')} className="px-6 py-2 bg-green-600 hover:bg-green-700 text-white rounded font-medium shadow-sm">
                    Approve KYC
                  </button>
                  <button onClick={() => handleAction(selectedReq.id, 'REJECTED')} className="px-6 py-2 bg-red-600 hover:bg-red-700 text-white rounded font-medium shadow-sm">
                    Reject
                  </button>
                  <button onClick={() => handleAction(selectedReq.id, 'RESUBMISSION_REQUIRED')} className="px-6 py-2 bg-amber-500 hover:bg-amber-600 text-white rounded font-medium shadow-sm">
                    Request Resubmission
                  </button>
                </div>
              </div>
            ) : (
              <div className="h-full flex items-center justify-center text-gray-400 bg-white border border-dashed rounded-lg">
                Select a request from the left panel to review.
              </div>
            )}
          </div>
        </div>

      </main>
    </div>
  );
};
export default AdminDashboard;