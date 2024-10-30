package com.gitgle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gitgle.entity.GithubUser;
import com.gitgle.service.vo.req.SearchReq;
import com.gitgle.service.vo.resp.SearchResp;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GithubUserMapper extends BaseMapper<GithubUser> {

    List<SearchResp> searchByCondition(SearchReq req);
}