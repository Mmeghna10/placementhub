import axiosInstance from './axiosInstance';

export const getDashboard = () => axiosInstance.get('/api/admin/dashboard');
export const getAllStudents = (page = 0, size = 20) => axiosInstance.get('/api/admin/students', { params: { page, size } });
export const getAllRecruiters = (page = 0, size = 20) => axiosInstance.get('/api/admin/recruiters', { params: { page, size } });
export const getAllJobs = (page = 0, size = 20) => axiosInstance.get('/api/admin/jobs', { params: { page, size } });
export const getPendingJobs = () => axiosInstance.get('/api/admin/jobs/pending');
export const approveJob = (jobId, status) => axiosInstance.put(`/api/admin/jobs/${jobId}/approve`, { status });
export const getAnalytics = () => axiosInstance.get('/api/admin/analytics');