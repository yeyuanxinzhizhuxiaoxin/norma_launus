package com.partner.mapper;

import com.partner.entity.library.LibraryProfile;
import com.partner.entity.library.LibraryTimeConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LibraryMapper {

    // --- 配置相关 ---
    @Select("SELECT * FROM library_profile WHERE student_id = #{studentId}")
    LibraryProfile findProfile(String studentId);

    @Insert("INSERT INTO library_profile (student_id, password, seat_label, seat_id, auto_enable, send_key) " +
            "VALUES (#{studentId}, #{password}, #{seatLabel}, #{seatId}, #{autoEnable}, #{sendKey}) " +
            "ON DUPLICATE KEY UPDATE password=#{password}, seat_label=#{seatLabel}, seat_id=#{seatId}, " +
            "auto_enable=#{autoEnable}, send_key=#{sendKey}")
    void saveProfile(LibraryProfile profile);

    // --- 时间配置相关 ---
    @Select("SELECT * FROM library_time_config WHERE student_id = #{studentId} AND is_active = 1")
    List<LibraryTimeConfig> findActiveTimeConfigs(String studentId);

    @Select("SELECT * FROM library_time_config")
    List<LibraryTimeConfig> findAllConfigs();

    @Insert("INSERT INTO library_time_config (student_id, start_time, end_time, auto_start_time, is_active) " +
            "VALUES (#{studentId}, #{startTime}, #{endTime}, #{autoStartTime}, 1)")
    void addTimeConfig(LibraryTimeConfig config);

    @Delete("DELETE FROM library_time_config WHERE id = #{id}")
    void deleteTimeConfig(Long id);

    // 查询所有开启了自动预约的用户及其时间配置
    @Select("SELECT p.* FROM library_profile p WHERE p.auto_enable = 1")
    List<LibraryProfile> findAutoEnabledUsers();
}