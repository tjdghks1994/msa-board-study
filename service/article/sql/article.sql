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

-- 게시글의 데이터가 많아질수록 쿼리 실행 속도가 느려지는 문제가 발생한다
SELECT COUNT(*) FROM article;
EXPLAIN SELECT COUNT(*) FROM article where board_id = 1;

-- 게시글의 페이지를 화면에 보여주기 위해서는 게시글의 전체 데이터 갯수가 필요한 것은 아니다
-- 현재 이용중인 페이지 기준에서 다음 페이지가 존재하는지의 여부만 확인할 수 있으면 된다
-- 즉, 게시글 개수의 일부만 확인하면 된다

-- 다음 페이지가 존재하는지의 게시글 개수를 확인하는 공식은 아래와 같다
-- 공식 = ( ((n-1) / k) +1 ) * m * k + 1     -> ((n-1) / k) 의 나머지는 버림
-- n : 현재 페이지, m : 페이지 당 게시글 개수, k : 이동 가능한 페이지 개수

-- COUNT 쿼리에서는 limit 이 동작하지 않기 때문에, 서브 쿼리에서 커버링 인덱스로 LIMIT 만큼만 조회하고 COUNT 하는 방식을 활용
SELECT COUNT(*)
FROM (
    SELECT article_id FROM article WHERE board_id = 1 LIMIT 300,301  -- 사용자가 10,001 ~ 10,010번 페이지에 있다고 가정
) t;

EXPLAIN SELECT COUNT(*)
        FROM (
            SELECT article_id FROM article WHERE board_id = 1 LIMIT 300,301
        ) t;

create table board_article_count (
    board_id bigint not null primary key,
    article_count bigint not null
);