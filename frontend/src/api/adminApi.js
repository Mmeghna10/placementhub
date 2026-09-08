import axiosInstance from './axiosInstance';

export const getDashboard = () => axiosInstance.get('/api/admin/dashboard');
export const getAllStudents = () => axiosInstance.get('/api/admin/students');
export const getAllRecruiters = () => axiosInstance.get('/api/admin/recruiters');
export const getAllJobs = () => axiosInstance.get('/api/admin/jobs');
export const getPendingJobs = () => axiosInstance.get('/api/admin/jobs/pending');
export const approveJob = (jobId, status) => axiosInstance.put(`/api/admin/jobs/${jobId}/approve`, { status });
export const getAnalytics = () => axiosInstance.get('/api/admin/analytics');