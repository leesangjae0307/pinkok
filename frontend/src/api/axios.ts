import axios from 'axios';

// 실기기(Galaxy) USB 연결 시: PowerShell에서 `adb reverse tcp:8080 tcp:8080` 실행 후 localhost 사용
// Wi-Fi로만 연결할 경우: PC의 IPv4 주소로 변경 (예: 'http://192.168.0.10:8080')
export const BASE_URL = 'http://localhost:8080';

const api = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

export default api;
