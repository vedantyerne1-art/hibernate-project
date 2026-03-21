import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axios from '../../api/axios';

export const fetchPendingRequests = createAsyncThunk(
  'admin/fetchRequests',
  async (status, { rejectWithValue }) => {
    try {
      const response = await axios.get(`/verification/admin/requests?status=${status || 'PENDING'}`);
      return response.data?.data || response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch requests');
    }
  }
);

export const reviewRequest = createAsyncThunk(
  'admin/reviewRequest',
  async ({ requestId, data }, { rejectWithValue }) => {
    try {
      const response = await axios.post(`/verification/admin/review/${requestId}`, data);
      return response.data?.data || response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to review request');
    }
  }
);

const adminSlice = createSlice({
  name: 'admin',
  initialState: {
    requests: [],
    loading: false,
    error: null,
  },
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchPendingRequests.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchPendingRequests.fulfilled, (state, action) => {
        state.loading = false;
        state.requests = action.payload;
      })
      .addCase(fetchPendingRequests.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      .addCase(reviewRequest.fulfilled, (state, action) => {
        state.requests = state.requests.filter((request) => request.id !== action.payload.id);
      })
      .addCase('auth/login/pending', (state) => {
        state.requests = [];
        state.loading = false;
        state.error = null;
      })
      .addCase('auth/register/pending', (state) => {
        state.requests = [];
        state.loading = false;
        state.error = null;
      })
      .addCase('auth/logout', (state) => {
        state.requests = [];
        state.loading = false;
        state.error = null;
      });
  },
});

export default adminSlice.reducer;
