package com.partner.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter

public enum SitesURL {
    //统一身份认证（CAS）服务器地址
    CAS_BASE_URL("https://mapp.haut.edu.cn"),
    //目标业务系统（信息门户）的回调地址
    SERVICE_URL("https://portal.haut.edu.cn/portal-pc/login/pcLogin"),
    //教务管理系统地址
    JWGL_BASE_URL("https://jwglxt.haut.edu.cn"),    //教务管理系统地址
    //教务系统登录
    JWGL_LOGIN_URL("https://jwglxt.haut.edu.cn/sso/jhlogin"),
    //成绩查询接口（正方教务系统典型路径）
    SCORE_QUERY_URL(SitesURL.JWGL_BASE_URL.getUrl()+"/jwglxt/cjcx/cjcx_cxXsgrcj.html?doType=query&gnmkdm=N305005");
    //课表查询接口

    private final String url;

    SitesURL(String url) {
        this.url = url; // ← 关键：把参数赋值给字段！
    }
}
