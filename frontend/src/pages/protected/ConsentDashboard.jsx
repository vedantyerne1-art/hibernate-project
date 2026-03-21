import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchConsents, revokeConsent } from '../../features/consents/consentSlice';
import { Link, useNavigate } from 'react-router-dom';
import { logout } from '../../features/auth/authSlice';

const ConsentDashboard = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { consents, loading } = useSelector(state => state.consents);

  useEffect(() => {
    dispatch(fetchConsents());
  }, [dispatch]);

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  const handleRevoke = (id, clientName) => {
    if (window.confirm(`Are you sure you want to revoke access for ${clientName}?`)) {
      dispatch(revokeConsent(id));
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex">
      <aside className="w-64 bg-white border-r h-screen shadow-sm sticky top-0">
        <div className="p-6">
          <h1 className="text-2xl font-bold text-blue-600 tracking-tight">TrustID</h1>
          <p className="text-sm text-gray-500">Consent Management</p>
        </div>
        <nav className="mt-6 flex flex-col space-y-1 px-4">
          <Link to="/dashboard" className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md">Dashboard</Link>
          <Link to="/documents" className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md">Document Vault</Link>
          <Link to="/consents" className="px-4 py-2 bg-blue-50 text-blue-700 rounded-md font-medium">Linked Apps</Link>
        </nav>
        <div className="absolute bottom-0 w-full p-4 border-t">
          <button onClick={handleLogout} className="w-full text-left px-4 py-2 text-red-600 hover:bg-red-50 rounded-md font-medium">
            Logout
          </button>
        </div>
      </aside>

      <main className="flex-1 p-8">
        <div className="max-w-4xl mx-auto space-y-8">
          <header>
            <h2 className="text-3xl font-bold text-gray-900">Linked Applications</h2>
            <p className="text-gray-500 mt-1">Manage third-party applications that have access to your Identity Profile and Vault.</p>
          </header>

          <section className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            {loading ? (
              <p>Loading consents...</p>
            ) : consents.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                You haven't granted access to any third-party applications yet.
              </div>
            ) : (
              <div className="space-y-4">
                {consents.map(consent => (
                  <div key={consent.id} className="flex justify-between items-center border p-4 rounded-lg">
                    <div>
                      <h4 className="font-semibold text-lg">{consent.clientName}</h4>
                      <p className="text-sm text-gray-500">Org: {consent.organizationName}</p>
                      <div className="mt-2 flex space-x-2">
                        {consent.scopes.split(',').map(scope => (
                           <span key={scope} className="inline-block px-2 text-xs bg-slate-100 border rounded text-slate-700">{scope.trim()}</span>
                        ))}
                      </div>
                      <p className="text-xs text-gray-400 mt-2">Granted on: {new Date(consent.grantedAt).toLocaleDateString()}</p>
                    </div>
                    <div>
                      <button 
                        onClick={() => handleRevoke(consent.id, consent.clientName)} 
                        className="px-4 py-2 bg-red-50 hover:bg-red-100 text-red-600 rounded font-medium text-sm transition"
                      >
                        Revoke Access
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>
      </main>
    </div>
  );
};
export default ConsentDashboard;