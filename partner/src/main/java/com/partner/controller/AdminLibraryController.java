package com.partner.controller;

import com.partner.entity.Result;
import com.partner.entity.library.LibraryProfile;
import com.partner.entity.library.LibraryTimeConfig;
import com.partner.mapper.LibraryMapper;
import com.partner.service.LibraryBookingService;
import com.partner.utils.LibraryHttpUtil;
import com.partner.utils.LibrarySeatUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/library")
public class AdminLibraryController {

    @Autowired
    private LibraryMapper libraryMapper;

    @Autowired
    private LibraryBookingService bookingService;

    @Autowired
    private LibrarySeatUtil seatUtil;

    // ================= 用户管理 (Profile) =================

    /**
     * 获取所有用户列表
     */
    @GetMapping("/users")
    public Result getAllUsers() {
        List<LibraryProfile> users = libraryMapper.findAllProfiles();
        return Result.success(users);
    }

    /**
     * 添加或更新用户
     */
    @PostMapping("/user")
    public Result saveUser(@RequestBody LibraryProfile profile) {
        if (profile.getStudentId() == null || profile.getPassword() == null) {
            return Result.error("学号和密码不能为空");
        }

        // 解析座位号 (如果传了 seatLabel 但没传 seatId)
        if (profile.getSeatLabel() != null && !profile.getSeatLabel().isEmpty()) {
            Integer seatId = seatUtil.parseSeatLabel(profile.getSeatLabel());
            if (seatId != null) {
                profile.setSeatId(seatId);
            } else {
                return Result.error("座位号解析失败: " + profile.getSeatLabel());
            }
        }

        libraryMapper.saveProfile(profile);
        return Result.success("用户保存成功");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/user/{studentId}")
    public Result deleteUser(@PathVariable String studentId) {
        libraryMapper.deleteProfile(studentId);
        return Result.success("用户删除成功");
    }

    // ================= 时间配置管理 (TimeConfig) =================

    /**
     * 获取某用户的所有时间配置
     */
    @GetMapping("/user/{studentId}/times")
    public Result getUserTimeConfigs(@PathVariable String studentId) {
        List<LibraryTimeConfig> configs = libraryMapper.findTimeConfigsByStudentId(studentId);
        return Result.success(configs);
    }

    /**
     * 添加时间配置
     */
    @PostMapping("/time")
    public Result addTimeConfig(@RequestBody LibraryTimeConfig config) {
        if (config.getStudentId() == null) {
            return Result.error("学号不能为空");
        }
        // 默认启用
        if (config.getIsActive() == null) config.setIsActive(true);

        libraryMapper.addTimeConfig(config);
        return Result.success("时间配置添加成功");
    }

    /**
     * 更新时间配置 (启用/禁用/修改时间)
     */
    @PutMapping("/time")
    public Result updateTimeConfig(@RequestBody LibraryTimeConfig config) {
        if (config.getId() == null) {
            return Result.error("配置ID不能为空");
        }
        libraryMapper.updateTimeConfig(config);
        return Result.success("时间配置更新成功");
    }

    /**
     * 删除时间配置
     */
    @DeleteMapping("/time/{id}")
    public Result deleteTimeConfig(@PathVariable Long id) {
        libraryMapper.deleteTimeConfig(id);
        return Result.success("时间配置删除成功");
    }

    // ================= 测试功能 =================

    /**
     * 测试预约请求
     * 手动触发一次预约，用于验证账号密码、座位号或系统连通性
     */
    @PostMapping("/test-booking")
    public Result testBooking(@RequestBody TestBookingDTO dto) {
        if (dto.getStudentId() == null || dto.getStartTime() == null || dto.getEndTime() == null) {
            return Result.error("参数不完整 (需 studentId, startTime, endTime)");
        }

        LibraryHttpUtil.BookingResult result = bookingService.testBooking(
                dto.getStudentId(),
                dto.getSeatId(), // 可选，不传则用默认
                dto.getStartTime(),
                dto.getEndTime()
        );

        if (result.isSuccess()) {
            return Result.success(result.getMessage());
        } else {
            // 返回包含原始响应的错误信息，方便调试
            return Result.error("预约失败: " + result.getMessage() + " | 原文: " + result.getRawResponse());
        }
    }

    @Data
    public static class TestBookingDTO {
        private String studentId;
        private Integer seatId;   // 可选，临时测试其他座位
        private String startTime; // "HH:mm"
        private String endTime;   // "HH:mm"
    }
}