import React, { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { createIdentityProfile, fetchIdentityProfile } from '../../features/identity/identitySlice';
import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';

const ProfileSetup = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { user } = useSelector((state) => state.auth);
  const { profile } = useSelector((state) => state.identity);
  const [errorMsg, setErrorMsg] = useState('');
  const [formData, setFormData] = useState({
    dateOfBirth: '',
    gender: 'PREFER_NOT_TO_SAY',
    addressLine1: '',
    addressLine2: '',
    city: '',
    state: '',
    postalCode: '',
    country: '',
  });

  useEffect(() => {
    dispatch(fetchIdentityProfile());
  }, [dispatch]);

  useEffect(() => {
    if (!profile) {
      return;
    }

    const parts = (profile.address || '').split(',').map((p) => p.trim());
    setFormData((prev) => ({
      ...prev,
      dateOfBirth: profile.dob || prev.dateOfBirth,
      gender: profile.gender || prev.gender,
      addressLine1: parts[0] || prev.addressLine1,
      addressLine2: parts.length > 5 ? parts.slice(1, parts.length - 4).join(', ') : prev.addressLine2,
      city: parts.length >= 4 ? parts[parts.length - 4] : prev.city,
      state: parts.length >= 3 ? parts[parts.length - 3] : prev.state,
      postalCode: parts.length >= 2 ? parts[parts.length - 2] : prev.postalCode,
      country: profile.nationality || (parts.length >= 1 ? parts[parts.length - 1] : prev.country),
    }));
  }, [profile]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');

    const fullAddress = [
      formData.addressLine1,
      formData.addressLine2,
      formData.city,
      formData.state,
      formData.postalCode,
      formData.country,
    ]
      .filter(Boolean)
      .join(', ');

    const payload = {
      fullName: user?.fullName || 'User',
      dob: formData.dateOfBirth,
      gender: formData.gender,
      address: fullAddress,
      nationality: formData.country,
    };

    try {
      await dispatch(createIdentityProfile(payload)).unwrap();
      navigate('/dashboard');
    } catch (err) {
      const msg =
        err?.message ||
        err?.data?.message ||
        err?.error ||
        'Profile creation failed. Please check all fields.';
      setErrorMsg(msg);
      console.error('Profile creation failed', err);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full mx-auto space-y-8 p-10 bg-white shadow rounded-lg border">
        <div>
          <h2 className="text-center text-3xl font-extrabold text-gray-900">Setup Identity Profile</h2>
          <p className="mt-2 text-center text-sm text-gray-600">Provide personal details for KYC verification.</p>
          {profile?.status === 'REJECTED' && (
            <p className="mt-4 text-sm text-red-600 text-left">
              Your previous KYC was rejected{profile?.rejectionReason ? `: ${profile.rejectionReason}` : '.'} Update details and submit again.
            </p>
          )}
        </div>
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          {errorMsg && <p className="text-sm text-red-600">{errorMsg}</p>}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Date of Birth</label>
              <input type="date" name="dateOfBirth" required className="w-full border rounded-md p-2" onChange={handleChange} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Gender</label>
              <select name="gender" className="w-full border rounded-md p-2" onChange={handleChange}>
                <option value="PREFER_NOT_TO_SAY">Select...</option>
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Address Line 1</label>
            <input type="text" name="addressLine1" required className="w-full border rounded-md p-2" onChange={handleChange} />
          </div>
          <div className="grid grid-cols-2 gap-4">
             <div>
               <label className="block text-sm font-medium text-gray-700 mb-1">City</label>
               <input type="text" name="city" required className="w-full border rounded-md p-2" onChange={handleChange} />
             </div>
             <div>
               <label className="block text-sm font-medium text-gray-700 mb-1">State</label>
               <input type="text" name="state" required className="w-full border rounded-md p-2" onChange={handleChange} />
             </div>
             <div>
               <label className="block text-sm font-medium text-gray-700 mb-1">Postal Code</label>
               <input type="text" name="postalCode" required className="w-full border rounded-md p-2" onChange={handleChange} />
             </div>
             <div>
               <label className="block text-sm font-medium text-gray-700 mb-1">Country</label>
               <input type="text" name="country" required className="w-full border rounded-md p-2" onChange={handleChange} />
             </div>
          </div>
          <button type="submit" className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700">
            Submit Profile
          </button>
        </form>
      </div>
    </div>
  );
};

export default ProfileSetup;