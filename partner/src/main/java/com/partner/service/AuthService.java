package com.partner.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.partner.dto.LoginDTO;
import com.partner.entity.Client;
import com.partner.entity.LoginContext;
import com.partner.enums.SitesURL;
import com.partner.mapper.ClientMapper;
import com.partner.vo.LoginVO;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthService {

    @Autowired
    ClientMapper clientMapper;

    /**
     * 登录主逻辑：包含数据库注册和爬虫模拟登录
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginDTO loginDTO) {
        Client dbClient = clientMapper.findClientByAccount(loginDTO.getAccount());

        // 注册逻辑更新
        if (dbClient == null) {
            Client newClient = new Client();
            newClient.setAccount(loginDTO.getAccount());
            newClient.setPassword(loginDTO.getPassword());
            newClient.setName("同学" + loginDTO.getAccount()); // 默认昵称
            newClient.setRole(0); // 默认为普通用户

            clientMapper.insertClient(newClient);
            dbClient = newClient; // 更新引用以便后续使用
        }

        // 2. 爬虫逻辑：执行 CAS 登录
        LoginContext ctx = loginServicePortal(client);

        try {
            // 3. 爬虫逻辑：获取教务系统最终 Cookies
            Map<String, String> cookies = enterJiaowuSystem(ctx);

            return LoginVO.builder()
                    .jsessionid(cookies.get("JSESSIONID"))
                    .route(cookies.getOrDefault("route", ""))
                    .build();
        } catch (IOException e) {
            log.error("登录异常", e);
            throw new RuntimeException("登录教务系统失败: " + e.getMessage());
        }
    }

    // --- 以下为私有爬虫底层方法 (从原 ClientService 迁移) ---

    private LoginContext loginServicePortal(Client client) {
        CookieManager userCookieManager = new CookieManager();
        OkHttpClient userHttpClient = new OkHttpClient.Builder()
                .cookieJar(new JavaNetCookieJar(userCookieManager))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .followRedirects(false)
                .build();

        try {
            // 1. 密码转码
            String encodedPassword = "{gilight}_" +
                    Base64.getEncoder().encodeToString(client.getPassword().getBytes(StandardCharsets.UTF_8));

            // 2. 获取 lt 和 execution
            String getLtUrl = SitesURL.CAS_BASE_URL.getUrl() + "/cas/login" +
                    "?action=getlt&service=" + URLEncoder.encode(SitesURL.SERVICE_URL.getUrl(), StandardCharsets.UTF_8);

            Request getLtRequest = new Request.Builder().url(getLtUrl).build();
            String lt, execution;

            try (Response ltResponse = userHttpClient.newCall(getLtRequest).execute()) {
                if (!ltResponse.isSuccessful()) throw new IOException("获取 lt 失败");
                String jsonp = ltResponse.body().string();
                String json = jsonp.substring(jsonp.indexOf('(') + 1, jsonp.lastIndexOf(')'));
                JsonObject data = JsonParser.parseString(json).getAsJsonObject();
                lt = data.get("lt").getAsString();
                execution = data.get("execution").getAsString();
            }

            // 3. 提交登录
            FormBody formBody = new FormBody.Builder()
                    .add("username", client.getAccount())
                    .add("password", encodedPassword)
                    .add("lt", lt)
                    .add("execution", execution)
                    .add("_eventId", "submit")
                    .build();

            String loginUrl = SitesURL.CAS_BASE_URL.getUrl() + "/cas/login" +
                    "?submit=%E7%99%BB++%E5%BD%95&service=" + URLEncoder.encode(SitesURL.SERVICE_URL.getUrl(), StandardCharsets.UTF_8);

            Request loginRequest = new Request.Builder().url(loginUrl).post(formBody).build();

            try (Response loginResponse = userHttpClient.newCall(loginRequest).execute()) {
                if (loginResponse.code() != 302) throw new IOException("CAS 登录验证失败");
                String location = loginResponse.header("Location");
                if (location == null || !location.contains("ticket=")) throw new IOException("未获取到 ticket");

                // 4. 换取 Ticket
                Request ticketRequest = new Request.Builder().url(location).build();
                try (Response ticketResponse = userHttpClient.newCall(ticketRequest).execute()) {
                    // 触发 CookieJar 自动保存 JSESSIONID
                }
            }

            // 5. 提取 JSESSIONID
            List<HttpCookie> cookies = userCookieManager.getCookieStore().getCookies();
            String jsession = cookies.stream()
                    .filter(c -> "JSESSIONID".equals(c.getName()))
                    .map(HttpCookie::getValue).findFirst()
                    .orElseThrow(() -> new IllegalStateException("未找到 JSESSIONID"));

            return new LoginContext(userHttpClient, jsession, "");

        } catch (Exception e) {
            throw new RuntimeException("CAS 认证服务异常", e);
        }
    }

    private Map<String, String> enterJiaowuSystem(LoginContext ctx) throws IOException {
        OkHttpClient client = ctx.getOkHttpClient().newBuilder()
                .followRedirects(false).followSslRedirects(false).build();

        String currentUrl = SitesURL.JWGL_LOGIN_URL.getUrl();
        Map<String, String> cookies = new HashMap<>();
        cookies.put("JSESSIONID", ctx.getJSESSIONID());

        for (int i = 0; i < 10; i++) {
            Request.Builder req = new Request.Builder().url(currentUrl);
            String cookieStr = cookies.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("; "));
            if (!cookieStr.isEmpty()) req.addHeader("Cookie", cookieStr);

            try (Response response = client.newCall(req.build()).execute()) {
                // 提取 Set-Cookie (特别是 route)
                List<String> setCookies = response.headers("Set-Cookie");
                for (String sc : setCookies) {
                    String[] parts = sc.split(";")[0].split("=", 2);
                    if (parts.length == 2) cookies.put(parts[0].trim(), parts[1].trim());
                }

                if (response.code() >= 300 && response.code() < 400) {
                    String location = response.header("Location");
                    if (location == null) break;
                    try {
                        currentUrl = new URL(new URL(currentUrl), location).toString();
                    } catch (MalformedURLException e) {
                        currentUrl = location;
                    }
                } else {
                    break;
                }
            }
        }
        return cookies;
    }
}