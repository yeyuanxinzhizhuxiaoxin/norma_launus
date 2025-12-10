import request from "@/utils/request";

// ========================
// 1. 系统/登录相关
// ========================
export const adminLogin = (data) => request.post("/admin/login", data);

// ========================
// 2. 图书馆预约管理 (已有的)
// ========================
export const getLibraryUsers = () => request.get("/admin/library/users");
export const saveLibraryUser = (data) => request.post("/admin/library/user", data);
export const deleteLibraryUser = (studentId) => request.delete(`/admin/library/user/${studentId}`);
export const getUserTimeConfigs = (studentId) => request.get(`/admin/library/user/${studentId}/times`);
export const addTimeConfig = (data) => request.post("/admin/library/time", data);
export const updateTimeConfig = (data) => request.put("/admin/library/time", data);
export const deleteTimeConfig = (id) => request.delete(`/admin/library/time/${id}`);
export const testBooking = (data) => request.post("/admin/library/test-booking", data);

// ========================
// 3. 系统用户管理 (Client)
// ========================
export const getClients = (params) => request.get("/admin/clients", { params });
export const addClient = (data) => request.post("/admin/client", data);
export const deleteClient = (id) => request.delete(`/admin/client/${id}`);

// ========================
// 4. 课表管理 (Schedule)
// ========================
// params: { studentId, year, semester, week }
export const getSchedules = (params) => request.get("/admin/schedules", { params });
export const deleteSchedule = (id) => request.delete(`/admin/schedule/${id}`);

// ========================
// 5. 成绩管理 (Score)
// ========================
// params: { studentId, year, semester }
export const getScores = (params) => request.get("/admin/scores", { params });
export const deleteScore = (id) => request.delete(`/admin/score/${id}`);