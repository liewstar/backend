package com.gitgle.service.resp;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchResp implements Serializable {

    private String login;

    private String avatar;

    private String talentRank;
}