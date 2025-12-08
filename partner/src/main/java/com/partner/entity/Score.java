package com.partner.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.partner.utils.TwoDecimalDoubleSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Score {
    private Long id;
    private String studentId; //学号 xh
    private String year; //学年 xnmmc
    private String semester;//学期 xqmmc
    private String courseName;//课程名称 kcmc

    @JsonSerialize(using = TwoDecimalDoubleSerializer.class)
    private Double credit;//学分 xf

    @JsonSerialize(using = TwoDecimalDoubleSerializer.class)
    private Double point;//绩点 jd

    @JsonSerialize(using = TwoDecimalDoubleSerializer.class)
    private Double grade;//成绩 cj

    @JsonSerialize(using = TwoDecimalDoubleSerializer.class)
    private Double gpa;//学分绩点 xfjd

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
