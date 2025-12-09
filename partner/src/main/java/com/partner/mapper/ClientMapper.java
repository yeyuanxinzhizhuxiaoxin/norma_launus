package com.partner.mapper;

import com.partner.entity.Client;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ClientMapper {

    // 修改查询：映射新增的字段
    @Select("SELECT id, account, password, name, role FROM client WHERE account = #{account}")
    Client findClientByAccount(@Param("account") String account);

    // 修改插入：包含 name 和 role (默认为0)
    @Insert("INSERT INTO client (account, password, name, role) VALUES (#{account}, #{password}, #{name}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertClient(Client client);

    // 新增：根据ID查询（用于公告显示发布人）
    @Select("SELECT id, account, name, role FROM client WHERE id = #{id}")
    Client findClientById(@Param("id") Long id);
}