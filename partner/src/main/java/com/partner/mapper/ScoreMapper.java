package com.partner.mapper;

import com.partner.entity.Score;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScoreMapper {
    /**
     * 批量插入成绩
     * @param scoreList
     */
    void insertScores(List<Score> scoreList);

    /**
     * 根据课程名称查找成绩记录
     * @param courseName
     */
    @Select("Select * FROM score WHERE course_name = #{courseName}")
    void findScoreByCourseName(String courseName);

    /**
     * 根据学号查询成绩
     * @param studentId
     * @return
     */
    @Select("SELECT course_name FROM score WHERE student_id = #{studentId}")
    List<String> findExistingCourseNamesByStudentId(@Param("studentId") String studentId);


}
