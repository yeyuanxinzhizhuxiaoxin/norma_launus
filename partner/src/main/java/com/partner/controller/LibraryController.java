package com.partner.controller;

import com.partner.entity.Result;
import com.partner.entity.library.LibraryProfile;
import com.partner.entity.library.LibraryTimeConfig;
import com.partner.mapper.LibraryMapper;
import com.partner.utils.LibrarySeatUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client/library")
public class LibraryController {

    @Autowired
    private LibraryMapper libraryMapper;

    @Autowired
    private LibrarySeatUtil seatUtil;

    // 保存配置 (对应前端点击保存)
    @PostMapping("/saveConfig")
    public Result saveConfig(@RequestBody LibraryProfile profile) {
        // 解析座位号
        Integer seatId = seatUtil.parseSeatLabel(profile.getSeatLabel());
        if (seatId == null) {
            return Result.error("无效的座位号，请检查格式 (如 03EN11F)");
        }
        profile.setSeatId(seatId);

        libraryMapper.saveProfile(profile);
        return Result.success("配置已保存");
    }

    // 添加预约时间段
    @PostMapping("/addTime")
    public Result addTime(@RequestBody LibraryTimeConfig config) {
        libraryMapper.addTimeConfig(config);
        return Result.success("时间段添加成功");
    }

    // 获取我的配置
    @GetMapping("/myConfig")
    public Result getMyConfig(@RequestParam String studentId) {
        return Result.success(libraryMapper.findProfile(studentId));
    }
}