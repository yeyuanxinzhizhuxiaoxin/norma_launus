package com.partner.entity;

import lombok.Data;

@Data
public class ScheduleUser {
    private Long id;
    private String studentId;
    private String semester;
    /**
     * 操作类型: ADD(新增), DELETE(删除/隐藏), MODIFY(修改)
     */
    private String operationType;

    //如果是修改/删除，需要锁定原课程的时间
    private Integer targetDay;
    private Integer targetStartNode;

    //如果是新增/修改，需要新的信息
    private String customName;
    private String customLocation;
    private String customWeeks; // JSON: "[1,2,3]"
    private Boolean isActive;
}