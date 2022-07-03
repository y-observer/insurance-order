package cn.net.insurance.order.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/login")
@RestController
public class LoginController {

    @PostMapping("/accountLogin")
    public void accountLogin(String account) {
        System.out.println("业务实现---->" + account);
    }
}
