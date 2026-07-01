package fpt.medical.repository;

import fpt.medical.entity.WorkSchedule;
import fpt.medical.enums.ShiftType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {

    //Yen
    List<WorkSchedule> findByDoctorIdAndAvailableTrueAndWorkDateGreaterThanEqualOrderByWorkDateAsc(
            Long doctorId, LocalDate today);

    List<WorkSchedule> findByDoctorIdAndWorkDateAndAvailableTrueOrderByShiftAsc(
            Long doctorId, LocalDate workDate);

    List<WorkSchedule> findByDoctorIdAndWorkDateAndShiftAndAvailableTrue(
            Long doctorId, LocalDate workDate, ShiftType shift);
}
