package com.partner.mapper;

import com.partner.entity.library.LibraryProfile;
import com.partner.entity.library.LibraryTimeConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LibraryMapper {

    @Select("SELECT * FROM library_profile WHERE student_id = #{studentId}")
    LibraryProfile findProfile(String studentId);

    @Select("SELECT * FROM library_profile")
    List<LibraryProfile> findAllProfiles();

    // 【移入 XML】保存用户配置
    void saveProfile(LibraryProfile profile);

    @Delete("DELETE FROM library_profile WHERE student_id = #{studentId}")
    void deleteProfile(String studentId);

    @Select("SELECT * FROM library_time_config WHERE student_id = #{studentId}")
    List<LibraryTimeConfig> findTimeConfigsByStudentId(String studentId);

    @Select("SELECT * FROM library_time_config WHERE student_id = #{studentId} AND is_active = 1")
    List<LibraryTimeConfig> findActiveTimeConfigs(String studentId);

    @Insert("INSERT INTO library_time_config (student_id, start_time, end_time, auto_start_time, is_active) " +
            "VALUES (#{studentId}, #{startTime}, #{endTime}, #{autoStartTime}, #{isActive})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void addTimeConfig(LibraryTimeConfig config);

    @Update("UPDATE library_time_config SET start_time=#{startTime}, end_time=#{endTime}, " +
            "auto_start_time=#{autoStartTime}, is_active=#{isActive} WHERE id=#{id}")
    void updateTimeConfig(LibraryTimeConfig config);

    @Delete("DELETE FROM library_time_config WHERE id = #{id}")
    void deleteTimeConfig(Long id);

    @Select("SELECT p.* FROM library_profile p WHERE p.auto_enable = 1")
    List<LibraryProfile> findAutoEnabledUsers();
}