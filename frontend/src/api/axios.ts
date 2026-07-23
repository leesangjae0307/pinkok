import axios from 'axios';

const api = axios.create({
  baseURL: 'http://YOUR_IP:8080/api',
  //http://192.168.0.xxx:8080 로 바꾸면 됨
  timeout: 10000,
});

export default api;