package com.partner.mapper;

import com.partner.entity.Curriculum;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CurriculumMapper {

    /**
     * 根据学号、学年、学期查询课表
     */
    @Select("SELECT * FROM curriculum WHERE student_id = #{studentId} AND year = #{year} AND semester = #{semester}")
    List<Curriculum> findCurriculum(@Param("studentId") String studentId,
                                    @Param("year") String year,
                                    @Param("semester") String semester);

    /**
     * 批量插入课表数据
     * 注意：使用 <script> 标签支持批量插入
     */
    @Insert("<script>" +
            "INSERT INTO curriculum (student_id, year, semester, course_name, teacher, location, week_range, day_of_week, day_code, session_info, credit) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.studentId}, #{item.year}, #{item.semester}, #{item.courseName}, #{item.teacher}, #{item.location}, #{item.weekRange}, #{item.dayOfWeek}, #{item.dayCode}, #{item.sessionInfo}, #{item.credit})" +
            "</foreach>" +
            "</script>")
    void insertBatch(@Param("list") List<Curriculum> list);

    // 可选：如果支持“刷新课表”功能，可能需要先删除旧数据
    // @Delete("DELETE FROM curriculum WHERE student_id = #{studentId} AND year = #{year} AND semester = #{semester}")
    // void deleteCurriculum(...);
}