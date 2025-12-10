package com.partner.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 座位解析工具类
 * 纯爬虫模式：直接调用图书馆接口查询座位 ID
 */
@Slf4j
@Component
public class LibrarySeatUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 复用 OkHttpClient，设置较短的超时时间，因为解析需要在用户交互时完成
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build();

    // 楼层-区域 到 regionId 的映射
    private static final Map<String, Integer> REGION_ID_MAP = new HashMap<>();
    static {
        // 3F
        REGION_ID_MAP.put("03-EN", 7); REGION_ID_MAP.put("03-ES", 8); REGION_ID_MAP.put("03-WN", 9); REGION_ID_MAP.put("03-WS", 10);
        // 4F
        REGION_ID_MAP.put("04-EN", 11); REGION_ID_MAP.put("04-ES", 12); REGION_ID_MAP.put("04-WN", 13); REGION_ID_MAP.put("04-WS", 14);
        // 5F
        REGION_ID_MAP.put("05-EN", 15); REGION_ID_MAP.put("05-ES", 16); REGION_ID_MAP.put("05-MM", 17); REGION_ID_MAP.put("05-WN", 18); REGION_ID_MAP.put("05-WS", 19);
        // 6F
        REGION_ID_MAP.put("06-EN", 20); REGION_ID_MAP.put("06-ES", 21); REGION_ID_MAP.put("06-WN", 22); REGION_ID_MAP.put("06-WS", 23);
        // 7F
        REGION_ID_MAP.put("07-EN", 24); REGION_ID_MAP.put("07-ES", 25); REGION_ID_MAP.put("07-WN", 26); REGION_ID_MAP.put("07-WS", 27);
        // 8F
        REGION_ID_MAP.put("08-EN", 28); REGION_ID_MAP.put("08-ES", 29); REGION_ID_MAP.put("08-MM", 30); REGION_ID_MAP.put("08-WN", 31); REGION_ID_MAP.put("08-WS", 32);
        // 9F
        REGION_ID_MAP.put("09-EN", 33); REGION_ID_MAP.put("09-ES", 34); REGION_ID_MAP.put("09-WN", 35); REGION_ID_MAP.put("09-WS", 36); REGION_ID_MAP.put("09-MM", 37);
        // 10F
        REGION_ID_MAP.put("10-EN", 38); REGION_ID_MAP.put("10-ES", 39); REGION_ID_MAP.put("10-WN", 40); REGION_ID_MAP.put("10-WS", 4);
    }

    /**
     * 解析座位标签
     * @param label 输入如 "03EN11F", "03-EN-11-F"
     * @return 对应的 seatId，如果解析失败返回 null
     */
    public Integer parseSeatLabel(String label) {
        if (label == null || label.trim().isEmpty()) {
            return null;
        }

        // 标准化输入：去除横杠，转大写
        String normLabel = label.trim().toUpperCase(Locale.ROOT).replace("-", "");

        // 1. 预校验格式 (例如 03EN11F)
        if (!normLabel.matches("^[0-9]{2}(EN|ES|WN|WS|MM)[0-9]{2}[A-Z]$")) {
            log.warn("座位标签格式错误: {}", label);
            return null;
        }

        // 2. 提取参数
        String floor = normLabel.substring(0, 2);
        String dir = normLabel.substring(2, 4);
        int table = Integer.parseInt(normLabel.substring(4, 6));
        char column = normLabel.charAt(6);

        // 3. 获取区域 ID
        Integer regionId = REGION_ID_MAP.get(floor + "-" + dir);
        if (regionId == null) {
            log.warn("未知的楼层区域: {}-{}", floor, dir);
            return null;
        }

        // 4. 调用 API 查找 seatId
        return fetchSeatIdFromApi(regionId, floor, dir, table, column);
    }

    private Integer fetchSeatIdFromApi(int regionId, String floor, String dir, int table, char column) {
        try {
            // 构造查询时间范围 (当天到现在 ~ 当晚22点)
            String startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String endTime = LocalDate.now().atTime(22, 0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            String url = buildUrl(regionId, startTime, endTime);

            Request request = new Request.Builder()
                    .url(url)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0 Safari/537.36")
                    .header("Referer", "https://wslib.haut.edu.cn/")
                    .build();

            // 简单的重试机制 (最多2次)
            for (int i = 0; i < 2; i++) {
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        return parseSeatIdFromJson(body, floor, dir, table, column);
                    }
                } catch (IOException e) {
                    log.warn("座位查询API请求失败 (第{}次): {}", i + 1, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("解析座位ID异常", e);
        }
        return null;
    }

    private String buildUrl(int regionId, String startTime, String endTime) {
        // OkHttp 会自动处理参数编码，但这里我们是拼接 URL 字符串
        try {
            String st = URLEncoder.encode(startTime, StandardCharsets.UTF_8).replace("+", "%20");
            String et = URLEncoder.encode(endTime, StandardCharsets.UTF_8).replace("+", "%20");

            return "https://wslib.haut.edu.cn/stage-api/api/seatbook/layout/query"
                    + "?pageNum=1&pageSize=500" // 稍微放大 pageSize 确保能查到
                    + "&regionid=" + regionId
                    + "&starttime=" + st
                    + "&endtime=" + et;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Integer parseSeatIdFromJson(String json, String floor, String dir, int table, char column) throws IOException {
        JsonNode root = MAPPER.readTree(json);
        if (!root.has("seatList") || !root.get("seatList").isArray()) {
            return null;
        }

        String regionNameZh = buildRegionZh(floor, dir); // 如 "三层东书库北区"
        String tableStr = String.format("%02d", table);  // 如 "01"

        // 目标后缀1: " 01桌 A座"
        String suffixDesk = " " + tableStr + "桌 " + column + "座";
        // 目标后缀2: " 01座 A座" (中区常见)
        String suffixSeat = " " + tableStr + "座 " + column + "座";

        for (JsonNode node : root.get("seatList")) {
            String seatName = node.path("seatName").asText("");
            int id = node.path("id").asInt(-1);

            if (id > 0 && seatName.contains(regionNameZh)) {
                if (seatName.endsWith(suffixDesk) || seatName.endsWith(suffixSeat)) {
                    log.info("座位解析成功: {} -> id={}", seatName, id);
                    return id;
                }
            }
        }
        log.warn("API返回中未找到匹配座位: {}{}", regionNameZh, suffixDesk);
        return null;
    }

    private String buildRegionZh(String floor, String dir) {
        String floorZh = toChineseFloor(floor);
        switch (dir) {
            case "EN": return floorZh + "东书库北区";
            case "ES": return floorZh + "东书库南区";
            case "WN": return floorZh + "西书库北区";
            case "WS": return floorZh + "西书库南区";
            case "MM": return floorZh + "中区";
            default: return floorZh;
        }
    }

    private String toChineseFloor(String floor) {
        switch (floor) {
            case "03": return "三层";
            case "04": return "四层";
            case "05": return "五层";
            case "06": return "六层";
            case "07": return "七层";
            case "08": return "八层";
            case "09": return "九层";
            case "10": return "十层";
            default: return floor + "层";
        }
    }
}