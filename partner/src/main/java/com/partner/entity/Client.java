package com.partner.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Client {
    private Long id;
    private String account;  // 学号/账号
    private String password; // 密码
    private String name;     // 昵称 (新增)

    /**
     * 角色：0-普通用户, 1-管理员
     */
    private Integer role;    // (新增)
}