package com.partner.controller;

import com.partner.dto.CommonQueryDTO;
import com.partner.dto.LoginDTO;
import com.partner.entity.Result;
import com.partner.service.AuthService;
import com.partner.service.CurriculumService;
import com.partner.service.ScoreService;
import com.partner.vo.ClassScheduleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/client")
public class ClientController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private CurriculumService curriculumService;


    /**
     * 客户登录
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO loginDTO) {
        try {
            log.info("用户登录: {}", loginDTO.getAccount());
            return Result.success(authService.login(loginDTO));
        } catch (Exception e) {
            log.error("登录失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 客户查询成绩
     */
    @PostMapping("/queryScore")
    public Result queryScore(@RequestBody CommonQueryDTO queryDTO) {
        try {
            log.info("查询成绩: {}", queryDTO.getAccount());
            return Result.success(scoreService.syncScores(queryDTO));
        } catch (Exception e) {
            log.error("成绩查询失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 【新】导入/同步全量课表
     * 场景：用户本学期首次使用，或点击“同步最新课表”按钮
     */
    @PostMapping("/importSchedule")
    public Result importSchedule(@RequestBody CommonQueryDTO queryDTO) {
        try {
            log.info("导入课表: {}", queryDTO.getAccount());
            curriculumService.importAllSchedules(queryDTO);
            return Result.success("课表导入成功");
        } catch (Exception e) {
            log.error("课表导入失败", e);
            return Result.error("导入失败: " + e.getMessage());
        }
    }

    /**
     * 【改】查询周课表
     * 场景：日常打开课表，切换周次。此接口现在【只查数据库】，速度极快。
     */
    @PostMapping("/queryClassSchedule")
    public Result queryClassSchedule(@RequestBody CommonQueryDTO queryDTO) {
        try {
            log.info("查询本地课表: {} 第{}周", queryDTO.getAccount(), queryDTO.getZs());
            List<ClassScheduleVO> list = curriculumService.getWeeklySchedule(queryDTO);

            // 如果查询结果为空，且是第一周，可能是还没导入过
            if (list.isEmpty() && "1".equals(queryDTO.getZs())) {
                // 可选：返回一个特定状态码，告诉前端“请先导入”
                // return Result.error("NO_DATA_PLEASE_IMPORT");
            }

            return Result.success(list);
        } catch (Exception e) {
            log.error("课表查询失败", e);
            return Result.error(e.getMessage());
        }
    }

    // 【新】添加自定义课程（前端暂未开发，但后端先留好接口）
    // @PostMapping("/addCustomCourse") ...
}