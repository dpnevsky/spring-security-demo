package com.pnevsky.spring_security.controllers;

import com.pnevsky.spring_security.security.PersonDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import com.pnevsky.spring_security.services.AdminService;

@Controller
public class StartController {

    private final AdminService adminService;

    public StartController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/hello")
    public String helloPage(){
        return "hello";
    }

    @GetMapping("/showUserInfo")
    public String userInfo(@AuthenticationPrincipal PersonDetails personDetails, Model model){
        model.addAttribute("username", personDetails.getUsername());
        model.addAttribute("role", personDetails.getPerson().getRole());
        return "hello";
    }

    @GetMapping("/admin")
    public String adminPage(){
        adminService.doAdminStaff();
        return "admin";
    }

}
