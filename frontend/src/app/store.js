import { configureStore } from '@reduxjs/toolkit';
import authReducer from '../features/auth/authSlice';
import identityReducer from '../features/identity/identitySlice';
import documentReducer from '../features/documents/documentSlice';
import adminReducer from '../features/admin/adminSlice';
import consentReducer from '../features/consents/consentSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    identity: identityReducer,
    documents: documentReducer,
    admin: adminReducer,
    consents: consentReducer,
  },
});
