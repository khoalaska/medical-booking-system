package fpt.medical.repository;

import fpt.medical.entity.WorkSchedule;
import fpt.medical.enums.ShiftType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {

    //Yen
    List<WorkSchedule> findByDoctorIdAndAvailableTrueAndPublishedTrueAndWorkDateGreaterThanEqualOrderByWorkDateAsc(
            Long doctorId, LocalDate today);

    List<WorkSchedule> findByDoctorIdAndWorkDateAndAvailableTrueAndPublishedTrueOrderByShiftAsc(
            Long doctorId, LocalDate workDate);

    List<WorkSchedule> findByDoctorIdAndWorkDateAndShiftAndAvailableTrueAndPublishedTrue(
            Long doctorId, LocalDate workDate, ShiftType shift);

    Page<WorkSchedule> findByDoctorId(Long doctorId, Pageable pageable);
    boolean existsByDoctorIdAndWorkDateAndShift(Long doctorId, LocalDate workDate, ShiftType shift);
    boolean existsByDoctorIdAndWorkDateAndShiftAndIdNot(Long doctorId, LocalDate workDate, ShiftType shift, Long id);

    long countByWorkDate(LocalDate workDate);
    Page<WorkSchedule> findByWorkDateGreaterThanEqualOrderByWorkDateAsc(
        LocalDate fromDate, Pageable pageable);

    List<WorkSchedule> findByWorkDateBetween(LocalDate fromDate, LocalDate toDate);
    List<WorkSchedule> findByDoctorIdAndWorkDateBetween(Long doctorId, LocalDate fromDate, LocalDate toDate);
    List<WorkSchedule> findByDoctorIdInAndWorkDateBetween(List<Long> doctorIds, LocalDate fromDate, LocalDate toDate);
    List<WorkSchedule> findByDoctorIdAndWorkDate(Long doctorId, LocalDate workDate);
}
