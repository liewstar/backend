package com.gitgle.service;

import cn.dev33.satoken.util.SaResult;
import com.gitgle.result.R;
import com.gitgle.service.VO.UserVo;


public interface UserService {

    String getUserName();

    //发送验证码邮件给指定邮箱
    R sendMimeMail(String email);

    //用户注册，验证验证码并保存用户信息
    R register(UserVo userVo);

    //登录
    SaResult login(String email, String password) throws Exception;

    //登出
    SaResult logout();
}
