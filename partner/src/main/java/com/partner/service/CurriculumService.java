package com.partner.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.partner.dto.CommonQueryDTO;
import com.partner.entity.ScheduleSystem;
import com.partner.entity.ScheduleUser;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CurriculumService {

    @Autowired
    CurriculumMapper curriculumMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 功能1：【导入/同步课程表】
     * 对应前端的 "导入课程表" 按钮。
     * 逻辑：爬取全量数据 -> 解析 -> 覆盖本地 schedule_system 表
     */
    @Transactional(rollbackFor = Exception.class)
    public void importAllSchedules(CommonQueryDTO dto) {
        String studentId = dto.getAccount();
        // 构造学期标识，例如 2024-3
        String semesterKey = dto.getXnm() + "-" + dto.getXqm();

        log.info("开始导入全量课表: {} {}", studentId, semesterKey);

        // 1. 爬虫获取全量 JSON (Request 2 接口)
        String jsonResponse = fetchFullScheduleJson(dto);

        // 2. 解析 JSON 为 System 实体列表
        List<ScheduleSystem> systemList = parseFullSchedule(jsonResponse, studentId, semesterKey);

        // 3. 入库操作
        if (!systemList.isEmpty()) {
            // 先删除旧的
            curriculumMapper.deleteSystemSchedule(studentId, semesterKey);
            // 批量插入新的
            curriculumMapper.insertSystemBatch(systemList);
            log.info("导入成功，共插入 {} 条课程数据", systemList.size());
        }
    }

    /**
     * 功能2：【查询某周课表】
     * 逻辑：查本地数据库 -> 筛选该周课程 -> 合并用户自定义修改
     */
    public List<ClassScheduleVO> getWeeklySchedule(CommonQueryDTO dto) {
        String studentId = dto.getAccount();
        String semesterKey = dto.getXnm() + "-" + dto.getXqm();
        int targetWeek = Integer.parseInt(dto.getZs() == null ? "1" : dto.getZs());

        // 1. 获取系统课表
        List<ScheduleSystem> systemList = curriculumMapper.selectSystemSchedule(studentId, semesterKey);

        // 2. 获取用户自定义操作 (增删改)
        List<ScheduleUser> userOps = curriculumMapper.selectUserSchedule(studentId, semesterKey);

        // 3. 过滤与合并逻辑
        List<ClassScheduleVO> result = new ArrayList<>();

        // A. 处理系统课程
        for (ScheduleSystem sys : systemList) {
            // 3.1 检查该周是否有课
            if (isWeekInList(sys.getWeekList(), targetWeek)) {
                // 3.2 检查是否被用户删除了 (Hidden)
                boolean isDeleted = userOps.stream().anyMatch(op ->
                        "DELETE".equals(op.getOperationType()) &&
                                op.getTargetDay().equals(sys.getDayOfWeek()) &&
                                op.getTargetStartNode().equals(sys.getStartNode())
                );

                if (!isDeleted) {
                    result.add(systemToVo(sys, targetWeek));
                }
            }
        }

        // B. 处理用户新增课程
        for (ScheduleUser op : userOps) {
            if ("ADD".equals(op.getOperationType()) && isWeekInList(op.getCustomWeeks(), targetWeek)) {
                ClassScheduleVO vo = new ClassScheduleVO();
                vo.setCourseName(op.getCustomName());
                vo.setLocation(op.getCustomLocation());
                vo.setDayOfWeek(getDayName(op.getTargetDay())); // 数字转中文
                vo.setDayCode(String.valueOf(op.getTargetDay()));
                vo.setSessionInfo(op.getTargetStartNode() + "-" + (op.getTargetStartNode() + 1)); // 简化处理，默认两节
                vo.setWeekRange("用户添加");
                result.add(vo);
            }
        }

        // 4. 排序 (按星期、节次)
        result.sort(Comparator.comparing(ClassScheduleVO::getDayCode).thenComparing(ClassScheduleVO::getSessionInfo));

        return result;
    }

    /**
     * 功能3：【查询某天课表】(为 AI 预留)
     */
    public List<ClassScheduleVO> getDailySchedule(String studentId, String year, String term, int week, int dayOfWeek) {
        CommonQueryDTO dto = new CommonQueryDTO();
        dto.setAccount(studentId);
        dto.setXnm(year);
        dto.setXqm(term);
        dto.setZs(String.valueOf(week));

        // 复用周课表逻辑，然后过滤那一天
        List<ClassScheduleVO> weekly = getWeeklySchedule(dto);
        return weekly.stream()
                .filter(v -> String.valueOf(dayOfWeek).equals(v.getDayCode()))
                .collect(Collectors.toList());
    }

    // --- 核心辅助方法 ---

    /**
     * 解析 "1-13周,15周(单)" 这种复杂字符串
     */
    private List<Integer> parseZcd(String zcdStr) {
        List<Integer> weeks = new ArrayList<>();
        if (zcdStr == null || zcdStr.isEmpty()) return weeks;

        // 去掉 "周" 字
        String cleanStr = zcdStr.replace("周", "");
        // 按逗号分割不同段落
        String[] parts = cleanStr.split(",");

        for (String part : parts) {
            int step = 1;
            if (part.contains("(单)")) {
                step = 2;
                part = part.replace("(单)", "");
            } else if (part.contains("(双)")) {
                step = 2;
                part = part.replace("(双)", "");
                // 注意：这里逻辑简化，实际上双周起点应为偶数，依靠 range 处理
            }

            if (part.contains("-")) {
                String[] range = part.split("-");
                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);
                for (int i = start; i <= end; i += step) {
                    // 如果是双周模式，且起始是奇数，可能需要逻辑调整，但通常教务数据是规范的
                    // 如 "2-10(双)" -> 2,4,6...
                    // 如 "1-10(双)" -> 这种情况教务极少出现，暂按步长处理
                    weeks.add(i);
                }
            } else {
                weeks.add(Integer.parseInt(part));
            }
        }
        return weeks;
    }

    private boolean isWeekInList(String jsonList, int targetWeek) {
        try {
            List<Integer> list = objectMapper.readValue(jsonList, new TypeReference<List<Integer>>() {});
            return list.contains(targetWeek);
        } catch (Exception e) {
            return false;
        }
    }

    // --- 爬虫与转换逻辑 ---

    private String fetchFullScheduleJson(CommonQueryDTO dto) {
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).build();
        // 注意：Request 2 的参数
        FormBody body = new FormBody.Builder()
                .add("xnm", dto.getXnm())
                .add("xqm", dto.getXqm())
                .add("zs", "") // 查全量不需要传周数，或者传空
                .add("kblx", "1")
                .add("doType", "app") // 关键参数
                .add("xh", dto.getAccount())
                .build();

        // URL 对应 Request 2
        String url = SitesURL.JWGL_BASE_URL.getUrl() + "/jwglxt/kbcx/xskbcxMobile_cxXsKb.html?gnmkdm=N2154";

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Cookie", "JSESSIONID=" + dto.getJsessionid() + "; route=" + dto.getRoute())
                .addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        } catch (IOException e) {
            throw new RuntimeException("爬取课表失败", e);
        }
    }

    private List<ScheduleSystem> parseFullSchedule(String json, String studentId, String semesterKey) {
        List<ScheduleSystem> list = new ArrayList<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        if (!root.has("kbList") || root.get("kbList").isJsonNull()) return list;

        JsonArray kbList = root.getAsJsonArray("kbList");
        for (JsonElement e : kbList) {
            JsonObject kb = e.getAsJsonObject();
            try {
                String zcd = getStr(kb, "zcd"); // "1-13周"
                String jcs = getStr(kb, "jcs"); // "7-8"

                // 解析周次
                List<Integer> weeks = parseZcd(zcd);
                // 解析节次
                String[] nodes = jcs.split("-");
                int startNode = Integer.parseInt(nodes[0]);
                int endNode = nodes.length > 1 ? Integer.parseInt(nodes[1]) : startNode;

                ScheduleSystem schedule = ScheduleSystem.builder()
                        .studentId(studentId)
                        .semester(semesterKey)
                        .courseName(getStr(kb, "kcmc"))
                        .teacher(getStr(kb, "xm"))
                        .location(getStr(kb, "cdmc"))
                        .dayOfWeek(Integer.parseInt(getStr(kb, "xqj")))
                        .startNode(startNode)
                        .endNode(endNode)
                        .weekList(objectMapper.writeValueAsString(weeks)) // 存为 JSON
                        .rawZcd(zcd)
                        .build();

                list.add(schedule);
            } catch (Exception ex) {
                log.error("解析单条课程失败: {}", ex.getMessage());
            }
        }
        return list;
    }

    private ClassScheduleVO systemToVo(ScheduleSystem sys, int currentWeek) {
        ClassScheduleVO vo = new ClassScheduleVO();
        vo.setCourseName(sys.getCourseName());
        vo.setTeacher(sys.getTeacher());
        vo.setLocation(sys.getLocation());
        vo.setDayOfWeek(getDayName(sys.getDayOfWeek()));
        vo.setDayCode(String.valueOf(sys.getDayOfWeek()));
        vo.setSessionInfo(sys.getStartNode() + "-" + sys.getEndNode());
        vo.setWeekRange(sys.getRawZcd());
        // 可以在这里加一个字段 isCurrentWeek: true
        return vo;
    }

    private String getStr(JsonObject o, String k) {
        return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsString() : "";
    }

    private String getDayName(int day) {
        String[] days = {"", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
        return (day >= 1 && day <= 7) ? days[day] : "";
    }
}