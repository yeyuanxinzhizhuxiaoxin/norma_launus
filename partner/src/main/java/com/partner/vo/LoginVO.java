package com.partner.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginVO {
    private String jsessionid;
    private String route;
    private Integer role; // 返回角色：0-普通用户，1-管理员
    private String name;  // 返回昵称
}