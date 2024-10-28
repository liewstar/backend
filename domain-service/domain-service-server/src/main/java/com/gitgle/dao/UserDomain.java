package com.gitgle.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 
 * @TableName user_domain
 */
@TableName(value ="user_domain")
@Data
public class UserDomain implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 
     */
    private String login;

    /**
     * 
     */
    private String domain;

    /**
     * 
     */
    private Double confidence;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}