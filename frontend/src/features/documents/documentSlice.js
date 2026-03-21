import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axios from '../../api/axios';

export const fetchMyDocuments = createAsyncThunk(
  'documents/fetchMine',
  async (_, { rejectWithValue }) => {
    try {
      const response = await axios.get('/documents/my');
      return response.data.data;
    } catch (error) {
      return rejectWithValue(error.response.data);
    }
  }
);

export const uploadKycDocument = createAsyncThunk(
  'documents/upload',
  async (formData, { rejectWithValue }) => {
    try {
      // Must set content-type to multipart/form-data
      const response = await axios.post('/documents/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      return response.data.data;
    } catch (error) {
      return rejectWithValue(error.response.data);
    }
  }
);

export const deleteDocument = createAsyncThunk(
  'documents/delete',
  async (documentId, { rejectWithValue }) => {
    try {
      await axios.delete(`/documents/${documentId}`);
      return documentId;
    } catch (error) {
      return rejectWithValue(error.response.data);
    }
  }
);

const documentSlice = createSlice({
  name: 'documents',
  initialState: {
    documents: [],
    loading: false,
    uploading: false,
    error: null,
  },
  reducers: {
    clearDocumentError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchMyDocuments.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchMyDocuments.fulfilled, (state, action) => {
        state.loading = false;
        state.documents = action.payload;
      })
      .addCase(fetchMyDocuments.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      .addCase(uploadKycDocument.pending, (state) => {
        state.uploading = true;
        state.error = null;
      })
      .addCase(uploadKycDocument.fulfilled, (state, action) => {
        state.uploading = false;
        state.documents.push(action.payload);
      })
      .addCase(uploadKycDocument.rejected, (state, action) => {
        state.uploading = false;
        state.error = action.payload;
      })
      .addCase(deleteDocument.fulfilled, (state, action) => {
        state.documents = state.documents.filter((doc) => doc.id !== action.payload);
      })
      .addCase('auth/login/pending', (state) => {
        state.documents = [];
        state.loading = false;
        state.uploading = false;
        state.error = null;
      })
      .addCase('auth/register/pending', (state) => {
        state.documents = [];
        state.loading = false;
        state.uploading = false;
        state.error = null;
      })
      .addCase('auth/logout', (state) => {
        state.documents = [];
        state.loading = false;
        state.uploading = false;
        state.error = null;
      });
  },
});

export const { clearDocumentError } = documentSlice.actions;
export default documentSlice.reducer;
