import React, { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from '../../api/axios';

const steps = [
  { key: 'STEP_1_PERSONAL', title: 'Personal Details' },
  { key: 'STEP_2_CONTACT', title: 'Contact Details' },
  { key: 'STEP_3_ADDRESS', title: 'Address Details' },
  { key: 'STEP_4_PROFILE_PHOTO', title: 'Profile Photo' },
  { key: 'STEP_5_DOCUMENTS', title: 'Documents' },
  { key: 'STEP_6_REVIEW_SUBMIT', title: 'Review & Submit' },
];

export default function OnboardingWizard() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [state, setState] = useState(null);
  const [msg, setMsg] = useState('');
  const [finalMsg, setFinalMsg] = useState('');
  const [photoFile, setPhotoFile] = useState(null);
  const [docFile, setDocFile] = useState(null);
  const [form, setForm] = useState({
    fullName: '', dob: '', gender: '', nationality: '', fatherName: '', motherName: '', occupation: '', maritalStatus: '',
    phone: '', alternatePhone: '',
    currentAddressLine1: '', currentAddressLine2: '', currentCity: '', currentDistrict: '', currentState: '', currentPincode: '', currentCountry: '',
    permanentSameAsCurrent: true, permanentAddressLine1: '', permanentAddressLine2: '', permanentCity: '', permanentDistrict: '', permanentState: '', permanentPincode: '', permanentCountry: ''
  });

  const activeStep = useMemo(() => {
    const key = state?.currentStep || 'STEP_1_PERSONAL';
    const idx = steps.findIndex((s) => s.key === key);
    return idx >= 0 ? idx : 0;
  }, [state]);

  const load = async () => {
    setLoading(true);
    try {
      const res = await axios.get('/identity/onboarding');
      const next = res?.data?.data;
      setState(next);
      if (next?.profile) {
        setForm((prev) => ({ ...prev, ...next.profile }));
      }
    } catch (e) {
      setMsg(e?.response?.data?.message || 'Failed to load onboarding state');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const saveStep = async (stepKey, saveAsDraft = true) => {
    setSaving(true);
    setMsg('');
    try {
      const res = await axios.put('/identity/onboarding/step', {
        step: stepKey,
        saveAsDraft,
        payload: form,
      });
      setState(res?.data?.data);
      setMsg(saveAsDraft ? 'Draft saved.' : 'Step completed.');
    } catch (e) {
      setMsg(e?.response?.data?.message || 'Could not save step');
    } finally {
      setSaving(false);
    }
  };

  const uploadPhoto = async () => {
    if (!photoFile) return;
    setSaving(true);
    try {
      const fd = new FormData();
      fd.append('file', photoFile);
      await axios.post('/identity/onboarding/profile-photo', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
      await saveStep('STEP_4_PROFILE_PHOTO', false);
      await load();
      setMsg('Profile photo uploaded.');
      setPhotoFile(null);
    } catch (e) {
      setMsg(e?.response?.data?.message || 'Photo upload failed');
    } finally {
      setSaving(false);
    }
  };

  const uploadDoc = async () => {
    if (!docFile) return;
    setSaving(true);
    try {
      const metadata = {
        documentType: 'AADHAAR',
        documentCategory: 'KYC',
        documentLabel: docFile.name
      };
      const fd = new FormData();
      fd.append('frontFile', docFile);
      fd.append('metadata', JSON.stringify(metadata));
      await axios.post('/locker/documents', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
      await saveStep('STEP_5_DOCUMENTS', false);
      setMsg('Document uploaded to locker.');
      setDocFile(null);
    } catch (e) {
      setMsg(e?.response?.data?.message || 'Document upload failed');
    } finally {
      setSaving(false);
    }
  };

  const submitFinal = async () => {
    setSaving(true);
    setFinalMsg('');
    try {
      // Persist latest in-memory form values before submit validation runs on backend.
      await axios.put('/identity/onboarding/step', {
        step: 'STEP_3_ADDRESS',
        saveAsDraft: true,
        payload: form,
      });

      // If user has selected files but has not clicked Upload buttons, auto-upload here.
      if (photoFile) {
        await uploadPhoto();
      }
      if (docFile) {
        await uploadDoc();
      }

      await axios.post('/identity/onboarding/submit');
      const docs = await axios.get('/locker/documents');
      const ids = (docs?.data?.data || []).map((d) => d.id);
      if (ids.length === 0) {
        throw new Error('Please upload at least one KYC document before final submit.');
      }
      await axios.post('/verification/submit', { documentIds: ids });
      setMsg('Onboarding submitted and KYC sent for admin review.');
      setFinalMsg('Onboarding submitted successfully. Redirecting...');
      navigate('/dashboard');
    } catch (e) {
      const message = e?.response?.data?.message || e?.message || 'Submission failed';
      setMsg(message);
      setFinalMsg(message);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="p-6">Loading onboarding...</div>;

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="max-w-5xl mx-auto bg-white border rounded-xl shadow-sm p-6">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-2xl font-bold">Multi-Step KYC Onboarding</h1>
          <Link to="/dashboard" className="text-blue-600 hover:underline">Back to Dashboard</Link>
        </div>

        <div className="grid grid-cols-6 gap-2 mb-6">
          {steps.map((s, i) => (
            <div key={s.key} className={`text-xs p-2 rounded text-center ${i <= activeStep ? 'bg-blue-600 text-white' : 'bg-slate-200 text-slate-700'}`}>
              {i + 1}. {s.title}
            </div>
          ))}
        </div>

        {msg && <p className="mb-4 text-sm text-blue-700">{msg}</p>}

        <div className="space-y-4">
          <div className="grid md:grid-cols-2 gap-3">
            <input className="border rounded p-2" placeholder="Full name" value={form.fullName || ''} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
            <input className="border rounded p-2" type="date" value={form.dob || ''} onChange={(e) => setForm({ ...form, dob: e.target.value })} />
            <input className="border rounded p-2" placeholder="Gender" value={form.gender || ''} onChange={(e) => setForm({ ...form, gender: e.target.value })} />
            <input className="border rounded p-2" placeholder="Nationality" value={form.nationality || ''} onChange={(e) => setForm({ ...form, nationality: e.target.value })} />
            <input className="border rounded p-2" placeholder="Phone" value={form.phone || ''} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            <input className="border rounded p-2" placeholder="Alternate phone" value={form.alternatePhone || ''} onChange={(e) => setForm({ ...form, alternatePhone: e.target.value })} />
            <input className="border rounded p-2" placeholder="Current Address Line 1" value={form.currentAddressLine1 || ''} onChange={(e) => setForm({ ...form, currentAddressLine1: e.target.value })} />
            <input className="border rounded p-2" placeholder="Current City" value={form.currentCity || ''} onChange={(e) => setForm({ ...form, currentCity: e.target.value })} />
            <input className="border rounded p-2" placeholder="Current State" value={form.currentState || ''} onChange={(e) => setForm({ ...form, currentState: e.target.value })} />
            <input className="border rounded p-2" placeholder="Current Pincode" value={form.currentPincode || ''} onChange={(e) => setForm({ ...form, currentPincode: e.target.value })} />
            <input className="border rounded p-2" placeholder="Current Country" value={form.currentCountry || ''} onChange={(e) => setForm({ ...form, currentCountry: e.target.value })} />
          </div>

          <div className="flex flex-wrap gap-2">
            {steps.map((s) => (
              <button key={s.key} type="button" className="px-3 py-2 bg-slate-100 rounded text-sm" onClick={() => saveStep(s.key, true)} disabled={saving}>
                Save {s.title}
              </button>
            ))}
          </div>

          <div className="border-t pt-4">
            <h3 className="font-semibold mb-2">Step 4: Profile Photo</h3>
            <input type="file" accept="image/*" onChange={(e) => setPhotoFile(e.target.files?.[0] || null)} />
            <button type="button" className="ml-2 px-3 py-1 bg-blue-600 text-white rounded" onClick={uploadPhoto} disabled={saving}>Upload Photo</button>
          </div>

          <div className="border-t pt-4">
            <h3 className="font-semibold mb-2">Step 5: KYC Document</h3>
            <input type="file" onChange={(e) => setDocFile(e.target.files?.[0] || null)} />
            <button type="button" className="ml-2 px-3 py-1 bg-blue-600 text-white rounded" onClick={uploadDoc} disabled={saving}>Upload Document</button>
          </div>

          <div className="border-t pt-4 flex justify-end">
            <div className="w-full flex items-center justify-between gap-4">
              <p className={`text-sm ${finalMsg && finalMsg.toLowerCase().includes('success') ? 'text-green-700' : 'text-red-600'}`}>
                {finalMsg}
              </p>
              <button type="button" className="px-4 py-2 bg-green-600 text-white rounded" onClick={submitFinal} disabled={saving}>
                {saving ? 'Submitting...' : 'Final Submit'}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
