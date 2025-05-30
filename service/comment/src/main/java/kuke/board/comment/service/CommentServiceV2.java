package kuke.board.comment.service;

import jakarta.persistence.EntityNotFoundException;
import kuke.board.comment.entity.ArticleCommentCount;
import kuke.board.comment.entity.CommentPath;
import kuke.board.comment.entity.CommentV2;
import kuke.board.comment.repository.ArticleCommentCountRepository;
import kuke.board.comment.repository.CommentRepositoryV2;
import kuke.board.comment.service.request.CommentCreateRequestV2;
import kuke.board.comment.service.response.CommentPageResponseV2;
import kuke.board.comment.service.response.CommentResponseV2;
import kuke.board.common.event.EventType;
import kuke.board.common.event.payload.CommentCreatedEventPayload;
import kuke.board.common.event.payload.CommentDeletedEventPayload;
import kuke.board.common.outboxmessagerelay.OutboxEventPublisher;
import kuke.board.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class CommentServiceV2 {
    private final Snowflake snowflake = new Snowflake();
    private final CommentRepositoryV2 commentRepository;
    private final ArticleCommentCountRepository articleCommentCountRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional
    public CommentResponseV2 create(CommentCreateRequestV2 request) {
        CommentV2 parent = findParent(request);
        CommentPath parentCommentPath = parent == null ? CommentPath.create("") : parent.getCommentPath();

        CommentV2 comment = commentRepository.save(
                CommentV2.create(
                        snowflake.nextId(),
                        request.getContent(),
                        request.getArticleId(),
                        request.getWriterId(),
                        parentCommentPath.createChildCommentPath(
                                commentRepository.findDescendantsTopPath(request.getArticleId(), parentCommentPath.getPath())
                                        .orElse(null)
                        )
                )
        );

        int result = articleCommentCountRepository.increase(request.getArticleId());
        if (result == 0) {
            articleCommentCountRepository.save(
                    ArticleCommentCount.init(request.getArticleId(), 1L)
            );
        }

        outboxEventPublisher.publish(
                EventType.COMMENT_CREATED,
                CommentCreatedEventPayload.builder()
                        .commentId(comment.getCommentId())
                        .content(comment.getContent())
                        .articleId(comment.getArticleId())
                        .writerId(comment.getWriterId())
                        .deleted(comment.getDeleted())
                        .createdAt(comment.getCreatedAt())
                        .articleCommentCount(count(comment.getArticleId()))
                        .build(),
                comment.getArticleId()
        );

        return CommentResponseV2.from(comment);
    }

    public CommentResponseV2 read(Long commentId) {
        return CommentResponseV2.from(
                commentRepository.findById(commentId).orElseThrow()
        );
    }

    @Transactional
    public void delete(Long commentId) {
        commentRepository.findById(commentId)
                .filter(Predicate.not(CommentV2::getDeleted))
                .ifPresent(commentV2 -> {
                    if (hasChildren(commentV2)) {
                        commentV2.delete(); // 삭제 표시만
                    } else {
                        delete(commentV2);   // 실제 삭제
                    }

                    outboxEventPublisher.publish(
                            EventType.COMMENT_DELETED,
                            CommentDeletedEventPayload.builder()
                                    .commentId(commentV2.getCommentId())
                                    .content(commentV2.getContent())
                                    .articleId(commentV2.getArticleId())
                                    .writerId(commentV2.getWriterId())
                                    .deleted(commentV2.getDeleted())
                                    .createdAt(commentV2.getCreatedAt())
                                    .articleCommentCount(count(commentV2.getArticleId()))
                                    .build(),
                            commentV2.getArticleId()
                    );
                });
    }

    public CommentPageResponseV2 readAll(Long articleId, Long page, Long pageSize) {
        return CommentPageResponseV2.of(
                commentRepository.findAll(articleId, (page - 1) * pageSize, pageSize).stream()
                        .map(CommentResponseV2::from)
                        .toList(),
                commentRepository.count(articleId, PageLimitCalculator.calculatePageLimit(page, pageSize, 10L))
        );
    }

    public List<CommentResponseV2> readAllInfiniteScroll(Long articleId, String lastPath, Long pageSize) {
        List<CommentV2> comments = lastPath == null ?
                commentRepository.findAllInfiniteScroll(articleId, pageSize) :
                commentRepository.findAllInfiniteScroll(articleId, lastPath, pageSize);

        return comments.stream()
                .map(CommentResponseV2::from)
                .toList();
    }

    public Long count(Long articleId) {
        return articleCommentCountRepository.findById(articleId)
                .map(ArticleCommentCount::getCommentCount)
                .orElse(0L);
    }

    private CommentV2 findParent(CommentCreateRequestV2 request) {
        String parentPath = request.getParentPath();
        if (parentPath == null) {
            return null;
        }

        return commentRepository.findByPath(parentPath)
                .filter(Predicate.not(CommentV2::getDeleted))
                .orElseThrow(() -> new EntityNotFoundException("not found parent path = " + parentPath));
    }

    private boolean hasChildren(CommentV2 comment) {
        return commentRepository.findDescendantsTopPath(
                comment.getArticleId(),
                comment.getCommentPath().getPath()
        ).isPresent();
    }

    private void delete(CommentV2 comment) {
        commentRepository.delete(comment);
        articleCommentCountRepository.decrease(comment.getArticleId());
        if (!comment.isRoot()) {    // 삭제한 댓글이 상위댓글이 아닌경우
            commentRepository.findByPath(comment.getCommentPath().getParentPath())    // 상위경로 조회
                    .filter(CommentV2::getDeleted)    // 삭제 표시 상태인 상위댓글인지 확인
                    .filter(Predicate.not(this::hasChildren))   // 하위 댓글이 없는지 확인
                    .ifPresent(this::delete);  // 위 2개의 조건을 모두 만족하면 상위댓글도 삭제 처리
        }
    }
}
