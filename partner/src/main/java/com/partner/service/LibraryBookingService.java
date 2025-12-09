package com.partner.service;

import com.partner.entity.library.LibraryProfile;
import com.partner.entity.library.LibraryTimeConfig;
import com.partner.mapper.LibraryMapper;
import com.partner.utils.LibraryHttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LibraryBookingService {

    @Autowired
    private LibraryMapper libraryMapper;

    @Autowired
    private LibraryHttpUtil httpUtil; // 原 HttpUtil

    /**
     * 执行单次预约任务 (被定时任务调用)
     */
    @Async("taskExecutor") // 确保使用了线程池
    public void executeBookingTask(LibraryProfile profile, LibraryTimeConfig timeConfig) {
        log.info("开始执行预约任务: 学号={}, 时间={}", profile.getStudentId(), timeConfig.getStartTime());

        try {
            // 1. 预登录 (获取 Token)
            String uuid = httpUtil.getCaptchaUuid();
            LibraryHttpUtil.LoginResult login = httpUtil.login(profile.getStudentId(), profile.getPassword(), uuid);

            // 2. 等待直到 autoStartTime
            // (原 BookingService 中的 waitUntil 逻辑)

            // 3. 循环重试预约
            boolean success = false;
            int maxRetries = 20;

            String date = LocalDate.now().toString();
            String startDateTime = date + " " + timeConfig.getStartTime() + ":00";
            String endDateTime = date + " " + timeConfig.getEndTime() + ":00";

            for (int i = 0; i < maxRetries; i++) {
                try {
                    LibraryHttpUtil.BookingResult result = httpUtil.bookSeat(
                            profile.getSeatId(),
                            startDateTime,
                            endDateTime,
                            login.getToken(),
                            login.getTicket()
                    );

                    if (result.isSuccess()) {
                        success = true;
                        log.info("预约成功! 学号={}", profile.getStudentId());
                        // TODO: 发送 Server酱通知
                        break;
                    }
                    // 失败立即重试，无 sleep
                } catch (Exception e) {
                    log.error("单次预约异常", e);
                }
            }

        } catch (Exception e) {
            log.error("预约流程致命错误", e);
        }
    }
}