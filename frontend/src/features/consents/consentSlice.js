import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axios from '../../api/axios';

export const fetchConsents = createAsyncThunk(
  'consents/fetchConsents',
  async (_, { rejectWithValue }) => {
    try {
      const response = await axios.get('/consents');
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

export const revokeConsent = createAsyncThunk(
  'consents/revokeConsent',
  async (consentId, { rejectWithValue }) => {
    try {
      await axios.delete(`/consents/${consentId}`);
      return consentId;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

const consentSlice = createSlice({
  name: 'consents',
  initialState: {
    consents: [],
    loading: false,
    error: null,
  },
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchConsents.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchConsents.fulfilled, (state, action) => {
        state.loading = false;
        state.consents = action.payload;
      })
      .addCase(fetchConsents.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      .addCase(revokeConsent.fulfilled, (state, action) => {
        state.consents = state.consents.filter((consent) => consent.id !== action.payload);
      })
      .addCase('auth/login/pending', (state) => {
        state.consents = [];
        state.loading = false;
        state.error = null;
      })
      .addCase('auth/register/pending', (state) => {
        state.consents = [];
        state.loading = false;
        state.error = null;
      })
      .addCase('auth/logout', (state) => {
        state.consents = [];
        state.loading = false;
        state.error = null;
      });
  },
});

export default consentSlice.reducer;
