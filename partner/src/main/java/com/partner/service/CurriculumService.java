package com.partner.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.partner.dto.CommonQueryDTO;
import com.partner.enums.SitesURL;
import com.partner.vo.ClassScheduleVO;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CurriculumService {

    public List<ClassScheduleVO> getWeeklySchedule(CommonQueryDTO dto) {
        String jsonResponse = fetchScheduleJson(dto);
        return parseScheduleJson(jsonResponse);
    }

    private String fetchScheduleJson(CommonQueryDTO dto) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS).build();

        FormBody body = new FormBody.Builder()
                .add("xnm", dto.getXnm() == null ? "" : dto.getXnm())
                .add("xqm", dto.getXqm() == null ? "" : dto.getXqm())
                .add("zs", dto.getZs() == null ? "" : dto.getZs())
                .add("kblx", "1")      // 课表类型
                .add("doType", "app")  // 移动端模式
                .add("xh", dto.getAccount())
                .build();

        // 注意：课表 URL 通常是 /jwglxt/kbcx/xskbcxMobile_cxXsKb.html
        String url = SitesURL.JWGL_BASE_URL.getUrl() + "/jwglxt/kbcx/xskbcxMobile_cxXsKb.html?gnmkdm=N2154";

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Cookie", "JSESSIONID=" + dto.getJsessionid() + "; route=" + dto.getRoute())
                .addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("HTTP Error: " + response.code());
            String res = response.body().string();
            if (res.contains("login") || res.contains("html")) throw new RuntimeException("登录过期");
            return res;
        } catch (IOException e) {
            throw new RuntimeException("课表查询失败", e);
        }
    }

    private List<ClassScheduleVO> parseScheduleJson(String json) {
        List<ClassScheduleVO> list = new ArrayList<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        // 核心数据在 kbList 数组
        if (root.has("kbList") && !root.get("kbList").isJsonNull()) {
            JsonArray kbList = root.getAsJsonArray("kbList");
            for (JsonElement e : kbList) {
                JsonObject kb = e.getAsJsonObject();
                ClassScheduleVO vo = new ClassScheduleVO();
                vo.setCourseName(getStr(kb, "kcmc")); // 课程名
                vo.setTeacher(getStr(kb, "xm"));      // 教师
                vo.setLocation(getStr(kb, "cdmc"));   // 教室
                vo.setWeekRange(getStr(kb, "zcd"));   // 周次
                vo.setDayOfWeek(getStr(kb, "xqjmc")); // 星期
                vo.setDayCode(getStr(kb, "xqj"));     // 星期代码
                vo.setSessionInfo(getStr(kb, "jc"));  // 节次
                vo.setCredit(getStr(kb, "xf"));       // 学分
                list.add(vo);
            }
        }
        return list;
    }

    private String getStr(JsonObject o, String k) {
        return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsString() : "";
    }
}