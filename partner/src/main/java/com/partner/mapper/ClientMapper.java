package com.partner.mapper;

import com.partner.entity.Client;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ClientMapper {

    @Insert("INSERT INTO client(account, password) VALUES(#{account}, #{password})")
    void insertClient(Client client);


    @Select("SELECT * FROM client WHERE account = #{account}")
    Client findClientByAccount(String account);
}
