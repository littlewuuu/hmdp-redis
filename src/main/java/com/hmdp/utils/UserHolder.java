package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;

/**
 * 封装 ThreadLocal 操作，用于登录拦截 com.hmdp.utils.LoginInterceptor
 */
public class UserHolder {
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    public static void saveUser(UserDTO user){
        tl.set(user);
    }

    public static UserDTO getUser(){
        return tl.get(); //获取当前 threadlocal 所对应的 value
    }

    public static void removeUser(){
        tl.remove(); //移除当前的 threadlocal
    }
}
