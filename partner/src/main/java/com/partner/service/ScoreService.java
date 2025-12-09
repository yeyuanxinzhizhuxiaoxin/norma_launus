package com.partner.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.partner.dto.CommonQueryDTO;
import com.partner.entity.Score;
import com.partner.enums.SitesURL;
import com.partner.mapper.ScoreMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ScoreService {

    @Autowired
    ScoreMapper scoreMapper;

    @Transactional(rollbackFor = Exception.class)
    public List<Score> syncScores(CommonQueryDTO queryDTO) {
        // 1. 爬虫获取数据
        String jsonResponse = fetchScoreJson(queryDTO);

        // 2. 解析数据
        List<Score> fetchedScores = parseScoreJson(jsonResponse, queryDTO.getAccount());

        // 3. 数据库查重与入库
        saveNewScores(queryDTO.getAccount(), fetchedScores);

        return fetchedScores;
    }

    private String fetchScoreJson(CommonQueryDTO dto) {
        if (dto.getJsessionid() == null) throw new IllegalArgumentException("Session 失效");

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build();

        FormBody body = new FormBody.Builder()
                .add("xnm", dto.getXnm() == null ? "" : dto.getXnm())
                .add("xqm", dto.getXqm() == null ? "" : dto.getXqm())
                .add("sfzgcj", "").add("_search", "false")
                .add("nd", String.valueOf(Instant.now().toEpochMilli()))
                .add("queryModel.showCount", "100").add("queryModel.currentPage", "1")
                .add("queryModel.sortOrder", "asc").add("time", "2")
                .build();

        Request request = new Request.Builder()
                .url(SitesURL.SCORE_QUERY_URL.getUrl())
                .post(body)
                .addHeader("Cookie", "JSESSIONID=" + dto.getJsessionid() + "; route=" + dto.getRoute())
                .addHeader("Referer", SitesURL.JWGL_BASE_URL.getUrl() + "/jwglxt/cjcx/cjcx_cxDgXscj.html")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("请求失败: " + response.code());
            String res = response.body().string();
            if (res.contains("login") || res.contains("html")) throw new RuntimeException("登录过期");
            return res;
        } catch (IOException e) {
            throw new RuntimeException("网络请求异常", e);
        }
    }

    private List<Score> parseScoreJson(String json, String studentId) {
        List<Score> list = new ArrayList<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray items = root.getAsJsonArray("items");
        if (items != null) {
            for (JsonElement e : items) {
                JsonObject obj = e.getAsJsonObject();
                Score s = new Score();
                s.setStudentId(studentId);
                s.setYear(getStr(obj, "xnmmc"));
                s.setSemester(getStr(obj, "xqmmc"));
                s.setCourseName(getStr(obj, "kcmc"));
                s.setCredit(getDbl(obj, "xf"));
                s.setGrade(getDbl(obj, "cj"));
                s.setPoint(getDbl(obj, "jd"));
                s.setGpa(getDbl(obj, "xfjd"));
                // 简单的兼容逻辑
                if (s.getGrade() == null && s.getPoint() != null) s.setGrade(s.getPoint() * 10 + 40);
                list.add(s);
            }
        }
        return list;
    }

    private void saveNewScores(String studentId, List<Score> scores) {
        List<String> existList = scoreMapper.findExistingCourseNamesByStudentId(studentId);
        Set<String> existSet = new HashSet<>(existList);
        List<Score> newScores = scores.stream()
                .filter(s -> !existSet.contains(s.getCourseName()))
                .peek(s -> {
                    s.setCreateTime(LocalDateTime.now());
                    s.setUpdateTime(LocalDateTime.now());
                }).collect(Collectors.toList());

        if (!newScores.isEmpty()) {
            scoreMapper.insertScores(newScores);
            log.info("入库 {} 条新成绩", newScores.size());
        }
    }

    // 工具方法
    private String getStr(JsonObject o, String k) {
        return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsString() : "";
    }
    private Double getDbl(JsonObject o, String k) {
        if (o.has(k) && !o.get(k).isJsonNull()) {
            String v = o.get(k).getAsString().trim();
            if (!v.isEmpty() && !v.equals("null") && !v.equals("--")) {
                try {
                    return new BigDecimal(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
                } catch (Exception ignored) {}
            }
        }
        return null;
    }
}