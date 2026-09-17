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

import CompanyProfile from './pages/recruiter/CompanyProfile';
import MyJobs from './pages/recruiter/MyJobs';
import JobForm from './pages/recruiter/JobForm';
import ApplicantsList from './pages/recruiter/ApplicantsList';

import ManageStudents from './pages/admin/ManageStudents';
import ManageRecruiters from './pages/admin/ManageRecruiters';
import ManageJobs from './pages/admin/ManageJobs';
import Analytics from './pages/admin/Analytics';

import RecruiterProfile from './pages/recruiter/RecruiterProfile';
import AdminProfile from './pages/admin/AdminProfile';

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

          <Route path="/recruiter/profile" element={
            <ProtectedRoute allowedRole="RECRUITER"><RecruiterProfile /></ProtectedRoute>
          } />
          <Route path="/recruiter/company" element={
            <ProtectedRoute allowedRole="RECRUITER"><CompanyProfile /></ProtectedRoute>
          } />
          <Route path="/admin/profile" element={
            <ProtectedRoute allowedRole="ADMIN"><AdminProfile /></ProtectedRoute>
          } />

          <Route path="/admin/students" element={
            <ProtectedRoute allowedRole="ADMIN"><ManageStudents /></ProtectedRoute>
          } />
          <Route path="/admin/recruiters" element={
            <ProtectedRoute allowedRole="ADMIN"><ManageRecruiters /></ProtectedRoute>
          } />
          <Route path="/admin/jobs" element={
            <ProtectedRoute allowedRole="ADMIN"><ManageJobs /></ProtectedRoute>
          } />
          <Route path="/admin/jobs/pending" element={
            <ProtectedRoute allowedRole="ADMIN"><ManageJobs /></ProtectedRoute>
          } />
          <Route path="/admin/analytics" element={
            <ProtectedRoute allowedRole="ADMIN"><Analytics /></ProtectedRoute>
          } />

          <Route path="/recruiter/profile" element={
            <ProtectedRoute allowedRole="RECRUITER"><CompanyProfile /></ProtectedRoute>
          } />
          <Route path="/recruiter/jobs" element={
            <ProtectedRoute allowedRole="RECRUITER"><MyJobs /></ProtectedRoute>
          } />
          <Route path="/recruiter/jobs/new" element={
            <ProtectedRoute allowedRole="RECRUITER"><JobForm /></ProtectedRoute>
          } />
          <Route path="/recruiter/jobs/:jobId/edit" element={
            <ProtectedRoute allowedRole="RECRUITER"><JobForm /></ProtectedRoute>
          } />
          <Route path="/recruiter/jobs/:jobId/applicants" element={
            <ProtectedRoute allowedRole="RECRUITER"><ApplicantsList /></ProtectedRoute>
          } />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;