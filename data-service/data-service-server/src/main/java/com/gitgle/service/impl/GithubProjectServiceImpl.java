package com.gitgle.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gitgle.constant.RpcResultCode;
import com.gitgle.dao.Repos;
import com.gitgle.mapper.ReposMapper;
import com.gitgle.request.GithubRequest;
import com.gitgle.response.*;
import com.gitgle.result.RpcResult;
import com.gitgle.service.GithubProjectService;
import com.gitgle.utils.GithubApiRequestUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.apache.commons.codec.binary.Base64;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@DubboService
@Slf4j
public class GithubProjectServiceImpl implements GithubProjectService {

    @Resource
    private GithubApiRequestUtils githubApiRequestUtils;

    @Resource
    private ReposMapper reposMapper;

    @Override
    public RpcResult<GithubReposResponse> getProjectByDeveloperId(String developerId) {
        return null;
    }

    @Override
    public RpcResult<GithubRepos> getRepoByOwnerAndRepoName(String developerId, String repoName) {
        RpcResult<GithubRepos> githubReposRpcResult = new RpcResult<>();
        try {
            // 先查库
            GithubRepos githubRepos = readGithubRepos(developerId, repoName);
            if(ObjectUtils.isNotEmpty(githubRepos)){
                githubReposRpcResult.setCode(RpcResultCode.SUCCESS);
                githubReposRpcResult.setData(githubRepos);
                return githubReposRpcResult;
            }
            Response response = githubApiRequestUtils.getOneRepo(developerId, repoName);
            if(!response.isSuccessful()){
                githubReposRpcResult.setCode(RpcResultCode.Github_RESPONSE_FAILED);
                return githubReposRpcResult;
            }
            JSONObject responseBody = JSON.parseObject(response.body().string());
            log.info("Github GetRepo Response: {}", responseBody);
            githubRepos = new GithubRepos();
            githubRepos.setId(responseBody.getString("id"));
            githubRepos.setName(responseBody.getString("name"));
            githubRepos.setFullName(responseBody.getString("full_name"));
            githubRepos.setOwnerLogin(responseBody.getJSONObject("owner").getString("login"));
            githubRepos.setDescription(responseBody.getString("description"));
            githubRepos.setForksCount(responseBody.getInteger("forks_count"));
            githubRepos.setStarsCount(responseBody.getInteger("stargazers_count"));
            githubRepos.setWatchersCount(responseBody.getInteger("watchers_count"));
            githubRepos.setCreatedAt(responseBody.getString("created_at"));
            githubRepos.setUpdateAt(responseBody.getString("updated_at"));
            githubRepos.setOrPrivate(responseBody.getBoolean("private"));
            // 异步写库
            final GithubRepos finalGithubRepos = githubRepos;
            CompletableFuture.runAsync(()->{
                writeGithubRepos(finalGithubRepos);
            }).exceptionally(ex -> {
                log.error("Github Write Exception: {}", ex);
                return null;
            });
            githubReposRpcResult.setData(githubRepos);
            githubReposRpcResult.setCode(RpcResultCode.SUCCESS);
            return githubReposRpcResult;
        } catch (IOException e) {
            log.error("Github getRepo Exception: {}", e);
            githubReposRpcResult.setCode(RpcResultCode.FAILED);
            return githubReposRpcResult;
        }
    }

    @Override
    public RpcResult<GithubReposContent> getRepoContentByPath(GithubRequest githubRequest) {
        RpcResult<GithubReposContent> githubReposContentRpcResult = new RpcResult<>();
        GithubReposContent githubReposContent = new GithubReposContent();
        try {
            Response response = githubApiRequestUtils.getRepoContent(githubRequest.getOwner(), githubRequest.getRepoName(), githubRequest.getPath());
            if(!response.isSuccessful()){
                githubReposContentRpcResult.setCode(RpcResultCode.Github_RESPONSE_FAILED);
                return githubReposContentRpcResult;
            }
            JSONObject responseBody = JSON.parseObject(response.body().string());
            log.info("Github SearchUsers Response: {}", responseBody);
            githubReposContent.setPath(responseBody.getString("path"));
            githubReposContent.setName(responseBody.getString("name"));
            githubReposContent.setSha(responseBody.getString("sha"));
            githubReposContent.setType(responseBody.getString("type"));
            githubReposContent.setEncoding(responseBody.getString("encoding"));
            githubReposContent.setSize(responseBody.getInteger("size"));
            byte[] decodedBytes = Base64.decodeBase64(responseBody.getString("content"));
            githubReposContent.setContent(new String(decodedBytes));
            githubReposContentRpcResult.setData(githubReposContent);
            githubReposContentRpcResult.setCode(RpcResultCode.SUCCESS);
            return githubReposContentRpcResult;
        } catch (IOException e) {
            log.error("Github GetRepoContent Exception: {}", e);
            githubReposContentRpcResult.setCode(RpcResultCode.FAILED);
            return githubReposContentRpcResult;
        }
    }

    public GithubRepos readGithubRepos(String developerId, String repoName){
        Repos repo = reposMapper.selectOne(Wrappers.lambdaQuery(Repos.class).eq(Repos::getName, repoName).eq(Repos::getOwnerlogin, developerId));
        if(ObjectUtils.isEmpty(repo)){
            return null;
        }
        GithubRepos githubRepos = new GithubRepos();
            githubRepos.setName(repo.getName());
            githubRepos.setOrPrivate(repo.getOrPrivate());
            githubRepos.setCreatedAt(repo.getCreateAt().toString());
            githubRepos.setUpdateAt(repo.getUpdateAt().toString());
            githubRepos.setStarsCount(repo.getStarsCount());
            githubRepos.setForksCount(repo.getForksCount());
            githubRepos.setIssueCount(repo.getIssueCount());
            return githubRepos;
    }

    public void writeGithubRepos(GithubRepos githubRepos){
        Repos repos = reposMapper.selectOne(Wrappers.lambdaQuery(Repos.class).eq(Repos::getName, githubRepos.getName()).eq(Repos::getOwnerlogin, githubRepos.getOwnerLogin()));
        if(ObjectUtils.isNotEmpty(repos)){
            return;
        }
        Repos repo = new Repos();
        repo.setName(githubRepos.getName());
        repo.setOrPrivate(githubRepos.getOrPrivate());
        repo.setCreateAt(githubRepos.getCreatedAt());
        repo.setUpdateAt(githubRepos.getUpdateAt());
        repo.setStarsCount(githubRepos.getStarsCount());
        repo.setForksCount(githubRepos.getForksCount());
        reposMapper.insert(repo);
    }
}
