package com.partner.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import okhttp3.OkHttpClient;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginContext {
    private OkHttpClient okHttpClient;
    private String JSESSIONID;
    private String route;
}
