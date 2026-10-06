package com.bluecrystal.user.service;

import com.bluecrystal.user.domain.dto.LoginFormDTO;
import com.bluecrystal.user.domain.vo.UserLoginVO;
import com.bluecrystal.user.domain.vo.UserVO;

public interface IUserService {

    /** 用户名 + 密码登录，成功后签发 JWT。 */
    UserLoginVO login(LoginFormDTO form);

    /** 查询当前登录用户（用户 id 来自网关透传的请求头）。 */
    UserVO currentUser();

    /** 扣减余额，余额不足则抛业务异常回滚（配合 Seata 全局事务）。 */
    void deductBalance(Long userId, Integer amount);
}
