package com.bluecrystal.user.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户表 `user`（MySQL 保留字，实体必须用反引号包裹表名）。 */
@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 密文，绝不返回给前端。 */
    private String password;

    private String phone;

    /** 1 正常，0 冻结。 */
    private Integer status;

    /** 余额，单位：分。 */
    private Integer balance;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
