package fpt.medical.repository;

import fpt.medical.entity.TimeSlot;
import fpt.medical.enums.ShiftType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findByWorkScheduleDoctorIdAndWorkScheduleWorkDateAndWorkScheduleShiftAndWorkSchedulePublishedTrueOrderByStartTimeAsc(
            Long doctorId, LocalDate workDate, ShiftType shift);

    List<TimeSlot> findByWorkScheduleIdOrderByStartTimeAsc(Long workScheduleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ts FROM TimeSlot ts " +
            "JOIN FETCH ts.workSchedule ws " +
            "JOIN FETCH ws.doctor " +
            "WHERE ts.id = :id")
    TimeSlot findByIdForBooking(@Param("id") Long id);
}
