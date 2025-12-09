package com.partner.service;

import com.partner.entity.SystemNotice;
import com.partner.mapper.SystemNoticeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NoticeService {
    @Autowired
    SystemNoticeMapper noticeMapper;

    // 获取公告列表 (所有用户)
    public List<SystemNotice> getNotices() {
        return noticeMapper.findAllNotices();
    }

    // 发布公告 (仅管理员)
    public void publishNotice(SystemNotice notice) {
        // 实际场景中应先校验当前用户是否为管理员(role==1)
        noticeMapper.insertNotice(notice);
    }
}