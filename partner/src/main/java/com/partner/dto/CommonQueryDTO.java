package com.partner.dto;

import lombok.Data;

@Data
public class CommonQueryDTO {
    // 业务参数
    private String xnm;        // 学年 (如 "2024")
    private String xqm;        // 学期 (3:秋季, 12:春季)
    private String zs;         // 周数 (课表查询用)
    private String account;    // 学号

    // 认证参数
    private String jsessionid; // 登录凭证
    private String route;      // 路由凭证
}