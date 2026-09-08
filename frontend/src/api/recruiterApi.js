import axiosInstance from './axiosInstance';

export const getRecruiterProfile = () => axiosInstance.get('/api/recruiter/profile');
export const updateRecruiterProfile = (data) => axiosInstance.put('/api/recruiter/profile', data);
export const createJob = (data) => axiosInstance.post('/api/recruiter/jobs', data);
export const getMyJobs = () => axiosInstance.get('/api/recruiter/jobs');
export const updateJob = (id, data) => axiosInstance.put(`/api/recruiter/jobs/${id}`, data);
export const deleteJob = (id) => axiosInstance.delete(`/api/recruiter/jobs/${id}`);
export const getApplicantsForJob = (jobId) => axiosInstance.get(`/api/recruiter/jobs/${jobId}/applications`);
export const updateApplicationStatus = (id, data) => axiosInstance.put(`/api/recruiter/applications/${id}/status`, data);