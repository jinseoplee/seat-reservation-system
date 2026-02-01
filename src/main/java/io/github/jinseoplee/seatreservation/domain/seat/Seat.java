package io.github.jinseoplee.seatreservation.domain.seat;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long performanceInstanceId;

    @Column(nullable = false)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    private String holdBy;
    private LocalDateTime holdUntil;

    public Seat(Long performanceInstanceId, String seatNumber) {
        this.performanceInstanceId = performanceInstanceId;
        this.seatNumber = seatNumber;
        this.status = SeatStatus.AVAILABLE;
    }

    public void hold(String holder, LocalDateTime holdUntil, LocalDateTime now) {
        if (isHoldExpired(now)) {
            release();
        }
        if (this.status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException("이미 점유 중인 좌석입니다.");
        }
        this.status = SeatStatus.HOLD;
        this.holdBy = holder;
        this.holdUntil = holdUntil;
    }

    private boolean isHoldExpired(LocalDateTime now) {
        return this.status == SeatStatus.HOLD
                && this.holdUntil != null
                && this.holdUntil.isBefore(now);
    }

    private void release() {
        this.status = SeatStatus.AVAILABLE;
        this.holdBy = null;
        this.holdUntil = null;
    }
}
