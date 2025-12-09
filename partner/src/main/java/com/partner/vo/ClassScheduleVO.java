package com.partner.vo;

import lombok.Data;

@Data
public class ClassScheduleVO {
    private String courseName;   // 课程名 (kcmc)
    private String teacher;      // 教师 (xm)
    private String location;     // 教室 (cdmc)
    private String weekRange;    // 周次 (zcd)
    private String dayOfWeek;    // 星期几 (xqjmc)
    private String dayCode;      // 星期代码 (xqj)
    private String sessionInfo;  // 节次 (jc)
    private String credit;       // 学分 (xf)
}