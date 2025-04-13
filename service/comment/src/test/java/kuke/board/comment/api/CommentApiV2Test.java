package kuke.board.comment.api;

import kuke.board.comment.service.response.CommentPageResponseV2;
import kuke.board.comment.service.response.CommentResponseV2;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;

public class CommentApiV2Test {
    RestClient restClient = RestClient.create("http://localhost:9001");

    @Test
    void create() {
        CommentResponseV2 response1 =
                createComment(new CommentCreateRequestV2(1L, "my comment1", null, 1L));
        CommentResponseV2 response2 =
                createComment(new CommentCreateRequestV2(1L, "my comment1", response1.getPath(), 1L));
        CommentResponseV2 response3 =
                createComment(new CommentCreateRequestV2(1L, "my comment1", response2.getPath(), 1L));

        System.out.println("response1.getPath() = " + response1.getPath());
        System.out.println("response1.getCommentId() = " + response1.getCommentId());
        System.out.println("\tresponse2.getPath() = " + response2.getPath());
        System.out.println("\t\tresponse3.getPath() = " + response3.getPath());
    }

    @Test
    void read() {
        CommentResponseV2 response = restClient.get()
                .uri("/v2/comments/{commentId}", 167265225855291392L)
                .retrieve()
                .body(CommentResponseV2.class);

        System.out.println("response = " + response);
    }

    @Test
    void delete() {
        restClient.delete()
                .uri("/v2/comments/{commentId}", 167265225855291392L)
                .retrieve();
    }

    @Test
    void readAll() {
        CommentPageResponseV2 responseV2 = restClient.get()
                .uri("/v2/comments?articleId=1&pageSize=10&page=50000")
                .retrieve()
                .body(CommentPageResponseV2.class);

        System.out.println("responseV2 getCommentCount() = " + responseV2.getCommentCount());
        for (CommentResponseV2 comment : responseV2.getComments()) {
            System.out.println("comment.getCommentId() = " + comment.getCommentId());
        }
    }

    @Test
    void readALlInfiniteScroll() {
        List<CommentResponseV2> response1 = restClient.get()
                .uri("/v2/comments/infinite-scroll?articleId=1&pageSize=5")
                .retrieve()
                .body(new ParameterizedTypeReference<List<CommentResponseV2>>() {
                });

        System.out.println("firstPage");
        for (CommentResponseV2 comment : response1) {
            System.out.println("comment.getCommentId() = " + comment.getCommentId());
        }

        String lastPath = response1.getLast().getPath();
        List<CommentResponseV2> response2 = restClient.get()
                .uri("/v2/comments/infinite-scroll?articleId=1&pageSize=5&lastPath=%s".formatted(lastPath))
                .retrieve()
                .body(new ParameterizedTypeReference<List<CommentResponseV2>>() {
                });

        System.out.println("secondPage");
        for (CommentResponseV2 comment : response2) {
            System.out.println("comment.getCommentId() = " + comment.getCommentId());
        }
    }

    CommentResponseV2 createComment(CommentCreateRequestV2 request) {
        return restClient.post()
                .uri("/v2/comments")
                .body(request)
                .retrieve()
                .body(CommentResponseV2.class);
    }

    @Getter
    @AllArgsConstructor
    public static class CommentCreateRequestV2 {
        private Long articleId;
        private String content;
        private String parentPath;
        private Long writerId;
    }
}
