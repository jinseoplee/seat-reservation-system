package io.github.jinseoplee.seatreservation.domain.seat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private final SeatRepository seatRepository;

    @Transactional
    public void hold(Long seatId, String holder, LocalDateTime holdUntil) {
        Seat seat = seatRepository.findByIdForUpdate(seatId)
                .orElseThrow(() -> new IllegalArgumentException("좌석이 존재하지 않습니다."));

        seat.hold(holder, holdUntil);
    }
}
