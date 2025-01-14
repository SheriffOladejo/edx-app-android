package com.grassroot.academy.model.api;

/**
 * For backward compatibility only
 */
public interface IPathNode {

    public boolean isChapter();

    public boolean isSequential();

    public boolean isVertical();

    public String getCategory();

    public String getDisplayName();

    public String getId();

}
