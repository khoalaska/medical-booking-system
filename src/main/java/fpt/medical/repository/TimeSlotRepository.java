package fpt.medical.repository;

import fpt.medical.entity.TimeSlot;
import fpt.medical.enums.ShiftType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findByWorkScheduleDoctorIdAndWorkScheduleWorkDateAndWorkScheduleShiftOrderByStartTimeAsc(
            Long doctorId, LocalDate workDate, ShiftType shift);
}
