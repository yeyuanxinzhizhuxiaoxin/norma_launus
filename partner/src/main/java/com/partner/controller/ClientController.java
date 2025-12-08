package com.partner.controller;


import com.partner.entity.*;
import com.partner.service.ClientService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/client")
public class ClientController {
    @Autowired
    ClientService clientService;

    /**
     * 客户登录
     * @param client
     * @return
     */
    @PostMapping("/login")
    public Result Login(@RequestBody Client client){
        log.info("客户登录信息：{}",client);
        try{
            // 1.在数据库中查询是否有这个客户
            Client client1 = clientService.findClientByAccount(client.getAccount());
            if(client1 == null){
                clientService.InsertClient(client);
            }
            // 2.登录服务门户获取Cookies中的JSESSIONID
            //客户登录服务门户，并返回已认证的 HttpClient（包含完整 Cookie 上下文）
            //OkHttpClient userHttpClient = clientService.loginServicePortal(client);
            //String js = clientService.loginServicePortal(client);
            LoginContext loginContext = clientService.loginServicePortal(client);


            // 3.登录教务系统
            //String outCame = clientService.enterJiaowuSystem(userHttpClient);
            Map<String,String> cookies= clientService.enterJiaowuSystem(loginContext);

            return Result.success(cookies);
        }catch (Exception e){
            return Result.error(e.getMessage());
        }
    }


    /**
     * 客户查询成绩
     * @param scoreQuery
     * @return
     */
    @PostMapping("/queryScore")
    public Result QueryScore(@RequestBody ScoreQuery scoreQuery){
        List<Score> scoreList = clientService.queryScore(scoreQuery);
        return Result.success(scoreList);
    }


    @GetMapping("/queryClassSchedule")
    public Result QueryClassSchedule(){
        return Result.success();
    }


    @PostMapping("bookSeat")
    public Result BookSeat(){
        return Result.success();
    }
}
