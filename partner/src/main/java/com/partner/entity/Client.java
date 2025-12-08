package com.partner.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.annotations.Delete;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Client {
    Long id;
    String account;
    String password;
}
