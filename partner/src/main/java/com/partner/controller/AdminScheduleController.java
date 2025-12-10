package com.partner.controller;

import com.partner.entity.Result;
import com.partner.entity.ScheduleSystem;
import com.partner.mapper.CurriculumMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminScheduleController {

    @Autowired
    private CurriculumMapper curriculumMapper;

    /**
     * 分页查询课表列表
     * GET /admin/schedules
     * 参数：page, size, studentId, year, semester, week
     */
    @GetMapping("/schedules")
    public Result getSchedules(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(required = false) String studentId,
                               @RequestParam(required = false) String year,
                               @RequestParam(required = false) String semester,
                               @RequestParam(required = false) String week) {

        int offset = (page - 1) * size;

        // 1. 查询数据列表
        List<ScheduleSystem> list = curriculumMapper.selectSchedulesByPage(
                studentId, year, semester, week, offset, size
        );

        // 2. 查询总数
        long total = curriculumMapper.countSchedules(
                studentId, year, semester, week
        );

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);

        return Result.success(data);
    }

    /**
     * 删除单条课程记录
     * DELETE /admin/schedule/{id}
     */
    @DeleteMapping("/schedule/{id}")
    public Result deleteSchedule(@PathVariable Long id) {
        if (id == null) {
            return Result.error("ID不能为空");
        }
        curriculumMapper.deleteScheduleById(id);
        log.info("管理员删除了课程记录 ID: {}", id);
        return Result.success("删除成功");
    }
}