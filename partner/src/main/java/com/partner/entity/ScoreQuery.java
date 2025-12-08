package com.partner.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreQuery {
    private String xnmmc;
    private String xqmmc;
    private String account;
    private String JSESSIONID;
    private String route;
}
