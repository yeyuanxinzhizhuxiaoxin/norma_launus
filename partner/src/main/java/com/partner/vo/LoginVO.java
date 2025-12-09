package com.partner.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginVO {
    private String jsessionid;
    private String route;
}