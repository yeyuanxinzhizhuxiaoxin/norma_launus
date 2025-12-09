package com.partner.service;

import com.partner.entity.library.LibraryProfile;
import com.partner.entity.library.LibraryTimeConfig;
import com.partner.mapper.LibraryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class LibrarySchedulerService {

    @Autowired
    private LibraryMapper libraryMapper;

    @Autowired
    private LibraryBookingService bookingService;

    // 每秒扫描一次
    @Scheduled(cron = "* * * * * ?")
    public void scanAndBook() {
        LocalTime now = LocalTime.now();

        // 1. 获取所有开启了自动预约的用户
        List<LibraryProfile> activeUsers = libraryMapper.findAutoEnabledUsers();

        for (LibraryProfile user : activeUsers) {
            // 2. 获取该用户的时间配置
            List<LibraryTimeConfig> configs = libraryMapper.findActiveTimeConfigs(user.getStudentId());

            for (LibraryTimeConfig config : configs) {
                // 3. 检查触发时间 (autoStartTime - 5秒)
                if (shouldTrigger(now, config.getAutoStartTime())) {
                    log.info("触发预约: 用户={}, 目标时间={}", user.getStudentId(), config.getAutoStartTime());
                    // 异步执行
                    bookingService.executeBookingTask(user, config);
                }
            }
        }
    }

    private boolean shouldTrigger(LocalTime now, String autoStartTimeStr) {
        try {
            LocalTime target = LocalTime.parse(autoStartTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime triggerTime = target.minusSeconds(5); // 提前5秒触发

            return now.getHour() == triggerTime.getHour() &&
                    now.getMinute() == triggerTime.getMinute() &&
                    now.getSecond() == triggerTime.getSecond();
        } catch (Exception e) {
            return false;
        }
    }
}