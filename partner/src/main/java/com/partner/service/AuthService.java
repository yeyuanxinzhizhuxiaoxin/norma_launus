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
     * 登录逻辑：
     * 1. 先尝试爬虫登录教务系统 (验证账号有效性)
     * 2. 验证通过后，检查数据库
     * 3. 如果是首次登录，将用户信息存入数据库
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginDTO loginDTO) {
        // --- 1. 验证阶段：尝试登录教务系统 ---
        log.info("尝试登录教务系统验证账号: {}", loginDTO.getAccount());

        // 构建临时 Client 对象用于爬虫验证
        Client tempClient = new Client();
        tempClient.setAccount(loginDTO.getAccount());
        tempClient.setPassword(loginDTO.getPassword());

        LoginContext ctx;
        Map<String, String> cookies;

        try {
            // 执行爬虫登录
            ctx = loginServicePortal(tempClient);
            // 获取最终 Cookies
            cookies = enterJiaowuSystem(ctx);
        } catch (Exception e) {
            // 登录失败（密码错误、系统崩溃等），直接抛出异常，不操作数据库
            log.warn("教务系统登录失败: {}", e.getMessage());
            throw new RuntimeException("登录失败：请检查账号密码是否正确，或教务系统是否开放。");
        }

        // --- 2. 数据库阶段：处理本地用户存储 ---
        Client dbClient = clientMapper.findClientByAccount(loginDTO.getAccount());

        if (dbClient == null) {
            // 首次登录：创建新用户并入库
            dbClient = new Client();
            dbClient.setAccount(loginDTO.getAccount());
            dbClient.setPassword(loginDTO.getPassword());
            dbClient.setName("同学" + loginDTO.getAccount()); // 设置默认昵称
            dbClient.setRole(0); // 默认为普通用户

            clientMapper.insertClient(dbClient);
            log.info("新用户首次登录，已自动入库: {}", loginDTO.getAccount());
        } else {
            // 非首次登录
            log.info("老用户登录: {}", loginDTO.getAccount());
            // (可选) 如果需要，可以在这里更新数据库中的密码，保持与教务系统一致
            /*
            if (!dbClient.getPassword().equals(loginDTO.getPassword())) {
                dbClient.setPassword(loginDTO.getPassword());
                // clientMapper.updateClient(dbClient); // 需要在Mapper中添加update方法
            }
            */
        }

        // --- 3. 返回结果 ---
        return LoginVO.builder()
                .jsessionid(cookies.get("JSESSIONID"))
                .route(cookies.getOrDefault("route", ""))
                .role(dbClient.getRole()) // 返回数据库中的角色
                .name(dbClient.getName())
                .build();
    }

    // --- 以下为爬虫底层私有方法 (保持原有逻辑不变) ---

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
                // CAS 登录成功通常重定向(302)，如果返回200通常意味着还在登录页(失败)
                if (loginResponse.code() != 302) throw new IOException("CAS 登录验证失败(密码错误)");

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
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> enterJiaowuSystem(LoginContext ctx) throws IOException {
        OkHttpClient client = ctx.getOkHttpClient().newBuilder()
                .followRedirects(false).followSslRedirects(false).build();

        String currentUrl = SitesURL.JWGL_LOGIN_URL.getUrl();
        Map<String, String> cookies = new HashMap<>();
        cookies.put("JSESSIONID", ctx.getJSESSIONID());

        // 尝试跳转并处理重定向，以获取 Route 等 Cookie
        for (int i = 0; i < 10; i++) {
            Request.Builder req = new Request.Builder().url(currentUrl);
            String cookieStr = cookies.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("; "));
            if (!cookieStr.isEmpty()) req.addHeader("Cookie", cookieStr);

            try (Response response = client.newCall(req.build()).execute()) {
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