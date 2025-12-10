package com.partner.mapper;

import com.partner.entity.Client;
import org.apache.ibatis.annotations.*;

import java.util.List;

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

    // 【新增】根据账号密码查询用户（用于管理员登录）
    @Select("SELECT * FROM client WHERE account = #{account} AND password = #{password}")
    Client loginCheck(@Param("account") String account, @Param("password") String password);

    // ClientMapper.java
    List<Client> selectByPage(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit) ;

    long count(@Param("keyword") String keyword);


    // 【新增】删除用户
    @Delete("DELETE FROM client WHERE id = #{id}")
    void deleteById(Long id);
}