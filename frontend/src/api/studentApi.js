import axiosInstance from './axiosInstance';

export const getStudentProfile = () => axiosInstance.get('/api/students/profile');
export const updateStudentProfile = (data) => axiosInstance.put('/api/students/profile', data);
export const getApprovedJobs = () => axiosInstance.get('/api/students/jobs');
export const getRecommendedJobs = () => axiosInstance.get('/api/students/jobs/recommended');
export const applyToJob = (jobId) => axiosInstance.post(`/api/students/jobs/${jobId}/apply`);
export const getMyApplications = () => axiosInstance.get('/api/students/applications');
export const getApplicationDetails = (id) => axiosInstance.get(`/api/students/applications/${id}`);
// Interview slot booking (application must be in INTERVIEW)
export const getInterviewSlots = (applicationId) => axiosInstance.get(`/api/students/applications/${applicationId}/slots`);
export const bookInterviewSlot = (applicationId, slotId) => axiosInstance.post(`/api/students/applications/${applicationId}/slots/${slotId}/booking`);
export const cancelInterviewBooking = (applicationId) => axiosInstance.delete(`/api/students/applications/${applicationId}/booking`);

// Paginated job search; params: { q, jobType, location, eligibleOnly, sort, page, size }
export const searchJobs = (params) => axiosInstance.get('/api/students/jobs/search', { params });
export const getJob = (jobId) => axiosInstance.get(`/api/students/jobs/${jobId}`);
