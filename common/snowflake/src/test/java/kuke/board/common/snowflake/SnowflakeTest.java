package kuke.board.common.snowflake;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

public class SnowflakeTest {
    Snowflake snowflake = new Snowflake();

    @Test
    @DisplayName("10개의 스레드풀이 1000번 동안 1000개의 아이디를 만든다.")
    void nextIdTest() throws ExecutionException, InterruptedException {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<Future<List<Long>>> futures = new ArrayList<>();
        int repeatCount = 1000;
        int idCount = 1000;

        // when
        for (int i = 0; i < repeatCount; i++) {
            futures.add(executorService.submit(() -> generateIdList(snowflake, idCount)));
        }

        // then
        List<Long> result = new ArrayList<>();
        for (Future<List<Long>> future : futures) {
            List<Long> idList = future.get();
            for (int i = 1; i < idList.size(); i++) {
                assertThat(idList.get(i)).isGreaterThan(idList.get(i - 1)); // 오름차순 정렬 검증
            }
            result.addAll(idList);
        }
        // 중복없이 100만개가 생성되었는지 검증
        assertThat(result.stream().distinct().count()).isEqualTo(repeatCount * idCount);

        executorService.shutdown();
    }

    @Test
    @DisplayName("10개의 스레드풀이 100만개의 ID를 만드는데 걸리는 시간을 측정한다.")
    void nextIdPerformanceTest() throws InterruptedException {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        int repeatCount = 1000;
        int idCount = 1000;
        CountDownLatch latch = new CountDownLatch(repeatCount);

        // when
        long start = System.nanoTime();
        for (int i = 0; i < repeatCount; i++) {
            executorService.submit(() -> {
                generateIdList(snowflake, idCount);
                latch.countDown();
            });
        }

        latch.await();

        long end = System.nanoTime();
        System.out.println("times = %s ms".formatted((end - start) / 1_000_000));

        executorService.shutdown();
    }

    List<Long> generateIdList(Snowflake snowflake, int count) {
        List<Long> idList = new ArrayList<>();
        while (count-- > 0) {
            idList.add(snowflake.nextId());
        }
        return idList;
    }
}
