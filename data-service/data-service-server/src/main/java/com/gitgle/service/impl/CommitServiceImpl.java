package com.gitgle.service.impl;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gitgle.dao.Commit;
import com.gitgle.mapper.CommitMapper;
import com.gitgle.request.GithubRequest;
import com.gitgle.response.GithubCommit;
import com.gitgle.service.CommitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommitServiceImpl implements CommitService {

    @Resource
    private CommitMapper commitMapper;

    @Override
    public void writeGithubCommit2Commit(GithubCommit githubCommit) {
        // 先根据sha查询数据库中是否存在
        Commit commit = commitMapper.selectOne(Wrappers.lambdaQuery(Commit.class).eq(Commit::getSha, githubCommit.getSha()));
        if(ObjectUtils.isNotEmpty(commit)){
            return;
        }
        // 如果没有则入库
        commit = new Commit();
        commit.setCommitDateTime(githubCommit.getCommitDataTime());
        commit.setAuthorLogin(githubCommit.getAuthorLogin());
        commit.setReposId(githubCommit.getReposId());
        commit.setReposName(githubCommit.getReposName());
        commit.setReposOwner(githubCommit.getReposOwner());
        commit.setSha(githubCommit.getSha());
        commit.setCreateTime(LocalDateTime.now());
        commit.setUpdateTime(LocalDateTime.now());
        commitMapper.insert(commit);
    }

    @Override
    public List<GithubCommit> readCommit2GithubCommit(String login) {
        List<Commit> commitList = commitMapper.selectList(Wrappers.lambdaQuery(Commit.class).eq(Commit::getAuthorLogin, login));
        if(ObjectUtils.isEmpty(commitList)){
            return null;
        }
        return commitList.stream().map(commit -> {
            GithubCommit githubCommit = new GithubCommit();
            githubCommit.setSha(commit.getSha());
            githubCommit.setAuthorLogin(commit.getAuthorLogin());
            githubCommit.setReposId(commit.getReposId());
            githubCommit.setReposName(commit.getReposName());
            githubCommit.setReposOwner(commit.getReposOwner());
            githubCommit.setCommitDataTime(commit.getCommitDateTime().toString());
            return githubCommit;
        }).collect(Collectors.toList());
    }

    @Override
    public List<GithubCommit> readCommit2GithubCommit(GithubRequest githubRequest) {
        List<Commit> commitList = commitMapper.selectList(Wrappers.lambdaQuery(Commit.class)
                .eq(Commit::getAuthorLogin, githubRequest.getAuthor())
                .eq(Commit::getReposName, githubRequest.getRepoName())
                .eq(Commit::getReposOwner, githubRequest.getOwner()));
        return commitList.stream().map(commit -> {
            GithubCommit githubCommit = new GithubCommit();
            githubCommit.setSha(commit.getSha());
            githubCommit.setAuthorLogin(commit.getAuthorLogin());
            githubCommit.setReposId(commit.getReposId());
            githubCommit.setReposName(commit.getReposName());
            githubCommit.setReposOwner(commit.getReposOwner());
            githubCommit.setCommitDataTime(commit.getCommitDateTime().toString());
            return githubCommit;
        }).collect(Collectors.toList());
    }
}