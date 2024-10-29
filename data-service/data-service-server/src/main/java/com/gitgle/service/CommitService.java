package com.gitgle.service;

import com.gitgle.request.GithubRequest;
import com.gitgle.response.GithubCommit;

import java.util.List;

public interface CommitService {

    void writeGithubCommit2Commit(GithubCommit githubCommit);

    List<GithubCommit> readCommit2GithubCommit(String login);

    List<GithubCommit> readCommit2GithubCommit(GithubRequest githubRequest);
}