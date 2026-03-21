import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axios from '../../api/axios';

export const fetchIdentityProfile = createAsyncThunk(
  'identity/fetchProfile',
  async (_, { rejectWithValue }) => {
    try {
      const response = await axios.get('/identity/me');
      return response.data.data;
    } catch (error) {
      if (error.response && error.response.status === 404) {
        return null; // No profile yet
      }
      return rejectWithValue(error.response.data);
    }
  }
);

export const createIdentityProfile = createAsyncThunk(
  'identity/createProfile',
  async (profileData, { rejectWithValue }) => {
    try {
      const response = await axios.post('/identity', profileData);
      return response.data.data;
    } catch (error) {
      return rejectWithValue(error.response.data);
    }
  }
);

const identitySlice = createSlice({
  name: 'identity',
  initialState: {
    profile: null,
    loading: false,
    error: null,
  },
  reducers: {
    clearIdentityError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchIdentityProfile.pending, (state) => {
        state.loading = true;
        state.error = null;
        state.profile = null;
      })
      .addCase(fetchIdentityProfile.fulfilled, (state, action) => {
        state.loading = false;
        state.profile = action.payload;
      })
      .addCase(fetchIdentityProfile.rejected, (state, action) => {
        state.loading = false;
        state.profile = null;
        state.error = action.payload;
      })
      .addCase(createIdentityProfile.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(createIdentityProfile.fulfilled, (state, action) => {
        state.loading = false;
        state.profile = action.payload;
      })
      .addCase(createIdentityProfile.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      .addCase('auth/login/pending', (state) => {
        state.profile = null;
        state.loading = false;
        state.error = null;
      })
      .addCase('auth/register/pending', (state) => {
        state.profile = null;
        state.loading = false;
        state.error = null;
      })
      .addCase('auth/logout', (state) => {
        state.profile = null;
        state.loading = false;
        state.error = null;
      });
  },
});

export const { clearIdentityError } = identitySlice.actions;
export default identitySlice.reducer;
