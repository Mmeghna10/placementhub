import axiosInstance from './axiosInstance';

export const sendChatMessage = (messages) =>
  axiosInstance.post('/api/chat', { messages });