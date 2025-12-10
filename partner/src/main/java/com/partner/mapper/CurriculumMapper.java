package com.partner.mapper;

import com.partner.entity.ScheduleSystem;
import com.partner.entity.ScheduleUser;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CurriculumMapper {

    @Delete("DELETE FROM schedule_system WHERE student_id = #{studentId} AND semester = #{semester}")
    void deleteSystemSchedule(@Param("studentId") String studentId, @Param("semester") String semester);

    // 【移入 XML】批量插入
    void insertSystemBatch(@Param("list") List<ScheduleSystem> list);

    @Select("SELECT * FROM schedule_system WHERE student_id = #{studentId} AND semester = #{semester}")
    List<ScheduleSystem> selectSystemSchedule(@Param("studentId") String studentId, @Param("semester") String semester);

    @Select("SELECT * FROM schedule_user WHERE student_id = #{studentId} AND semester = #{semester} AND is_active = 1")
    List<ScheduleUser> selectUserSchedule(@Param("studentId") String studentId, @Param("semester") String semester);

    @Insert("INSERT INTO schedule_user (student_id, semester, operation_type, custom_name, custom_location, custom_weeks, target_day, target_start_node) " +
            "VALUES (#{studentId}, #{semester}, #{operationType}, #{customName}, #{customLocation}, #{customWeeks}, #{targetDay}, #{targetStartNode})")
    void insertUserSchedule(ScheduleUser scheduleUser);

    // 【移入 XML】管理端分页查询
    List<ScheduleSystem> selectSchedulesByPage(@Param("studentId") String studentId,
                                               @Param("year") String year,
                                               @Param("semester") String semester,
                                               @Param("week") String week,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    // 【移入 XML】管理端统计
    long countSchedules(@Param("studentId") String studentId,
                        @Param("year") String year,
                        @Param("semester") String semester,
                        @Param("week") String week);

    @Delete("DELETE FROM schedule_system WHERE id = #{id}")
    void deleteScheduleById(Long id);
}