package com.example.controller;

import com.example.common.Result;
import com.example.service.DuobaoService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/duobao")
public class DuobaoController {

    @Resource
    private DuobaoService duobaoService;

    @PostMapping("/chat")
    public Result chat(@RequestBody Map<String, Object> request) {
        String message = (String) request.get("message");
        List<Map<String, String>> history = (List<Map<String, String>>) request.get("history");
        
        if (message == null || message.trim().isEmpty()) {
            return Result.error("请输入消息");
        }
        
        String response = duobaoService.chat(message, history);
        return Result.success(response);
    }
}
