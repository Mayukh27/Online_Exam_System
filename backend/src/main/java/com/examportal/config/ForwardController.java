package com.examportal.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ForwardController {

    @RequestMapping(value = {
            "/auth/**",
            "/login","/register",
            "/student/**",
            "/admin/**",
            "/teacher/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}