package fpt.medical.service.impl;

import fpt.medical.entity.TimeSlot;
import fpt.medical.entity.WorkSchedule;
import fpt.medical.enums.ShiftType;
import fpt.medical.exception.ResourceNotFoundException;
import fpt.medical.repository.TimeSlotRepository;
import fpt.medical.repository.WorkScheduleRepository;
import fpt.medical.service.WorkScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkScheduleServiceImpl implements WorkScheduleService {

    private final WorkScheduleRepository workScheduleRepository;
    private final TimeSlotRepository timeSlotRepository;

    //Yen
    @Override
    @Transactional(readOnly = true)
    public List<WorkSchedule> getSchedulesFromToday(Long doctorId) {
        return workScheduleRepository
                .findByDoctorIdAndAvailableTrueAndWorkDateGreaterThanEqualOrderByWorkDateAsc(
                        doctorId, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkSchedule> getSchedulesByDate(Long doctorId, LocalDate workDate) {
        return workScheduleRepository.findByDoctorIdAndWorkDateAndAvailableTrueOrderByShiftAsc(doctorId, workDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkSchedule> getSchedulesByShift(Long doctorId, LocalDate workDate, ShiftType shift) {
        return workScheduleRepository.findByDoctorIdAndWorkDateAndShiftAndAvailableTrue(
                doctorId, workDate, shift);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalDate> getAvailableDates(Long doctorId) {
        List<WorkSchedule> schedules = getSchedulesFromToday(doctorId);
        List<LocalDate> dates = new ArrayList<>();

        for (WorkSchedule schedule : schedules) {
            if (!dates.contains(schedule.getWorkDate())) {
                dates.add(schedule.getWorkDate());
            }
        }

        return dates;
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDate getMinAvailableDate(Long doctorId) {
        List<LocalDate> dates = getAvailableDates(doctorId);
        if (dates.isEmpty()) {
            return null;
        }
        return dates.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDate getMaxAvailableDate(Long doctorId) {
        List<LocalDate> dates = getAvailableDates(doctorId);
        if (dates.isEmpty()) {
            return null;
        }
        return dates.get(dates.size() - 1);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlot> getAvailableTimeSlots(Long doctorId, LocalDate workDate, ShiftType shift) {
        return timeSlotRepository.findByWorkScheduleDoctorIdAndWorkScheduleWorkDateAndWorkScheduleShiftOrderByStartTimeAsc(
                doctorId, workDate, shift);
    }

    @Override
    @Transactional(readOnly = true)
    public TimeSlot getTimeSlotById(Long id) {
        return timeSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TimeSlot", "id", id));
    }
}
