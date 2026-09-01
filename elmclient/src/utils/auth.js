/**
 * Basic authentication utility functions
 */

// 这个文件应该只包含纯函数，用于操作 Token（存储、读取、删除）。它不应该包含任何 axios 或业务逻辑。

// Check if user is authenticated
export function isAuthenticated() {
    return !!localStorage.getItem('authToken');
}

// Save authentication token
export function setAuthToken(token) {
    localStorage.setItem('authToken', token);
}

// Remove authentication token
export function clearAuthToken() {
    localStorage.removeItem('authToken');
}

// Get authentication token
export function getAuthToken() {
    return localStorage.getItem('authToken');
}