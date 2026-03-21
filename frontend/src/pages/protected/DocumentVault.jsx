import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchMyDocuments, uploadKycDocument, deleteDocument } from '../../features/documents/documentSlice';
import { fetchIdentityProfile } from '../../features/identity/identitySlice';
import { Link, useNavigate } from 'react-router-dom';
import { logout } from '../../features/auth/authSlice';
import axios from '../../api/axios';

const DocumentVault = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { documents, loading, uploading } = useSelector(state => state.documents);
  const { profile } = useSelector(state => state.identity);

  const [uploadData, setUploadData] = useState({
    file: null,
    documentType: 'AADHAAR',
    documentName: '',
    documentNumber: ''
  });

  const [msg, setMsg] = useState('');

  useEffect(() => {
    dispatch(fetchIdentityProfile());
    dispatch(fetchMyDocuments());
  }, [dispatch]);

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  const handleFileChange = (e) => {
    setUploadData({ ...uploadData, file: e.target.files[0] });
  };

  const handleChange = (e) => {
    setUploadData({ ...uploadData, [e.target.name]: e.target.value });
  };

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!uploadData.file) return;

    const formData = new FormData();
    formData.append('file', uploadData.file);
    formData.append('type', uploadData.documentType);
    formData.append('name', uploadData.documentName);
    if (profile) {
      formData.append('identityProfileId', profile.id);
    }

    try {
      await dispatch(uploadKycDocument(formData)).unwrap();
      setMsg("Document uploaded successfully.");
      setUploadData({ ...uploadData, file: null, documentName: '', documentNumber: '' });
      // Reset file input
      document.getElementById('file-upload').value = '';
    } catch (err) {
      setMsg(err.message || 'Upload failed');
    }
  };

  const downloadDoc = async (id, fileName) => {
    try {
      const response = await axios.get(`/documents/${id}/download`, {
        responseType: 'blob'
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', fileName);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Sidebar */}
      <aside className="w-64 bg-white border-r h-screen shadow-sm sticky top-0">
        <div className="p-6">
          <h1 className="text-2xl font-bold text-blue-600 tracking-tight">TrustID</h1>
          <p className="text-sm text-gray-500">Document Vault</p>
        </div>
        <nav className="mt-6 flex flex-col space-y-1 px-4">
          <Link to="/dashboard" className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md">Dashboard</Link>
          <Link to="/documents" className="px-4 py-2 bg-blue-50 text-blue-700 rounded-md font-medium">Document Vault</Link>
          <Link to="/consents" className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md">Linked Apps</Link>
        </nav>
        <div className="absolute bottom-0 w-full p-4 border-t">
          <button onClick={handleLogout} className="w-full text-left px-4 py-2 text-red-600 hover:bg-red-50 rounded-md font-medium">
            Logout
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 p-8">
        <div className="max-w-5xl mx-auto space-y-8">
          <header>
            <h2 className="text-3xl font-bold text-gray-900">Your Documents</h2>
            <p className="text-gray-500 mt-1">Upload and manage your secure documents.</p>
          </header>

          <div className="grid grid-cols-3 gap-6">
            
            {/* Upload Section */}
            <div className="col-span-1 bg-white p-6 rounded-xl shadow-sm border border-gray-100 h-fit">
              <h3 className="font-semibold text-lg mb-4">Upload New</h3>
              {msg && <p className="text-sm bg-blue-50 text-blue-700 p-2 mb-4 rounded">{msg}</p>}
              <form onSubmit={handleUpload} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Doc Type</label>
                  <select name="documentType" className="w-full border rounded p-2 text-sm" value={uploadData.documentType} onChange={handleChange}>
                    <option value="AADHAAR">Aadhaar</option>
                    <option value="PAN">PAN</option>
                    <option value="PASSPORT">Passport</option>
                    <option value="DRIVING_LICENSE">Driver's License</option>
                    <option value="VOTER_ID">Voter ID</option>
                    <option value="UTILITY_BILL">Utility Bill</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Doc Name</label>
                  <input type="text" name="documentName" required className="w-full border rounded p-2 text-sm" value={uploadData.documentName} onChange={handleChange} placeholder="e.g. My SSN Card" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Doc Number (Optional)</label>
                  <input type="text" name="documentNumber" className="w-full border rounded p-2 text-sm" value={uploadData.documentNumber} onChange={handleChange} />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">File</label>
                  <input type="file" id="file-upload" required onChange={handleFileChange} className="w-full text-sm" />
                </div>
                <button type="submit" disabled={uploading} className="w-full bg-blue-600 text-white font-medium py-2 rounded shadow hover:bg-blue-700 disabled:opacity-50">
                  {uploading ? 'Uploading...' : 'Upload Securely'}
                </button>
              </form>
            </div>

            {/* Document List */}
            <div className="col-span-2 bg-white p-6 rounded-xl shadow-sm border border-gray-100">
              <h3 className="font-semibold text-lg mb-4">Vault ({documents.length})</h3>
              {loading ? (
                <p>Loading documents...</p>
              ) : documents.length === 0 ? (
                <p className="text-gray-500">No documents found. Start uploading!</p>
              ) : (
                <div className="space-y-3">
                  {documents.map(doc => (
                    <div key={doc.id} className="border border-gray-200 rounded-lg p-4 flex items-center justify-between">
                      <div>
                        <h4 className="font-semibold">{doc.documentName} <span className="text-xs font-normal text-gray-500 ml-2 px-2 py-0.5 bg-gray-100 rounded">{doc.documentType}</span></h4>
                        <div className="text-xs text-gray-500 mt-1 flex space-x-4">
                          <span>Status: <b className="text-gray-700">{doc.status}</b></span>
                          <span>Size: {(doc.fileSize / 1024).toFixed(1)} KB</span>
                          {doc.documentNumber && <span>No: {doc.documentNumber}</span>}
                        </div>
                      </div>
                      <div className="flex space-x-2">
                        <button onClick={() => downloadDoc(doc.id, doc.fileName)} className="px-3 py-1.5 bg-gray-100 hover:bg-gray-200 text-sm font-medium rounded text-gray-700">Download</button>
                        <button onClick={() => dispatch(deleteDocument(doc.id))} className="px-3 py-1.5 bg-red-50 hover:bg-red-100 text-sm font-medium rounded text-red-700">Delete</button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

          </div>
        </div>
      </main>
    </div>
  );
};

export default DocumentVault;