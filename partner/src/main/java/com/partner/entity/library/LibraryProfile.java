package com.partner.entity.library;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LibraryProfile {
    private String studentId;
    private String password;
    private String seatLabel;
    private Integer seatId;
    private Boolean autoEnable;
    private String sendKey;
    private LocalDateTime updateTime;
}