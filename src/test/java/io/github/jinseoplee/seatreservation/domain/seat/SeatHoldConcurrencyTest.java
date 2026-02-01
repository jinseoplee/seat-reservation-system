package io.github.jinseoplee.seatreservation.domain.seat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SeatHoldConcurrencyTest {

    @Autowired
    SeatRepository seatRepository;

    @Autowired
    SeatHoldService seatHoldService;

    @Test
    @DisplayName("동시에 같은 좌석을 HOLD 요청해도 성공은 1번만 된다")
    void hold_concurrently_only_one_success() throws Exception {
        Seat seat = seatRepository.save(new Seat(1L, "A1"));
        Long seatId = seat.getId();

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        LocalDateTime holdUntil = LocalDateTime.now().plusMinutes(5);

        for (int i = 0; i < threadCount; i++) {
            final String holder = "user-" + i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    seatHoldService.hold(seatId, holder, holdUntil);
                    success.incrementAndGet();
                } catch (Throwable t) {
                    failed.incrementAndGet();
                    errors.add(t);
                } finally {
                    done.countDown();
                }
            });
        }

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();

        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(success.get()).isEqualTo(1);
        assertThat(failed.get()).isEqualTo(threadCount - 1);

        assertThat(errors).allSatisfy(t -> {
            assertThat(t).isInstanceOf(IllegalStateException.class);
            assertThat(t).hasMessageContaining("이미 점유 중인 좌석입니다.");
        });

        Seat reloaded = seatRepository.findById(seatId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SeatStatus.HOLD);
        assertThat(reloaded.getHoldBy()).isNotBlank();
        assertThat(reloaded.getHoldUntil()).isNotNull();
    }
}
