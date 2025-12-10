package com.partner.controller;

import com.partner.dto.LoginDTO;
import com.partner.entity.Client;
import com.partner.entity.Result;
import com.partner.mapper.ClientMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/admin")
public class SystemAdminController {

    @Autowired
    private ClientMapper clientMapper;

    /**
     * 管理员登录接口
     * 不走教务爬虫，直接查本地数据库
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO loginDTO) {
        log.info("管理员尝试登录: {}", loginDTO.getAccount());

        // 1. 校验参数
        if (loginDTO.getAccount() == null || loginDTO.getPassword() == null) {
            return Result.error("账号或密码不能为空");
        }

        // 2. 查库验证
        Client client = clientMapper.loginCheck(loginDTO.getAccount(), loginDTO.getPassword());

        // 3. 验证结果
        if (client == null) {
            return Result.error("账号或密码错误");
        }

        // 4. 权限验证 (role = 1 为管理员)
        if (client.getRole() == null || client.getRole() != 1) {
            return Result.error("该账号没有管理员权限");
        }

        // 5. 生成 Token (这里简单模拟，实际项目建议使用 JWT)
        String token = UUID.randomUUID().toString().replace("-", "");

        // 6. 返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("name", client.getName());
        data.put("account", client.getAccount());
        data.put("role", client.getRole());

        log.info("管理员登录成功: {}", client.getName());
        return Result.success(data);
    }
}