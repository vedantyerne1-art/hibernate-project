import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import axios from '../../api/axios';

export default function MyLocker() {
  const [docs, setDocs] = useState([]);
  const [folders, setFolders] = useState([]);
  const [q, setQ] = useState('');
  const [type, setType] = useState('');
  const [folderName, setFolderName] = useState('');
  const [tags, setTags] = useState('');
  const [compareA, setCompareA] = useState('');
  const [compareB, setCompareB] = useState('');
  const [comparisonResult, setComparisonResult] = useState('');
  const [preview, setPreview] = useState(null);

  const load = async () => {
    const params = new URLSearchParams();
    if (q) params.set('q', q);
    if (type) params.set('type', type);
    const res = await axios.get(`/locker/documents?${params.toString()}`);
    const allDocs = res?.data?.data || [];
    setDocs(allDocs.filter((doc) => doc?.documentName !== 'KYC Primary Document'));

    const folderRes = await axios.get('/locker/folders');
    setFolders(folderRes?.data?.data || []);
  };

  useEffect(() => {
    load();
  }, []);

  const filtered = useMemo(() => docs, [docs]);

  const upload = async (file, documentType) => {
    if (!file) return;
    const fd = new FormData();
    fd.append('frontFile', file);
    fd.append('metadata', JSON.stringify({ documentType, documentCategory: 'PERSONAL', documentLabel: file.name, folderName, tags }));
    await axios.post('/locker/documents', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
    await load();
  };

  const showVersions = async (id) => {
    const response = await axios.get(`/locker/documents/${id}/versions`);
    const versions = response?.data?.data || [];
    const summary = versions.map((v) => `v${v.versionNumber || 1} - ${v.status}`).join('\n');
    alert(summary || 'No versions found');
  };

  const compareDocuments = async () => {
    if (!compareA || !compareB) return;
    const response = await axios.get(`/locker/documents/compare?first=${compareA}&second=${compareB}`);
    setComparisonResult(response?.data?.data?.result || 'No comparison output');
  };

  const openPreview = async (id) => {
    const response = await axios.get(`/locker/documents/${id}/preview`, { responseType: 'blob' });
    const url = URL.createObjectURL(new Blob([response.data]));
    setPreview(url);
  };

  const download = async (id, name) => {
    const response = await axios.get(`/locker/documents/${id}/download`, { responseType: 'blob' });
    const url = URL.createObjectURL(new Blob([response.data]));
    const a = document.createElement('a');
    a.href = url;
    a.setAttribute('download', name || 'document');
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 5000);
  };

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="max-w-6xl mx-auto bg-white border rounded-xl p-6 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <h1 className="text-2xl font-bold">My Locker</h1>
          <Link to="/dashboard" className="text-blue-600 hover:underline">Back to Dashboard</Link>
        </div>

        <div className="grid md:grid-cols-4 gap-3 mb-4">
          <input className="border rounded p-2" placeholder="Search by name/type" value={q} onChange={(e) => setQ(e.target.value)} />
          <select className="border rounded p-2" value={type} onChange={(e) => setType(e.target.value)}>
            <option value="">All Types</option>
            <option value="AADHAAR">Aadhaar</option>
            <option value="PAN">PAN</option>
            <option value="PASSPORT">Passport</option>
            <option value="DRIVING_LICENSE">Driving License</option>
            <option value="VOTER_ID">Voter ID</option>
            <option value="UTILITY_BILL">Utility Bill</option>
          </select>
          <button className="px-3 py-2 bg-slate-900 text-white rounded" onClick={load}>Apply</button>
          <input className="border rounded p-2" placeholder="Folder name" value={folderName} onChange={(e) => setFolderName(e.target.value)} />
          <input className="border rounded p-2" placeholder="Tags (comma separated)" value={tags} onChange={(e) => setTags(e.target.value)} />
          <label className="px-3 py-2 bg-blue-600 text-white rounded cursor-pointer text-center">
            Upload
            <input type="file" className="hidden" onChange={(e) => upload(e.target.files?.[0], type || 'OTHER')} />
          </label>
        </div>

        {folders.length > 0 && (
          <div className="mb-4 text-sm text-slate-600">
            Folders: {folders.map((f) => f.folderName).join(', ')}
          </div>
        )}

        <div className="mb-4 flex flex-wrap items-center gap-2">
          <select className="border rounded p-2" value={compareA} onChange={(e) => setCompareA(e.target.value)}>
            <option value="">Compare doc A</option>
            {filtered.map((d) => <option key={d.id} value={d.id}>{d.documentName}</option>)}
          </select>
          <select className="border rounded p-2" value={compareB} onChange={(e) => setCompareB(e.target.value)}>
            <option value="">Compare doc B</option>
            {filtered.map((d) => <option key={d.id} value={d.id}>{d.documentName}</option>)}
          </select>
          <button className="px-3 py-2 bg-slate-900 text-white rounded" onClick={compareDocuments}>Compare</button>
          {comparisonResult && <span className="text-sm text-amber-700">{comparisonResult}</span>}
        </div>

        <div className="grid md:grid-cols-3 gap-3">
          {filtered.map((d) => (
            <div key={d.id} className="border rounded p-3 bg-slate-50">
              <p className="font-semibold">{d.documentName}</p>
              <p className="text-xs text-slate-500">{d.documentType} · {d.status}</p>
              <p className="text-xs text-slate-500">Version: {d.versionNumber || 1}</p>
              {d.folderName && <p className="text-xs text-slate-500">Folder: {d.folderName}</p>}
              {d.tags && <p className="text-xs text-slate-500">Tags: {d.tags}</p>}
              {d.comparisonWarning && <p className="text-xs text-amber-700">{d.comparisonWarning}</p>}
              <div className="mt-3 flex gap-2">
                <button className="text-sm text-blue-600" onClick={() => openPreview(d.id)}>Preview</button>
                <button className="text-sm text-blue-600" onClick={() => download(d.id, d.fileName)}>Download</button>
                <button className="text-sm text-blue-600" onClick={() => showVersions(d.id)}>Versions</button>
                <button className="text-sm text-red-600" onClick={async () => { await axios.patch(`/locker/documents/${d.id}/archive`); await load(); }}>Archive</button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {preview && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center p-6" onClick={() => setPreview(null)}>
          <div className="bg-white rounded-xl w-full max-w-3xl h-[80vh]" onClick={(e) => e.stopPropagation()}>
            <iframe title="preview" src={preview} className="w-full h-full rounded-xl" />
          </div>
        </div>
      )}
    </div>
  );
}
