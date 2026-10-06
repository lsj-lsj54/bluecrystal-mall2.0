package com.bluecrystal.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecrystal.common.exception.BadRequestException;
import com.bluecrystal.common.exception.BizIllegalException;
import com.bluecrystal.common.exception.ForbiddenException;
import com.bluecrystal.common.exception.UnauthorizedException;
import com.bluecrystal.common.utils.UserContext;
import com.bluecrystal.user.domain.dto.LoginFormDTO;
import com.bluecrystal.user.domain.po.User;
import com.bluecrystal.user.domain.vo.UserLoginVO;
import com.bluecrystal.user.domain.vo.UserVO;
import com.bluecrystal.user.mapper.UserMapper;
import com.bluecrystal.user.service.IUserService;
import com.bluecrystal.user.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    /** BCrypt 校验器：无状态且线程安全，全局复用一个实例即可。 */
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final UserMapper userMapper;

    private final JwtTool jwtTool;

    @Override
    public UserLoginVO login(LoginFormDTO form) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, form.username()));
        // 用户不存在与密码错误返回同一句提示，避免暴露某个用户名是否已注册
        if (user == null || !PASSWORD_ENCODER.matches(form.password(), user.getPassword())) {
            throw new UnauthorizedException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new ForbiddenException("账号已被冻结");
        }
        String token = jwtTool.createToken(user.getId());
        log.info("用户 {} 登录成功", user.getId());
        return new UserLoginVO(token, user.getId(), user.getUsername(), user.getBalance());
    }

    @Override
    public UserVO currentUser() {
        Long userId = UserContext.requireUser();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BadRequestException("用户不存在：" + userId);
        }
        return toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductBalance(Long userId, Integer amount) {
        if (amount == null || amount <= 0) {
            throw new BadRequestException("扣减金额必须为正数");
        }
        int rows = userMapper.deductBalance(userId, amount);
        if (rows == 0) {
            // 抛出异常让本地事务与全局事务一起回滚
            throw new BizIllegalException("余额不足");
        }
        log.info("用户 {} 扣减余额 {} 分", userId, amount);
    }

    private UserVO toVO(User user) {
        return new UserVO(user.getId(), user.getUsername(), user.getPhone(),
                user.getBalance(), user.getStatus());
    }
}
