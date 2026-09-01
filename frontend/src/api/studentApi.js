import axiosInstance from './axiosInstance';

export const getStudentProfile = () => axiosInstance.get('/api/students/profile');
export const updateStudentProfile = (data) => axiosInstance.put('/api/students/profile', data);
export const getApprovedJobs = () => axiosInstance.get('/api/students/jobs');
export const getRecommendedJobs = () => axiosInstance.get('/api/students/jobs/recommended');
export const applyToJob = (jobId) => axiosInstance.post(`/api/students/jobs/${jobId}/apply`);
export const getMyApplications = () => axiosInstance.get('/api/students/applications');
export const getApplicationDetails = (id) => axiosInstance.get(`/api/students/applications/${id}`);