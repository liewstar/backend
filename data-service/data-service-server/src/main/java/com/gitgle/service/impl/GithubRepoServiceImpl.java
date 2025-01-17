package com.gitgle.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.gitgle.constant.RedisConstant;
import com.gitgle.constant.RpcResultCode;
import com.gitgle.convert.GithubRepoContentConvert;
import com.gitgle.request.GithubRequest;
import com.gitgle.response.*;
import com.gitgle.result.RpcResult;
import com.gitgle.service.*;
import com.gitgle.utils.GithubApiRequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.redis.core.RedisTemplate;

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
    private ContributorService contributorService;

    @Resource
    private RepoContentService repoContentService;

    @Resource
    private RepoLanguageService repoLanguageService;

    @Resource
    private RedisTemplate redisTemplate;

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
            });
            githubReposRpcResult.setData(githubRepos);
            githubReposRpcResult.setCode(RpcResultCode.SUCCESS);
            return githubReposRpcResult;
        } catch (IOException e) {
            log.error("获取仓库信息失败: {}", e.getMessage());
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
            });
            githubReposContentRpcResult.setData(githubReposContent);
            githubReposContentRpcResult.setCode(RpcResultCode.SUCCESS);
            return githubReposContentRpcResult;
        } catch (IOException e) {
            log.error("获取仓库文件信息失败: {}", e.getMessage());
            githubReposContentRpcResult.setCode(RpcResultCode.FAILED);
            return githubReposContentRpcResult;
        }
    }

    @Override
    public RpcResult<GithubReposResponse> listUserRepos(String owner) {
        RpcResult<GithubReposResponse> githubReposResponseRpcResult = new RpcResult<>();
        Map<String, String> queryParams = new HashMap<>();
        try {
            List<GithubRepos> githubReposList = reposService.getReposByLogin(owner);
            if(ObjectUtils.isNotEmpty(githubReposList)){
                GithubReposResponse githubReposResponse = new GithubReposResponse();
                githubReposResponse.setGithubProjectList(githubReposList);
                githubReposResponseRpcResult.setData(githubReposResponse);
                githubReposResponseRpcResult.setCode(RpcResultCode.SUCCESS);
                return githubReposResponseRpcResult;
            }
            GithubReposResponse githubReposResponse = githubApiRequestUtils.listUserRepos(owner, queryParams);
            CompletableFuture.runAsync(()->{
                for(GithubRepos githubRepos: githubReposResponse.getGithubProjectList()){
                    reposService.writeGithubRepos2Repos(githubRepos);
                }
            });
            githubReposResponseRpcResult.setData(githubReposResponse);
            githubReposResponseRpcResult.setCode(RpcResultCode.SUCCESS);
            return githubReposResponseRpcResult;
        } catch (IOException e) {
            log.info("获取用户的仓库失败: {}", e.getMessage());
            githubReposResponseRpcResult.setCode(RpcResultCode.FAILED);
            return githubReposResponseRpcResult;
        }
    }

    @Override
    public RpcResult<GithubContributorResponse> listRepoContributors(String owner, String repoName) {
        RpcResult<GithubContributorResponse> githubContributorResponseRpcResult = new RpcResult<>();
        try{
            // 先查库
            List<GithubContributor> githubContributorList = contributorService.readContributor2GithubContributor(repoName, owner);
            if(ObjectUtils.isNotEmpty(githubContributorList)){
                GithubContributorResponse githubContributorResponse = new GithubContributorResponse();
                githubContributorResponse.setGithubContributorList(githubContributorList);
                githubContributorResponseRpcResult.setData(githubContributorResponse);
                githubContributorResponseRpcResult.setCode(RpcResultCode.SUCCESS);
                return githubContributorResponseRpcResult;
            }
            Map<String,String> params = new HashMap<>();
            GithubContributorResponse githubContributorResponse = githubApiRequestUtils.listRepoContributors(owner, repoName, params);
            CompletableFuture.runAsync(()->{
                contributorService.writeGithubContributor2Contributor(githubContributorResponse.getGithubContributorList());
            });
            githubContributorResponseRpcResult.setData(githubContributorResponse);
            githubContributorResponseRpcResult.setCode(RpcResultCode.SUCCESS);
            return githubContributorResponseRpcResult;
        }catch (IOException e){
            log.error("获取仓库贡献者失败: {}", e.getMessage());
            githubContributorResponseRpcResult.setCode(RpcResultCode.FAILED);
            return githubContributorResponseRpcResult;
        }
    }

    @Override
    public RpcResult<GithubLanguagesResponse> getRepoLanguages(String owner, String repoName) {
        RpcResult<GithubLanguagesResponse> githubLanguagesResponseRpcResult = new RpcResult<>();
        try {
            // 先查库
            Map<String, Integer> repoLanguages = repoLanguageService.readRepoLanguages(repoName, owner);
            if(ObjectUtils.isNotEmpty(repoLanguages)){
                GithubLanguagesResponse githubLanguagesResponse = new GithubLanguagesResponse();
                githubLanguagesResponse.setLanguagesMap(repoLanguages);
                githubLanguagesResponseRpcResult.setData(githubLanguagesResponse);
                githubLanguagesResponseRpcResult.setCode(RpcResultCode.SUCCESS);
                return githubLanguagesResponseRpcResult;
            }
            GithubLanguagesResponse githubLanguagesResponse = githubApiRequestUtils.listRepoLanguages(owner, repoName);
            CompletableFuture.runAsync(()->{
                repoLanguageService.writeRepoLanguages(owner, repoName, githubLanguagesResponse.getLanguagesMap());
            });
            githubLanguagesResponseRpcResult.setData(githubLanguagesResponse);
            githubLanguagesResponseRpcResult.setCode(RpcResultCode.SUCCESS);
            return githubLanguagesResponseRpcResult;
        } catch (IOException e) {
            log.error("获取仓库语言失败: {}", e.getMessage());
            githubLanguagesResponseRpcResult.setCode(RpcResultCode.FAILED);
            return githubLanguagesResponseRpcResult;
        }
    }

    @Override
    public RpcResult<GithubRepoRankResponse> getHotRepos() {
        RpcResult<GithubRepoRankResponse> githubReposResponseRpcResult = new RpcResult<>();
        GithubRepoRankResponse githubRepoRankResponse = new GithubRepoRankResponse();
        List<GithubRepoRank> githubRepoRankList = redisTemplate.opsForList().range(RedisConstant.GITHUB_REPO_RANK, 0, 50);
        if(ObjectUtils.isNotEmpty(githubRepoRankList)){
            githubRepoRankResponse.setGithubRepoRankList(githubRepoRankList);
            githubReposResponseRpcResult.setData(githubRepoRankResponse);
            githubReposResponseRpcResult.setCode(RpcResultCode.SUCCESS);
            return githubReposResponseRpcResult;
        }
        githubRepoRankList = reposService.getReposOrderByStar();
        githubRepoRankResponse.setGithubRepoRankList(githubRepoRankList);
        githubReposResponseRpcResult.setData(githubRepoRankResponse);
        githubReposResponseRpcResult.setCode(RpcResultCode.SUCCESS);
        return githubReposResponseRpcResult;
    }

    @Override
    public RpcResult<PageRepoResponse> getReposOrderByStar(Integer page, Integer size) {
        RpcResult<PageRepoResponse> githubReposResponseRpcResult = new RpcResult<>();
        PageRepoResponse pageRepoResponse = reposService.pageRepos2GithubRepos(page, size);
        githubReposResponseRpcResult.setData(pageRepoResponse);
        githubReposResponseRpcResult.setCode(RpcResultCode.SUCCESS);
        return githubReposResponseRpcResult;
    }
}
