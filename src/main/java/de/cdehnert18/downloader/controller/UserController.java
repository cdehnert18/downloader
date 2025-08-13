package de.cdehnert18.downloader.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserController {
    
    @GetMapping("/")
    public String viewPage() {
        return "index";
    }

}
