package com.partner.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleSystem {
    private Long id;
    private String studentId;
    private String semester;    // 学期 (如 2024-2025-1)
    private String courseName;
    private String teacher;
    private String location;
    private Integer dayOfWeek;  // 1-7
    private Integer startNode;  // 开始节次
    private Integer endNode;    // 结束节次
    private String weekList;    // 存 JSON 字符串: "[1,2,3,4,5]"
    private String rawZcd;      // 原始周次描述 (备份用)
}