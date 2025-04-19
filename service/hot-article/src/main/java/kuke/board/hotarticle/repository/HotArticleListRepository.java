package kuke.board.hotarticle.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class HotArticleListRepository {
    private final StringRedisTemplate redisTemplate;

    // hot-article::list::{yyyyMMdd}
    private static final String KEY_FORMAT = "hot-article::list::%s";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public void add(Long articleId, LocalDateTime time, Long score, Long limit, Duration ttl) {
        // 인기글을 실시간으로 생성
        redisTemplate.executePipelined((RedisCallback<?>)  action -> {
            StringRedisConnection conn = (StringRedisConnection) action;
            String key = generateKey(time);
            conn.zAdd(key, score, String.valueOf(articleId));   // 레디스에 key 값에 sortedSet 을 만들고 articleId를 점수와 함께 정렬하여 저장
            conn.zRemRange(key, 0, -limit - 1); // 상위 limit 개만 유지하고 나머지는 삭제
            conn.expire(key, ttl.toSeconds());  // 만료시간

            return null;
        });
    }

    public List<Long> readAll(String dateStr) {
        return redisTemplate.opsForZSet()
                .reverseRangeWithScores(generateKey(dateStr), 0, -1)// 점수를 기준으로 내림차순
                .stream()
                .peek(tuple ->
                        log.info("[HotArticleListRepository.readAll] articleId={}, score={}", tuple.getValue(), tuple.getScore())
                )
                .map(ZSetOperations.TypedTuple::getValue)
                .map(Long::valueOf)
                .toList();
    }

    private String generateKey(LocalDateTime time) {
        return generateKey(TIME_FORMATTER.format(time));
    }

    private String generateKey(String dateStr) {
        return KEY_FORMAT.formatted(dateStr);
    }

}
