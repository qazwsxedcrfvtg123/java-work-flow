/**
 * Axios 请求拦截器配置 - 支持 JWT Token 自动刷新
 * 
 * 功能：
 * 1. 自动在请求头添加 Access Token
 * 2. 检测 Token 过期（401）自动刷新
 * 3. 防止重复刷新
 * 4. 队列化失败请求
 */

import axios from 'axios';

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

// 标记是否正在刷新
let isRefreshing = false;

// 失败请求队列
let failedQueue = [];

// 处理队列中的请求
const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  
  failedQueue = [];
};

// 请求拦截器 - 添加 Access Token
request.interceptors.request.use(
  config => {
    const accessToken = localStorage.getItem('accessToken');
    
    if (accessToken) {
      // 检查 Token 是否快过期（剩余时间 < 5 分钟）
      const payload = JSON.parse(atob(accessToken.split('.')[1]));
      const expiryTime = payload.exp * 1000;
      const now = Date.now();
      const timeLeft = expiryTime - now;
      
      // 如果快过期，提前刷新（可选）
      if (timeLeft < 300000) { // 5 分钟
        console.log('Token 快过期了，建议刷新');
      }
      
      config.headers['Authorization'] = `Bearer ${accessToken}`;
    }
    
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 响应拦截器 - 自动刷新 Token
request.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;

    // 如果是 401 且不是刷新请求
    if (error.response?.status === 401 && !originalRequest._retry) {
      // 检查是否是 Token 过期
      const isTokenExpired = error.response.headers['x-token-expired'] === 'true';
      
      if (!isTokenExpired) {
        // 其他 401 错误（如未登录），直接跳转登录页
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(error);
      }

      // 如果已经在刷新中，加入队列
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
        .then(token => {
          originalRequest.headers['Authorization'] = 'Bearer ' + token;
          return request(originalRequest);
        })
        .catch(err => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // 获取 Refresh Token
        const refreshToken = localStorage.getItem('refreshToken');
        
        if (!refreshToken) {
          // 没有 Refresh Token，跳转登录
          localStorage.clear();
          window.location.href = '/login';
          return Promise.reject(error);
        }

        // 调用刷新接口
        const response = await axios.post(
          '/api/auth/refresh',
          null,
          {
            params: { refreshToken }
          }
        );

        const { accessToken, expiresIn } = response.data;
        
        // 保存新 Token
        localStorage.setItem('accessToken', accessToken);
        
        // 处理等待队列
        processQueue(null, accessToken);
        
        // 重试原请求
        originalRequest.headers['Authorization'] = 'Bearer ' + accessToken;
        return request(originalRequest);
        
      } catch (refreshError) {
        // 刷新失败（Refresh Token 过期或无效）
        processQueue(refreshError, null);
        
        // 清除本地存储
        localStorage.clear();
        
        // 跳转到登录页
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
        
        return Promise.reject(refreshError);
        
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default request;
