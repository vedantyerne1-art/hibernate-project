import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Provider, useSelector } from 'react-redux';
import { store } from './app/store';

import Login from './pages/public/Login';
import Register from './pages/public/Register';
import VerifyEmail from './pages/public/VerifyEmail';
import PublicQrVerify from './pages/public/PublicQrVerify';
import PublicVerifyToken from './pages/public/PublicVerifyToken';
import Dashboard from './pages/protected/Dashboard';
import ProfileSetup from './pages/protected/ProfileSetup';
import DocumentVault from './pages/protected/DocumentVault';
import OnboardingWizard from './pages/protected/OnboardingWizard';
import MyLocker from './pages/protected/MyLocker';
import DigitalIdCard from './pages/protected/DigitalIdCard';
import ConsentDashboard from './pages/protected/ConsentDashboard';
import AdminDashboard from './pages/protected/AdminDashboard';

const toRoleArray = (user) => {
  if (Array.isArray(user?.roles)) {
    return user.roles;
  }

  if (user?.role) {
    return [String(user.role).startsWith('ROLE_') ? user.role : `ROLE_${user.role}`];
  }

  return [];
};

const homeRouteFor = (user) => {
  const roles = toRoleArray(user);
  if (roles.includes('ROLE_ADMIN') || roles.includes('ROLE_SUPER_ADMIN')) {
    return '/admin/dashboard';
  }
  return '/dashboard';
};

const ProtectedRoute = ({ children, requiredRoles }) => {
  const { token, user } = useSelector((state) => state.auth);
  
  if (!token) {
    return <Navigate to="/login" replace />;
  }

  const userRoles = toRoleArray(user);

  if (requiredRoles && !requiredRoles.some((role) => userRoles.includes(role))) {
    return <Navigate to={homeRouteFor(user)} replace />;
  }
  
  // if (user && !user.isEmailVerified) {
  //   return <Navigate to="/verify-email" replace />;
  // }

  return children;
};

const RoleHomeRedirect = () => {
  const { user } = useSelector((state) => state.auth);
  return <Navigate to={homeRouteFor(user)} replace />;
};

function AppRoutes() {
  return (
    <Router>
      <div className="min-h-screen bg-slate-50 text-slate-900 font-sans">
        <Routes>
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/verify-email" element={<VerifyEmail />} />
          <Route path="/public/verify" element={<PublicQrVerify />} />
          <Route path="/public/verify/:token" element={<PublicVerifyToken />} />
          
          <Route path="/dashboard" element={<ProtectedRoute requiredRoles={["ROLE_USER"]}><Dashboard /></ProtectedRoute>} />
          <Route path="/profile-setup" element={<ProtectedRoute requiredRoles={["ROLE_USER"]}><OnboardingWizard /></ProtectedRoute>} />
          <Route path="/onboarding" element={<ProtectedRoute requiredRoles={["ROLE_USER"]}><OnboardingWizard /></ProtectedRoute>} />
          <Route path="/documents" element={<ProtectedRoute requiredRoles={["ROLE_USER"]}><DocumentVault /></ProtectedRoute>} />
          <Route path="/locker" element={<ProtectedRoute requiredRoles={["ROLE_USER"]}><MyLocker /></ProtectedRoute>} />
          <Route path="/digital-id" element={<ProtectedRoute requiredRoles={["ROLE_USER"]}><DigitalIdCard /></ProtectedRoute>} />
          <Route path="/consents" element={<ProtectedRoute requiredRoles={["ROLE_USER"]}><ConsentDashboard /></ProtectedRoute>} />
          
          <Route path="/admin/dashboard" element={<ProtectedRoute requiredRoles={["ROLE_ADMIN", "ROLE_SUPER_ADMIN"]}><AdminDashboard /></ProtectedRoute>} />
          <Route path="*" element={<RoleHomeRedirect />} />
        </Routes>
      </div>
    </Router>
  );
}

function App() {
  return (
    <Provider store={store}>
      <AppRoutes />
    </Provider>
  );
}

export default App;
