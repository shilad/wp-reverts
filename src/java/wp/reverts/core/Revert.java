package wp.reverts.core;

import gnu.trove.map.TIntObjectMap;

public class Revert {
    private Article article;
    private String code;

    private User revertedUser;
    private long revertedTstamp;
    String revertedComment;

    private User revertingUser;
    private long revertingTstamp;
    String revertingComment;

    public Revert(Article article, String code, User revertedUser, long revertedTstamp, User revertingUser, long revertingTstamp) {
        this.revertedUser = revertedUser;
        this.revertedTstamp = revertedTstamp;
        this.revertingUser = revertingUser;
        this.revertingTstamp = revertingTstamp;
        this.article = article;
        this.code = code;
    }

    public User getRevertedUser() {
        return revertedUser;
    }

    public long getRevertedTstamp() {
        return revertedTstamp;
    }

    public User getRevertingUser() {
        return revertingUser;
    }

    public long getRevertingTstamp() {
        return revertingTstamp;
    }

    public Article getArticle() {
        return article;
    }

    public String getCode() {
        return code;
    }

    public String getRevertedComment() {
        return revertedComment;
    }

    public void setRevertedComment(String revertedComment) {
        this.revertedComment = revertedComment;
    }

    public String getRevertingComment() {
        return revertingComment;
    }

    public void setRevertingComment(String revertingComment) {
        this.revertingComment = revertingComment;
    }

}
