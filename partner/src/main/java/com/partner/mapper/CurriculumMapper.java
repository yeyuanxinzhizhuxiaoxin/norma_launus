package com.partner.mapper;

import com.partner.entity.ScheduleSystem;
import com.partner.entity.ScheduleUser;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CurriculumMapper {

    // --- System 表操作 ---

    /**
     * 清空某学生某学期的系统课表 (重新导入前调用)
     */
    @Delete("DELETE FROM schedule_system WHERE student_id = #{studentId} AND semester = #{semester}")
    void deleteSystemSchedule(@Param("studentId") String studentId, @Param("semester") String semester);

    /**
     * 批量插入系统课表
     */
    @Insert("<script>" +
            "INSERT INTO schedule_system (student_id, semester, course_name, teacher, location, day_of_week, start_node, end_node, week_list, raw_zcd) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.studentId}, #{item.semester}, #{item.courseName}, #{item.teacher}, #{item.location}, #{item.dayOfWeek}, #{item.startNode}, #{item.endNode}, #{item.weekList}, #{item.rawZcd})" +
            "</foreach>" +
            "</script>")
    void insertSystemBatch(@Param("list") List<ScheduleSystem> list);

    /**
     * 查询某学期所有系统课表 (用于后续内存中过滤周次)
     */
    @Select("SELECT * FROM schedule_system WHERE student_id = #{studentId} AND semester = #{semester}")
    List<ScheduleSystem> selectSystemSchedule(@Param("studentId") String studentId, @Param("semester") String semester);


    // --- User 表操作 (手动修改支持) ---

    @Select("SELECT * FROM schedule_user WHERE student_id = #{studentId} AND semester = #{semester} AND is_active = 1")
    List<ScheduleUser> selectUserSchedule(@Param("studentId") String studentId, @Param("semester") String semester);

    // 添加用户自定义课程
    @Insert("INSERT INTO schedule_user (student_id, semester, operation_type, custom_name, custom_location, custom_weeks, target_day, target_start_node) " +
            "VALUES (#{studentId}, #{semester}, #{operationType}, #{customName}, #{customLocation}, #{customWeeks}, #{targetDay}, #{targetStartNode})")
    void insertUserSchedule(ScheduleUser scheduleUser);
}