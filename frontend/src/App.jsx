import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './routes/ProtectedRoute';

import Landing from './pages/public/Landing';
import Login from './pages/public/Login';
import Register from './pages/public/Register';

import StudentDashboard from './pages/student/StudentDashboard';
import StudentProfile from './pages/student/StudentProfile';
import BrowseJobs from './pages/student/BrowseJobs';
import JobDetails from './pages/student/JobDetails';
import MyApplications from './pages/student/MyApplications';
import ApplicationDetail from './pages/student/ApplicationDetail';

import RecruiterDashboard from './pages/recruiter/RecruiterDashboard';
import AdminDashboard from './pages/admin/AdminDashboard';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Landing />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          <Route path="/student/dashboard" element={
            <ProtectedRoute allowedRole="STUDENT"><StudentDashboard /></ProtectedRoute>
          } />
          <Route path="/student/profile" element={
            <ProtectedRoute allowedRole="STUDENT"><StudentProfile /></ProtectedRoute>
          } />
          <Route path="/student/jobs" element={
            <ProtectedRoute allowedRole="STUDENT"><BrowseJobs /></ProtectedRoute>
          } />
          <Route path="/student/jobs/:jobId" element={
            <ProtectedRoute allowedRole="STUDENT"><JobDetails /></ProtectedRoute>
          } />
          <Route path="/student/applications" element={
            <ProtectedRoute allowedRole="STUDENT"><MyApplications /></ProtectedRoute>
          } />
          <Route path="/student/applications/:id" element={
            <ProtectedRoute allowedRole="STUDENT"><ApplicationDetail /></ProtectedRoute>
          } />

          <Route path="/recruiter/dashboard" element={
            <ProtectedRoute allowedRole="RECRUITER"><RecruiterDashboard /></ProtectedRoute>
          } />

          <Route path="/admin/dashboard" element={
            <ProtectedRoute allowedRole="ADMIN"><AdminDashboard /></ProtectedRoute>
          } />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;