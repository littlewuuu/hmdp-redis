package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Random;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1. verify phone number
        if (RegexUtils.isPhoneInvalid(phone)) {
            //is not valid
            return Result.fail("invalid phone number");
        }
        //2. generate code
        String code = RandomUtil.randomNumbers(6);
        //3. save to session, to varify the code entered by user and code generated
        session.setAttribute("code",code);
        //4. send code
        log.debug("code send : " + code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1. verify phone number
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            //is not valid
            return Result.fail("invalid phone number");
        }

        //code == cachecode?
        String cacheCode = (String) session.getAttribute("code");
        String code = loginForm.getCode();
        if(cacheCode==null || !cacheCode.equals(code)){
            return Result.fail("wrong code");
        }

        //user in DB?
        User user = query().eq("phone", phone).one();
        if(user == null){
            user = saveUserWithPhone(phone);
        }

        //save user to session
        session.setAttribute("user",user);
        return Result.ok();

    }

    private User saveUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(6));
        return user;
    }
}
