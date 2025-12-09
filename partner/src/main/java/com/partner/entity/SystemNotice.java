package com.partner.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SystemNotice {
    private Long id;
    private String title;
    private String content;
    private Long publisherId; // 发布人ID (对应 Client.id)
    private String publisherName; // 发布人姓名 (非数据库字段，用于展示)
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}