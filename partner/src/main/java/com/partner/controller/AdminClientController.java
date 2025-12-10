package com.partner.controller;

import com.partner.entity.Client;
import com.partner.entity.Result;
import com.partner.mapper.ClientMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminClientController {

    @Autowired
    private ClientMapper clientMapper;

    /**
     * 分页查询用户列表
     * GET /admin/clients?page=1&size=10&keyword=张三
     */
    @GetMapping("/clients")
    public Result getClients(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(required = false) String keyword) {

        int offset = (page - 1) * size;
        List<Client> list = clientMapper.selectByPage(keyword, offset, size);
        long total = clientMapper.count(keyword);

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);

        return Result.success(data);
    }

    /**
     * 新增用户
     */
    @PostMapping("/client")
    public Result addClient(@RequestBody Client client) {
        if (client.getAccount() == null || client.getPassword() == null) {
            return Result.error("账号和密码不能为空");
        }

        // 简单查重
        Client exist = clientMapper.findClientByAccount(client.getAccount());
        if (exist != null) {
            return Result.error("该账号已存在");
        }

        // 默认昵称
        if (client.getName() == null) {
            client.setName("同学" + client.getAccount());
        }

        clientMapper.insertClient(client);
        return Result.success("用户添加成功");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/client/{id}")
    public Result deleteClient(@PathVariable Long id) {
        clientMapper.deleteById(id);
        return Result.success("用户删除成功");
    }
}