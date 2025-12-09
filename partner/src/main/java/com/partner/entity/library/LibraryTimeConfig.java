package com.partner.entity.library;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LibraryTimeConfig {
    private Long id;
    private String studentId;
    private String startTime;
    private String endTime;
    private String autoStartTime;
    private Boolean isActive;
    private LocalDateTime createTime;
}