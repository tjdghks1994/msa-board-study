package kuke.board.comment.service;

import kuke.board.comment.entity.Comment;
import kuke.board.comment.repository.CommentRepository;
import kuke.board.comment.service.request.CommentCreateRequest;
import kuke.board.comment.service.response.CommentPageResponse;
import kuke.board.comment.service.response.CommentResponse;
import kuke.board.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final Snowflake snowflake = new Snowflake();

    @Transactional
    public CommentResponse create(CommentCreateRequest request) {
        Comment parent = findParent(request);
        Comment comment = commentRepository.save(
                Comment.create(
                        snowflake.nextId(),
                        request.getContent(),
                        parent == null ? null : parent.getParentCommentId(),
                        request.getArticleId(),
                        request.getWriterId()
                )
        );

        return CommentResponse.from(comment);
    }

    public CommentResponse read(Long commentId) {
        return CommentResponse.from(
                commentRepository.findById(commentId).orElseThrow()
        );
    }

    @Transactional
    public void delete(Long commentId) {
        commentRepository.findById(commentId)
                .filter(Predicate.not(Comment::getDeleted))
                .ifPresent(comment -> {
                    if (hasChildren(comment)) { // 자식 댓글이 존재하는지
                        comment.delete();   // 삭제 표시만
                    } else {
                        delete(comment);    // 실제 삭제
                    }
                });
    }

    // 페이지 번호 방식
    public CommentPageResponse readAll(Long articleId, Long page, Long pageSize) {
        return CommentPageResponse.of(
                commentRepository.findAll(articleId, (page - 1) * pageSize, pageSize).stream()
                        .map(CommentResponse::from)
                        .toList(),
                commentRepository.count(articleId, PageLimitCalculator.calculatePageLimit(page, pageSize, 10L))
        );
    }

    // 무한 스크롤 방식
    public List<CommentResponse> readAll(Long articleId, Long lastParentCommentId, Long lastCommentId, Long limit) {
        List<Comment> comments = lastParentCommentId == null || lastCommentId == null ?
                commentRepository.findAllInfiniteScroll(articleId, limit) :
                commentRepository.findAllInfiniteScroll(articleId, lastParentCommentId, lastCommentId, limit);

        return comments.stream().map(CommentResponse::from).toList();
    }

    private Comment findParent(CommentCreateRequest request) {
        Long parentCommentId = request.getParentCommentId();
        if(parentCommentId == null) {
            return null;
        }

        return commentRepository.findById(parentCommentId)
                .filter(Predicate.not(Comment::getDeleted))
                .filter(Comment::isRoot)
                .orElseThrow();
    }

    private boolean hasChildren(Comment comment) {
        // 자신과 하위 자식이 1개 이상 존재하는지 확인
        return commentRepository.countBy(comment.getArticleId(), comment.getCommentId(), 2L) == 2;
    }

    private void delete(Comment comment) {
        commentRepository.delete(comment);
        if (!comment.isRoot()) {    // 삭제한 댓글이 부모댓글이 아닌경우
            commentRepository.findById(comment.getParentCommentId())    // 부모댓글 조회
                    .filter(Comment::getDeleted)    // 삭제 표시 상태인 부모댓글인지 확인
                    .filter(Predicate.not(this::hasChildren))   // 자식 댓글이 없는지 확인
                    .ifPresent(commentRepository::delete);  // 위 2개의 조건을 모두 만족하면 부모댓글 삭제 처리
        }
    }


}
