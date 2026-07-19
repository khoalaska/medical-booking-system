package fpt.medical.service;

import fpt.medical.entity.WorkSchedule;
import fpt.medical.entity.TimeSlot;
import fpt.medical.enums.ShiftType;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface WorkScheduleService {
    //Yen
    List<WorkSchedule> getSchedulesFromToday(Long doctorId);
    List<WorkSchedule> getSchedulesByDate(Long doctorId, LocalDate workDate);
    List<WorkSchedule> getSchedulesByShift(Long doctorId, LocalDate workDate, ShiftType shift);
    List<LocalDate> getAvailableDates(Long doctorId);
    LocalDate getMinAvailableDate(Long doctorId);
    LocalDate getMaxAvailableDate(Long doctorId);
    List<TimeSlot> getAvailableTimeSlots(Long doctorId, LocalDate workDate, ShiftType shift);
    TimeSlot getTimeSlotById(Long id);

    Page<WorkSchedule> getSchedules(
        Long doctorId, int page, int size, String sortBy, String sortDir);
    WorkSchedule getScheduleById(Long id);
    WorkSchedule createSchedule(WorkSchedule schedule);
    WorkSchedule updateSchedule(Long id, WorkSchedule updated);
    void deleteSchedule(Long id);
}
