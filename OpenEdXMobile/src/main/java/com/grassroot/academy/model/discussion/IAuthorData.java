package com.grassroot.academy.model.discussion;


import java.util.Date;

public interface IAuthorData {

    String getAuthor();
    String getAuthorLabel();
    Date getCreatedAt();
    boolean isAuthorAnonymous();
}
