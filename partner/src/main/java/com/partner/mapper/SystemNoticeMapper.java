package com.partner.mapper;

import com.partner.entity.SystemNotice;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SystemNoticeMapper {

    // 管理员：发布公告
    @Insert("INSERT INTO system_notice (title, content, publisher_id, create_time, update_time) " +
            "VALUES (#{title}, #{content}, #{publisherId}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertNotice(SystemNotice notice);

    // 管理员：删除公告
    @Delete("DELETE FROM system_notice WHERE id = #{id}")
    void deleteNoticeById(Long id);

    // 全体用户：查询所有公告 (关联查询发布者姓名)
    @Select("SELECT n.*, c.name as publisherName " +
            "FROM system_notice n " +
            "LEFT JOIN client c ON n.publisher_id = c.id " +
            "ORDER BY n.create_time DESC")
    List<SystemNotice> findAllNotices();
}