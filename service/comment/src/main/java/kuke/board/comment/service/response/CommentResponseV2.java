package kuke.board.comment.service.response;

import kuke.board.comment.entity.CommentV2;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@ToString
public class CommentResponseV2 {
    private Long commentId;
    private String content;
    private Long articleId;
    private Long writerId;
    private Boolean deleted;
    private String path;
    private LocalDateTime createdAt;

    public static CommentResponseV2 from(CommentV2 comment) {
        CommentResponseV2 commentResponse = new CommentResponseV2();
        commentResponse.commentId = comment.getCommentId();
        commentResponse.content = comment.getContent();
        commentResponse.path = comment.getCommentPath().getPath();
        commentResponse.articleId = comment.getArticleId();
        commentResponse.writerId = comment.getWriterId();
        commentResponse.deleted = comment.getDeleted();
        commentResponse.createdAt = comment.getCreatedAt();
        return commentResponse;
    }
}
