package com.app.authentication.controller;

import com.app.authentication.common.CommonApiReturn;
import com.app.authentication.entity.TMstUser;
import com.app.authentication.model.TMstUserModel;
import com.app.authentication.service.TMstUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AuthenticationController {
    @Autowired
    private TMstUserService tMstUserService;

    public AuthenticationController() {
        this.tMstUserService = new TMstUserService();
    }

    @PostMapping("/authentication/do_signup")
    public CommonApiReturn<TMstUser> do_signup(@RequestBody TMstUserModel new_user){
        TMstUser saved_user = tMstUserService.saveProduct(new_user);
        return CommonApiReturn.success(saved_user);
    }
}