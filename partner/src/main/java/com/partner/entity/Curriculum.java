package com.partner.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Curriculum {
    private Long id;
    private String studentId;
    private String year;        // 学年
    private String semester;    // 学期

    private String courseName;
    private String teacher;
    private String location;
    private String weekRange;
    private String dayOfWeek;
    private String dayCode;
    private String sessionInfo;
    private String credit;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}