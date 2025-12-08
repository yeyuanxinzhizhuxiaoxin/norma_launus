import request from "@/utils/request";

//登录
export const loginApi = (data) => request.post("/client/login", data);

//查询成绩
export const queryScoreApi = (scoreQuery) => request.post("/client/queryScore", scoreQuery);

//查询课表
