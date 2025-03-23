package kuke.board.article.service.request;

import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@ToString
public class ArticleCreateRequest {
    private String title;
    private String content;
    private Long writerId;
    private Long boardId;
}
