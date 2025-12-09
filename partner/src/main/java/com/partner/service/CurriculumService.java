package com.partner.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.partner.dto.CommonQueryDTO;
import com.partner.entity.Curriculum;
import com.partner.enums.SitesURL;
import com.partner.mapper.CurriculumMapper;
import com.partner.vo.ClassScheduleVO;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CurriculumService {

    @Autowired
    CurriculumMapper curriculumMapper;

    /**
     * 获取周课表 (带数据库缓存)
     */
    @Transactional(rollbackFor = Exception.class)
    public List<ClassScheduleVO> getWeeklySchedule(CommonQueryDTO dto) {
        String studentId = dto.getAccount();
        String year = dto.getXnm();
        String semester = dto.getXqm();

        // 1. 先查询数据库
        List<Curriculum> dbList = curriculumMapper.findCurriculum(studentId, year, semester);

        if (dbList != null && !dbList.isEmpty()) {
            log.info("从数据库命中课表数据: 学号={}, 学年={}, 学期={}", studentId, year, semester);
            // 将 Entity 转换为 VO 返回
            return dbList.stream().map(this::entityToVo).collect(Collectors.toList());
        }

        // 2. 数据库无数据，执行爬虫逻辑
        log.info("数据库无数据，开始爬取课表: {}", studentId);
        String jsonResponse = fetchScheduleJson(dto);

        // 3. 解析 JSON 为 VO
        List<ClassScheduleVO> voList = parseScheduleJson(jsonResponse);

        // 4. 将 VO 转换为 Entity 并入库
        if (!voList.isEmpty()) {
            List<Curriculum> entityList = voList.stream()
                    .map(vo -> voToEntity(vo, studentId, year, semester))
                    .collect(Collectors.toList());

            curriculumMapper.insertBatch(entityList);
            log.info("已将 {} 条课表数据存入数据库", entityList.size());
        }

        return voList;
    }

    // --- 辅助转换方法 ---

    private ClassScheduleVO entityToVo(Curriculum entity) {
        ClassScheduleVO vo = new ClassScheduleVO();
        BeanUtils.copyProperties(entity, vo);
        // 注意：BeanUtils拷贝属性名必须一致。
        // Entity: weekRange, dayOfWeek... -> VO: weekRange, dayOfWeek...
        // 我们的字段名在 Entity 和 VO 中是一致的，可以直接用 copyProperties
        return vo;
    }

    private Curriculum voToEntity(ClassScheduleVO vo, String studentId, String year, String semester) {
        Curriculum entity = new Curriculum();
        BeanUtils.copyProperties(vo, entity);
        // 补全数据库特有字段
        entity.setStudentId(studentId);
        entity.setYear(year);
        entity.setSemester(semester);
        return entity;
    }

    // --- 以下保持原有的爬虫逻辑 ---

    private String fetchScheduleJson(CommonQueryDTO dto) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS).build();

        FormBody body = new FormBody.Builder()
                .add("xnm", dto.getXnm() == null ? "" : dto.getXnm())
                .add("xqm", dto.getXqm() == null ? "" : dto.getXqm())
                .add("zs", dto.getZs() == null ? "" : dto.getZs())
                .add("kblx", "1")
                .add("doType", "app")
                .add("xh", dto.getAccount())
                .build();

        // 注意：请确保 SitesURL 枚举或常量类正确配置
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
            // 简单校验是否掉线
            if (res.contains("login") || res.contains("html")) {
                throw new RuntimeException("登录过期，请重新登录");
            }
            return res;
        } catch (IOException e) {
            throw new RuntimeException("课表查询网络异常", e);
        }
    }

    private List<ClassScheduleVO> parseScheduleJson(String json) {
        List<ClassScheduleVO> list = new ArrayList<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

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