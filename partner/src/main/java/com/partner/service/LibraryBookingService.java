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

    /**
     * 【新增】测试预约 (同步方法，直接返回结果)
     * 用于管理员手动测试某用户的账号或时间段是否有效
     */
    public LibraryHttpUtil.BookingResult testBooking(String studentId, Integer seatId, String startTime, String endTime) {
        log.info("管理员触发测试预约: 用户={}, 座位={}, 时间={}-{}", studentId, seatId, startTime, endTime);

        // 1. 获取用户配置
        LibraryProfile profile = libraryMapper.findProfile(studentId);
        if (profile == null) {
            return new LibraryHttpUtil.BookingResult(false, "用户不存在", "");
        }

        // 如果未指定座位，使用用户默认座位
        Integer targetSeatId = (seatId != null) ? seatId : profile.getSeatId();
        if (targetSeatId == null) {
            return new LibraryHttpUtil.BookingResult(false, "未配置座位ID，且未传入临时座位ID", "");
        }

        try {
            // 2. 登录获取 Token
            String uuid = httpUtil.getCaptchaUuid();
            LibraryHttpUtil.LoginResult login = httpUtil.login(profile.getStudentId(), profile.getPassword(), uuid);

            // 3. 构建完整的日期时间字符串 (假设测试当天的)
            String date = LocalDate.now().toString();
            // 如果传入的是 HH:mm 格式，补全日期
            String fullStartTime = startTime.contains(" ") ? startTime : (date + " " + startTime + ":00");
            String fullEndTime = endTime.contains(" ") ? endTime : (date + " " + endTime + ":00");

            // 4. 发起一次预约请求
            return httpUtil.bookSeat(
                    targetSeatId,
                    fullStartTime,
                    fullEndTime,
                    login.getToken(),
                    login.getTicket()
            );

        } catch (Exception e) {
            log.error("测试预约异常", e);
            return new LibraryHttpUtil.BookingResult(false, "执行异常: " + e.getMessage(), "");
        }
    }
}