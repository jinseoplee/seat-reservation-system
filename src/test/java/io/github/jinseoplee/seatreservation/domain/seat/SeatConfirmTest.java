package io.github.jinseoplee.seatreservation.domain.seat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class SeatConfirmTest {

    @Autowired
    SeatRepository seatRepository;

    @Autowired
    SeatHoldService seatHoldService;

    @Autowired
    SeatConfirmService seatConfirmService;

    @Test
    @DisplayName("유효한 HOLD 상태의 좌석은 CONFIRM 할 수 있다")
    void confirm_success_when_hold_is_valid() {
        Seat seat = seatRepository.save(new Seat(1L, "A1"));
        Long seatId = seat.getId();
        String user = "user-1";

        LocalDateTime holdUntil = LocalDateTime.now().plusMinutes(5);
        seatHoldService.hold(seatId, user, holdUntil);

        seatConfirmService.confirm(seatId, user);

        Seat result = seatRepository.findById(seatId).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(SeatStatus.CONFIRMED);
        assertThat(result.getHoldBy()).isNull();
        assertThat(result.getHoldUntil()).isNull();
    }

    @Test
    @DisplayName("만료된 HOLD 상태의 좌석은 CONFIRM 할 수 없다")
    void confirm_fail_when_hold_is_expired() {
        Seat seat = seatRepository.save(new Seat(1L, "A2"));
        Long seatId = seat.getId();
        String user = "user-1";

        LocalDateTime expiredAt = LocalDateTime.now().minusMinutes(1);
        seatHoldService.hold(seatId, user, expiredAt);

        assertThatThrownBy(() -> seatConfirmService.confirm(seatId, user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("만료");

        Seat result = seatRepository.findById(seatId).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(SeatStatus.HOLD);
        assertThat(result.getHoldUntil()).isEqualToIgnoringNanos(expiredAt);
    }
}