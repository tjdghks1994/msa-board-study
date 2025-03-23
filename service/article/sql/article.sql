SELECT * FROM article
WHERE board_id = 1
ORDER BY article_id DESC
    LIMIT 30 OFFSET 1499970;

-- 페이징 쿼리에서 OFFSET 이 커질수록 조회 속도가 느려지는 문제 발생
EXPLAIN SELECT * FROM article
        WHERE board_id = 1
        ORDER BY article_id DESC
            LIMIT 30 OFFSET 1499970;

SELECT board_id, article_id FROM article
WHERE board_id = 1
ORDER BY article_id DESC
    LIMIT 30 OFFSET 1499970;

-- 커버링 인덱스를 활용해 조회속도가 느려지는 문제 해결
EXPLAIN SELECT board_id, article_id FROM article
        WHERE board_id = 1
        ORDER BY article_id DESC
            LIMIT 30 OFFSET 1499970;

SELECT * FROM (
  SELECT article_id FROM article
  WHERE board_id = 1
  ORDER BY article_id DESC
      LIMIT 30 OFFSET 1499970
) t
LEFT JOIN article ON t.article_id = article.article_id;

-- 커버링 인덱스를 활용해 서브쿼리로 article_id (프라이머리 키)를 조회하고,
-- 실제 데이터는 조인하여 조회함으로 조회속도가 느려지는 문제를 해결
EXPLAIN SELECT * FROM (
  SELECT article_id FROM article
  WHERE board_id = 1
  ORDER BY article_id DESC
      LIMIT 30 OFFSET 1499970
) t
  LEFT JOIN article ON t.article_id = article.article_id;
