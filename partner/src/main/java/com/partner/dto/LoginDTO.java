package com.partner.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String account;  // 学号
    private String password; // 密码
}