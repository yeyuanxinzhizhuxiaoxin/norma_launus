package com.partner.controller;

import com.partner.dto.CommonQueryDTO;
import com.partner.dto.LoginDTO;
import com.partner.entity.Result;
import com.partner.service.AuthService;
import com.partner.service.CurriculumService;
import com.partner.service.ScoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
     * 客户查询课表
     */
    @PostMapping("/queryClassSchedule")
    public Result queryClassSchedule(@RequestBody CommonQueryDTO queryDTO) {
        try {
            log.info("查询课表: {}", queryDTO.getAccount());
            return Result.success(curriculumService.getWeeklySchedule(queryDTO));
        } catch (Exception e) {
            log.error("课表查询失败", e);
            return Result.error(e.getMessage());
        }
    }
}