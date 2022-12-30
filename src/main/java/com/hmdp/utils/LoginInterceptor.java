package com.hmdp.utils;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断是否要拦截：根据 threadlocal 里面是否有 user
        //在上一层拦截器 RefreshTokenInterceptor 已经把 User 放到 threadlocal 里面了（满足条件的话）
        if (UserHolder.getUser() == null) {
            response.setStatus(401); //401 未授权
            System.out.println("已拦截");
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser(); //当前是同一个线程，所以不需要指定 remove 的 user
        //移除当前 Localhost，防止内存泄露
    }
}
