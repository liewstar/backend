package com.gitgle.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.gitgle.constant.RpcResultCode;
import com.gitgle.convert.GithubRepoContentConvert;
import com.gitgle.convert.GithubRepoConvert;
import com.gitgle.request.GithubRequest;
import com.gitgle.response.*;
import com.gitgle.result.RpcResult;
import com.gitgle.service.GithubRepoService;
import com.gitgle.service.RepoContentService;
import com.gitgle.service.ReposService;
import com.gitgle.utils.GithubApiRequestUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.apache.commons.codec.binary.Base64;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@DubboService
@Slf4j
public class GithubRepoServiceImpl implements GithubRepoService {

    @Resource
    private GithubApiRequestUtils githubApiRequestUtils;

    @Resource
    private ReposService reposService;

    @Resource
    private RepoContentService repoContentService;

    @Override
    public RpcResult<GithubRepos> getRepoByOwnerAndRepoName(String developerId, String repoName) {
        RpcResult<GithubRepos> githubReposRpcResult = new RpcResult<>();
        try {
            // 先查库
            GithubRepos githubRepos = reposService.readRepos2GithubRepos(developerId, repoName);
            if(ObjectUtils.isNotEmpty(githubRepos)){
                githubReposRpcResult.setCode(RpcResultCode.SUCCESS);
                githubReposRpcResult.setData(githubRepos);
                return githubReposRpcResult;
            }
            githubRepos = githubApiRequestUtils.getOneRepo(developerId, repoName);
            // 异步写库
            final GithubRepos finalGithubRepos = githubRepos;
            CompletableFuture.runAsync(()->{
                reposService.writeGithubRepos2Repos(finalGithubRepos);
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
        try {
            // 先查库，没有再github上搜索
            GithubReposContent githubReposContent = repoContentService.readRepoContent2GithubReposContent(githubRequest.getPath(), githubRequest.getRepoName(), githubRequest.getOwner());
            if(ObjectUtils.isNotEmpty(githubReposContent)){
                githubReposContentRpcResult.setCode(RpcResultCode.SUCCESS);
                githubReposContentRpcResult.setData(githubReposContent);
                return githubReposContentRpcResult;
            }
            JSONObject response = githubApiRequestUtils.getRepoContent(githubRequest.getOwner(), githubRequest.getRepoName(), githubRequest.getPath());
            githubReposContent = GithubRepoContentConvert.convert(response, githubRequest);
            final GithubReposContent finalGithubReposContent = githubReposContent;
            // 异步入库
            CompletableFuture.runAsync(()->{
                repoContentService.writeGithubReposContent2RepoContent(finalGithubReposContent);
            }).exceptionally(ex -> {
                log.error("Github Write Exception: {}", ex);
                return null;
            });
            githubReposContentRpcResult.setData(githubReposContent);
            githubReposContentRpcResult.setCode(RpcResultCode.SUCCESS);
            return githubReposContentRpcResult;
        } catch (IOException e) {
            log.error("Github GetRepoContent Exception: {}", e);
            githubReposContentRpcResult.setCode(RpcResultCode.FAILED);
            return githubReposContentRpcResult;
        }
    }

    @Override
    public RpcResult<GithubReposResponse> listUserRepos(String owner) {
        RpcResult<GithubReposResponse> githubReposResponseRpcResult = new RpcResult<>();
        Map<String, String> queryParams = new HashMap<>();
        try {
            GithubReposResponse githubReposResponse = githubApiRequestUtils.listUserRepos(owner, queryParams);
            githubReposResponseRpcResult.setData(githubReposResponse);
            githubReposResponseRpcResult.setCode(RpcResultCode.SUCCESS);
            return githubReposResponseRpcResult;
        } catch (IOException e) {
            log.info("Github ListUserRepos Exception: {}", e.getMessage());
            githubReposResponseRpcResult.setCode(RpcResultCode.FAILED);
            return githubReposResponseRpcResult;
        }
    }

    @Override
    public RpcResult<GithubContributorResponse> listRepoContributors(String owner, String repoName) {
        RpcResult<GithubContributorResponse> githubContributorResponseRpcResult = new RpcResult<>();
        try{
            Map<String,String> params = new HashMap<>();
            GithubContributorResponse githubContributorResponse = githubApiRequestUtils.listRepoContributors(owner, repoName, params);
            githubContributorResponseRpcResult.setData(githubContributorResponse);
            githubContributorResponseRpcResult.setCode(RpcResultCode.SUCCESS);
            return githubContributorResponseRpcResult;
        }catch (IOException e){
            log.error("Github ListRepoContributors Exception: {}", e.getMessage());
            githubContributorResponseRpcResult.setCode(RpcResultCode.FAILED);
            return githubContributorResponseRpcResult;
        }
    }

    @Override
    public RpcResult<GithubLanguagesResponse> getRepoLanguages(String owner, String repoName) {
        RpcResult<GithubLanguagesResponse> githubLanguagesResponseRpcResult = new RpcResult<>();
        try {
            GithubLanguagesResponse githubLanguagesResponse = githubApiRequestUtils.listRepoLanguages(owner, repoName);
            githubLanguagesResponseRpcResult.setData(githubLanguagesResponse);
            githubLanguagesResponseRpcResult.setCode(RpcResultCode.SUCCESS);
            return githubLanguagesResponseRpcResult;
        } catch (IOException e) {
            log.error("Github GetRepoLanguages Exception: {}", e.getMessage());
            githubLanguagesResponseRpcResult.setCode(RpcResultCode.FAILED);
            return githubLanguagesResponseRpcResult;
        }
    }
}
