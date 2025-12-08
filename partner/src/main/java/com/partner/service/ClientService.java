package com.partner.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.partner.entity.Client;
import com.partner.entity.LoginContext;
import com.partner.entity.Score;
import com.partner.entity.ScoreQuery;
import com.partner.enums.SitesURL;
import com.partner.mapper.ClientMapper;
import com.partner.mapper.ScoreMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.Double.parseDouble;


@Slf4j
@Service

public class ClientService {
    @Autowired
    ClientMapper clientMapper;

    @Autowired
    ScoreMapper scoreMapper;

    /**
     * 客户登录时插入数据
     * @param client
     */
    @Transactional
    public void InsertClient(Client client){
        clientMapper.insertClient(client);
    }

    /**
     * 根据客户账号查询客户
     * @param account
     * @return
     */
    @Transactional
    public Client findClientByAccount(String account){
        return clientMapper.findClientByAccount(account);
    }

    /**
     * 客户登录服务门户获取JSESSIONID
     * @param client
     * @return
     */

    /**
     * 客户登录服务门户，并返回已认证的 HttpClient（包含完整 Cookie 上下文）
     */
    //public OkHttpClient loginServicePortal(Client client) {
    public LoginContext loginServicePortal(Client client) {
        // 1. 为当前用户创建独立的 CookieManager 和 HttpClient
        CookieManager userCookieManager = new CookieManager();
        OkHttpClient userHttpClient = new OkHttpClient.Builder()
                .cookieJar(new JavaNetCookieJar(userCookieManager))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .followRedirects(false)
                .build();

        // 1.密码转码
        String encodedPassword = "{gilight}_" +
                Base64.getEncoder().encodeToString(client.getPassword().getBytes(StandardCharsets.UTF_8));
        log.info("转码后密码为: {}", encodedPassword);

        // 2. 获取 lt 和 execution
        String getLtUrl = SitesURL.CAS_BASE_URL.getUrl()+ "/cas/login" +
                "?action=getlt&service=" + URLEncoder.encode(SitesURL.SERVICE_URL.getUrl(), StandardCharsets.UTF_8);
        log.info("getLtUrl:{}",getLtUrl);
        Request getLtRequest = new Request.Builder().url(getLtUrl).build();

        try (Response ltResponse = userHttpClient.newCall(getLtRequest).execute()) {
            if (!ltResponse.isSuccessful()) {
                throw new IOException("获取 lt 失败，状态码: " + ltResponse.code());
            }

            String jsonp = ltResponse.body().string();
            String json = jsonp.substring(jsonp.indexOf('(') + 1, jsonp.lastIndexOf(')'));
            JsonObject data = JsonParser.parseString(json).getAsJsonObject();

            String lt = data.get("lt").getAsString();
            String execution = data.get("execution").getAsString();

            // 3. 提交登录表单
            FormBody formBody = new FormBody.Builder()
                    .add("username", client.getAccount())
                    .add("password", encodedPassword)
                    .add("lt", lt)
                    .add("execution", execution)
                    .add("_eventId", "submit")
                    .build();

            String loginUrl = SitesURL.CAS_BASE_URL.getUrl() + "/cas/login" +
                    "?submit=%E7%99%BB++%E5%BD%95&service=" + URLEncoder.encode(SitesURL.SERVICE_URL.getUrl(), StandardCharsets.UTF_8);

            Request loginRequest = new Request.Builder()
                    .url(loginUrl)
                    .post(formBody)
                    .build();

            try (Response loginResponse = userHttpClient.newCall(loginRequest).execute()) {
                if (loginResponse.code() != 302) {
                    throw new IOException("CAS 登录失败，状态码: " + loginResponse.code());
                }

                String location = loginResponse.header("Location");
                if (location == null || !location.contains("ticket=")) {
                    throw new IOException("无 ticket: " + location);
                }

                log.info("重定向到: {}", location);

                // 4. 用 ticket 换取业务系统会话（触发 JSESSIONID 设置）
                Request ticketRequest = new Request.Builder().url(location).build();
                try (Response ticketResponse = userHttpClient.newCall(ticketRequest).execute()) {
                    log.info("业务系统响应: {}", ticketResponse.code());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 5. 提取 JSESSIONID
        List<HttpCookie> cookies = userCookieManager.getCookieStore().getCookies();
        String jsession = cookies.stream()
                .filter(cookie -> "JSESSIONID".equals(cookie.getName()))
                .map(HttpCookie::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("登录成功但未找到 JSESSIONID"));

        log.info("最终 JSESSIONID: {}", jsession);
        //return userHttpClient;
        LoginContext loginContext = new LoginContext(userHttpClient,jsession,"");
        return loginContext;
    }

//    /**
//     * 使用已登录的会话，跳转进入教务系统
//     */
//    public String enterJiaowuSystem(LoginContext loginContext) throws IOException {
//    //public String enterJiaowuSystem(String js) throws IOException {
//        OkHttpClient clientWithRedirects = loginContext.getOkHttpClient().newBuilder()
//                .followRedirects(true)
//                .followSslRedirects(true)
//                .build();
//
//        Request request = new Request.Builder()
//                .url(SitesURL.JWGL_LOGIN_URL.getUrl())
//                .addHeader("Cookie", "JSESSIONID=" + loginContext.getJSESSIONID())
//                .build();
//
//        try (Response response = clientWithRedirects.newCall(request).execute()) {
//            log.info("最终教务系统响应状态: {}", response.code());
//
//            String body = response.body().string();
//
//            if (body.contains("欢迎") || body.contains("jsxsd") || body.contains("学生")) {
//                log.info("成功进入教务系统！");
//            } else {
//                log.warn("可能仍在登录页，响应片段:\n{}", body.substring(0, Math.min(500, body.length())));
//            }
//        return ;
//    }
    /**
     * 使用已登录的会话，跳转进入教务系统，并提取最终有效的 Cookies（包括 route）
     */
    public Map<String, String> enterJiaowuSystem(LoginContext loginContext) throws IOException {
        OkHttpClient client = loginContext.getOkHttpClient().newBuilder()
                .followRedirects(false) // 关键：关闭自动重定向
                .followSslRedirects(false)
                .build();

        String currentUrl = SitesURL.JWGL_LOGIN_URL.getUrl();
        String jsessionid = loginContext.getJSESSIONID();
        Map<String, String> cookies = new HashMap<>();
        cookies.put("JSESSIONID", jsessionid);

        int maxRedirects = 10; // 防止无限循环
        for (int i = 0; i < maxRedirects; i++) {
            Request.Builder requestBuilder = new Request.Builder()
                    .url(currentUrl);

            // 构建 Cookie 头：key1=value1; key2=value2
            String cookieHeader = cookies.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("; "));
            if (!cookieHeader.isEmpty()) {
                requestBuilder.addHeader("Cookie", cookieHeader);
            }

            Request request = requestBuilder.build();
            try (Response response = client.newCall(request).execute()) {
                log.info("跳转步骤 {}: {} -> 状态码 {}", i + 1, currentUrl, response.code());

                // 提取 Set-Cookie 中的新 Cookie（包括 route）
                List<String> setCookieHeaders = response.headers("Set-Cookie");
                for (String setCookie : setCookieHeaders) {
                    // 解析 "route=abc123; Path=/; HttpOnly" 这样的字符串
                    String[] parts = setCookie.split(";");
                    if (parts.length > 0) {
                        String cookiePart = parts[0].trim(); // "route=abc123"
                        if (cookiePart.contains("=")) {
                            String[] kv = cookiePart.split("=", 2);
                            if (kv.length == 2) {
                                String name = kv[0].trim();
                                String value = kv[1].trim();
                                cookies.put(name, value);
                                log.debug("捕获 Cookie: {} = {}", name, value);
                            }
                        }
                    }
                }

                // 检查是否需要重定向
                if (response.code() >= 300 && response.code() < 400) {
                    String location = response.header("Location");
                    if (location == null) break;

                    // 处理相对路径
                    currentUrl = resolveUrl(currentUrl, location);
                    log.info("重定向到: {}", currentUrl);
                } else {
                    // 到达最终页面
                    String body = response.body().string();
                    if (body.contains("欢迎") || body.contains("jsxsd") || body.contains("学生")) {
                        log.info("成功进入教务系统！");
                    } else {
                        log.warn("可能未成功登录，响应片段:\n{}",
                                body.substring(0, Math.min(500, body.length())));
                    }
                    break;
                }
            }
        }

        return cookies; // 包含 JSESSIONID 和 route 等
    }
    /**
     * xnm 学年，如 "2023"；为空则查当前学年
     * 学期，"3"=秋季，"12"=春季；为空则查当前学期
     * @param scoreQuery
     * @return
     */
    @Transactional
    public List<Score> queryScore(ScoreQuery scoreQuery) {
        log.info("成绩查询参数{}",scoreQuery.toString());
        //log.info("正在查询成绩：学年={}，学期={}", scoreQuery.getXnmmc(), scoreQuery.getXqmmc());

        // 1.从参数中获取 JSESSIONID,route
        String jsession = scoreQuery.getJSESSIONID();
        String route = scoreQuery.getRoute();
        if (jsession == null || jsession.trim().isEmpty()) {
            throw new IllegalArgumentException("JSESSIONID 不能为空");
        }

        FormBody formBody = new FormBody.Builder()
                .add("xnm", scoreQuery.getXnmmc() == null ? "" : scoreQuery.getXnmmc())
                .add("xqm", scoreQuery.getXqmmc() == null ? "" : scoreQuery.getXqmmc())
                .add("sfzgcj", "")
                .add("kcbj", "")
                .add("_search", "false")
                .add("nd", String.valueOf(Instant.now().toEpochMilli()))
                .add("queryModel.showCount", "30")
                .add("queryModel.currentPage", "1")
                //.add("queryModel.sortName", "")
                .add("queryModel.sortOrder", "asc")
                .add("time", "2")
                .build();

        Request request = new Request.Builder()
                .url(SitesURL.SCORE_QUERY_URL.getUrl())
                .post(formBody)
                // 2.手动设置 Cookie 头
                .addHeader("Cookie", "JSESSIONID=" + jsession +"; route="+ route)
                .addHeader("Referer", SitesURL.JWGL_BASE_URL.getUrl() + "/jwglxt/cjcx/cjcx_cxDgXscj.html?gnmkdm=N305005&layout=default")
                .addHeader("Origin", SitesURL.JWGL_BASE_URL.getUrl())
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
                .build();

        // 3. 使用最简 OkHttpClient（无需 CookieJar）
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .followRedirects(false) // 成绩查询是 AJAX，通常不重定向
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("成绩查询失败，HTTP 状态码: " + response.code());
            }

            String responseBody = response.body().string();
            log.debug("教务系统返回原始数据（前200字符）: {}", responseBody.substring(0, Math.min(200, responseBody.length())));

            // 检查是否返回了登录页（说明 JSESSIONID 无效）
            if (responseBody.contains("<html") || responseBody.contains("login") || responseBody.contains("未登录")) {
                throw new SecurityException("JSESSIONID 已失效或无效，请重新登录");
            }

            // 解析 JSON
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            var items = root.getAsJsonArray("items");

            List<Score> scoreList = new ArrayList<>();
            if (items != null) {
                for (int i = 0; i < items.size(); i++) {
                    JsonObject item = items.get(i).getAsJsonObject();

                    Score score = new Score();
                    score.setStudentId(scoreQuery.getAccount());
                    score.setYear(item.has("xnmmc") ? item.get("xnmmc").getAsString() : "");
                    score.setSemester(item.has("xqmmc") ? item.get("xqmmc").getAsString() : "");
                    score.setCourseName(item.has("kcmc") ? item.get("kcmc").getAsString() : "");

                    // 安全转换字符串为 Double
                    score.setCredit(parseDouble(item, "xf"));
                    score.setGrade(parseDouble(item, "cj")); // zcj = 最终成绩
                    score.setPoint(parseDouble(item, "jd"));  // 如果有 jd 字段
                    if(score.getGrade()== null){
                        score.setGrade(score.getPoint()*10+40);
                    }
                    score.setGpa(parseDouble(item,"xfjd"));// 注意：gpa = credit * point，可计算也可直接取 xfjd（如果返回）

                    log.info("-------------------{}-{}-{}-{}",
                            score.getGpa(),parseDouble(item,"xfjd"),score.getPoint(),score.getCredit());
                    if((score.getGpa()==0||parseDouble(item,"xfjd")!=null) && score.getPoint()!=0&score.getCredit()!=0){
                        //score.setGpa(score.getCredit()*score.getPoint());
                    }
                    scoreList.add(score);
                }
            }

            log.info("成功解析 {} 门课程成绩", scoreList.size());

            // 将查询的成绩存储到数据库（仅插入新数据）
            String studentId = scoreQuery.getAccount();
            // 1. 查询该学生已存在的课程名
            List<String> existingCourses = scoreMapper.findExistingCourseNamesByStudentId(studentId);
            Set<String> existingSet = new HashSet<>(existingCourses);

            // 2. 过滤出新成绩
            List<Score> newScores = scoreList.stream()
                    .filter(score -> !existingSet.contains(score.getCourseName()))
                    .peek(score -> {
                        // 补充必要字段
                        score.setStudentId(studentId);
                        score.setCreateTime(LocalDateTime.now());
                        score.setUpdateTime(LocalDateTime.now());
                    })
                    .collect(Collectors.toList());

            // 3. 批量插入新成绩
            if (!newScores.isEmpty()) {
                scoreMapper.insertScores(newScores);
                log.info("成功插入 {} 条新成绩记录", newScores.size());
            } else {
                log.info("无新成绩需要插入");
            }

            return scoreList;
        } catch (IOException e) {
            log.error("查询成绩时发生 IO 异常", e);
            throw new RuntimeException("查询成绩失败: " + e.getMessage(), e);
        }
    }
    private Double parseDouble(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            String value = obj.get(key).getAsString().trim(); // ← 这里需要字符串
            if (!value.isEmpty() && !value.equals("null") && !value.equals("--")) {
                try {
                    return new BigDecimal(Double.parseDouble(value)).setScale(2, RoundingMode.HALF_UP).doubleValue();
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    /**
     * 将相对重定向地址解析为绝对 URL
     */
    private String resolveUrl(String baseUrl, String location) {
        try {
            // 使用 baseUrl 作为上下文，解析 location（支持相对路径）
            return new URL(new URL(baseUrl), location).toString();
        } catch (MalformedURLException e) {
            // 如果解析失败，直接返回 location（可能是完整 URL）
            log.warn("无法解析重定向 URL: base={}, location={}", baseUrl, location, e);
            return location;
        }
    }
}
