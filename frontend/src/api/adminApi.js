import axiosInstance from './axiosInstance';

export const getDashboard = () => axiosInstance.get('/api/admin/dashboard');
export const getAllStudents = (page = 0, size = 20) => axiosInstance.get('/api/admin/students', { params: { page, size } });
export const getAllRecruiters = (page = 0, size = 20) => axiosInstance.get('/api/admin/recruiters', { params: { page, size } });
export const getAllJobs = (page = 0, size = 20) => axiosInstance.get('/api/admin/jobs', { params: { page, size } });
export const getPendingJobs = () => axiosInstance.get('/api/admin/jobs/pending');
// tier (REGULAR | DREAM | SUPER_DREAM) applies when approving
export const approveJob = (jobId, status, tier) => axiosInstance.put(`/api/admin/jobs/${jobId}/approve`, { status, tier });
export const getPlacementPolicy = () => axiosInstance.get('/api/admin/placement-policy');
export const updatePlacementPolicy = (policy) => axiosInstance.put('/api/admin/placement-policy', policy);
export const getAnalytics = () => axiosInstance.get('/api/admin/analytics');