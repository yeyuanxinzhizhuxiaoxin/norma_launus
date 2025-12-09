package com.partner.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP工具类 (Library 专用)
 * 重写为 OkHttp 实现，适配现有项目依赖
 */
@Slf4j
@Component
public class LibraryHttpUtil {

    private static final String BASE_URL = "https://wslib.haut.edu.cn/stage-api";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36";

    // 复用 OkHttpClient 实例
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();

    /**
     * 获取验证码UUID
     */
    public String getCaptchaUuid() throws Exception {
        String url = BASE_URL + "/captchaImage";

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("请求失败: " + response.code());
            String responseBody = response.body() != null ? response.body().string() : "";

            log.debug("验证码响应: {}", responseBody);
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            String uuid = null;
            if (jsonNode.has("data") && jsonNode.get("data").has("uuid")) {
                uuid = jsonNode.get("data").get("uuid").asText();
            } else if (jsonNode.has("uuid")) {
                uuid = jsonNode.get("uuid").asText();
            }

            if (uuid == null || uuid.isEmpty()) {
                throw new Exception("获取验证码UUID失败: " + responseBody);
            }
            return uuid;
        }
    }

    /**
     * 用户登录
     */
    public LoginResult login(String userId, String password, String uuid) throws Exception {
        String url = BASE_URL + "/login";

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("username", userId);
        requestMap.put("password", password);
        requestMap.put("code", "1");
        requestMap.put("uuid", uuid);

        String jsonBody = objectMapper.writeValueAsString(requestMap);
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("登录请求网络失败: " + response.code());
            String responseBody = response.body() != null ? response.body().string() : "";

            log.debug("登录响应: {}", responseBody);
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            if (!jsonNode.has("code") || jsonNode.get("code").asInt() != 200) {
                String msg = jsonNode.has("msg") ? jsonNode.get("msg").asText() : responseBody;
                throw new Exception("登录业务失败: " + msg);
            }

            // 提取 token
            String token = null;
            JsonNode dataNode = jsonNode.get("data");
            if (dataNode != null) {
                if (dataNode.has("token")) token = dataNode.get("token").asText();
                else if (dataNode.has("fresh_token")) token = dataNode.get("fresh_token").asText();
            }
            if (token == null && jsonNode.has("token")) token = jsonNode.get("token").asText();

            if (token == null || token.isEmpty()) {
                throw new Exception("登录成功但未获取到Token");
            }

            // 提取 ticket
            String ticket = (dataNode != null && dataNode.has("ticket")) ? dataNode.get("ticket").asText() : "";

            log.info("图书馆登录成功, 用户: {}", userId);
            return new LoginResult(token, ticket);
        }
    }

    /**
     * 预约座位
     */
    public BookingResult bookSeat(Integer seatId, String startTime, String endTime, String token, String ticket) throws Exception {
        // 构建带参数的 URL
        HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + "/api/seatbook/user/addbooking").newBuilder();
        urlBuilder.addQueryParameter("channel", "1001");
        urlBuilder.addQueryParameter("seatid", String.valueOf(seatId));
        urlBuilder.addQueryParameter("starttime", startTime);
        urlBuilder.addQueryParameter("endtime", endTime);
        urlBuilder.addQueryParameter("terminal", "WEB");

        String url = urlBuilder.build().toString();

        // 构建 Cookie
        StringBuilder cookieBuilder = new StringBuilder();
        cookieBuilder.append("Admin-Token=").append(token);
        if (ticket != null && !ticket.isEmpty()) {
            cookieBuilder.append("; my_client_ticket=").append(ticket);
        }

        Request request = new Request.Builder()
                .url(url)
                .get() // GET 请求
                .header("Authorization", "Bearer " + token)
                .header("Cookie", cookieBuilder.toString())
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("authority", "wslib.haut.edu.cn")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("预约请求网络失败: " + response.code());
            String responseBody = response.body() != null ? response.body().string() : "";

            log.info("预约响应 - 座位: {}, 响应: {}", seatId, responseBody);
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            boolean success = jsonNode.has("code") && jsonNode.get("code").asInt() == 200;
            String message = jsonNode.has("msg") ? jsonNode.get("msg").asText() : "";
            if (!success && message.isEmpty() && jsonNode.has("message")) {
                message = jsonNode.get("message").asText();
            }
            if (message.isEmpty()) message = success ? "预约成功" : "预约失败(未知原因)";

            return new BookingResult(success, message, responseBody);
        }
    }

    // --- 内部类 ---

    public static class LoginResult {
        private final String token;
        private final String ticket;

        public LoginResult(String token, String ticket) {
            this.token = token;
            this.ticket = ticket;
        }
        public String getToken() { return token; }
        public String getTicket() { return ticket; }
    }

    public static class BookingResult {
        private final boolean success;
        private final String message;
        private final String rawResponse;

        public BookingResult(boolean success, String message, String rawResponse) {
            this.success = success;
            this.message = message;
            this.rawResponse = rawResponse;
        }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getRawResponse() { return rawResponse; }
    }
}