package fpt.medical.service.impl;

import fpt.medical.entity.TimeSlot;
import fpt.medical.entity.WorkSchedule;
import fpt.medical.enums.ShiftType;
import fpt.medical.exception.DuplicateRecordException;
import fpt.medical.exception.ResourceNotFoundException;
import fpt.medical.exception.ScheduleInUseException;
import fpt.medical.repository.TimeSlotRepository;
import fpt.medical.repository.WorkScheduleRepository;
import fpt.medical.service.WorkScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkScheduleServiceImpl implements WorkScheduleService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "workDate");

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

    @Override
    @Transactional(readOnly = true)
    public Page<WorkSchedule> getSchedules(Long doctorId, int page, int size, String sortBy, String sortDir) {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 15) {
            size = 15;
        }
        if (sortBy == null || sortBy.trim().isEmpty() || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "workDate";
        }

        Sort sort = sortDir != null && sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        if (doctorId != null) {
            return workScheduleRepository.findByDoctorId(doctorId, pageable);
        } else {
            return workScheduleRepository.findAll(pageable);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public WorkSchedule getScheduleById(Long id) {
        return workScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkSchedule", "id", id));
    }

    @Override
    public WorkSchedule createSchedule(WorkSchedule schedule) {
        if (schedule.getDoctor() == null || schedule.getDoctor().getId() == null) {
            throw new IllegalArgumentException("Bác sĩ là bắt buộc khi tạo lịch làm việc");
        }

        Long doctorId = schedule.getDoctor().getId();
        LocalDate workDate = schedule.getWorkDate();
        ShiftType shift = schedule.getShift();

        if (workScheduleRepository.existsByDoctorIdAndWorkDateAndShift(doctorId, workDate, shift)) {
            throw new DuplicateRecordException("WorkSchedule", "doctor+date+shift", doctorId + "/" + workDate + "/" + shift);
        }

        return workScheduleRepository.save(schedule);
    }

    @Override
    public WorkSchedule updateSchedule(Long id, WorkSchedule updated) {
        WorkSchedule schedule = getScheduleById(id);

        Long doctorId = schedule.getDoctor().getId();
        LocalDate newDate = updated.getWorkDate();
        ShiftType newShift = updated.getShift();

        if (!schedule.getWorkDate().equals(newDate) || schedule.getShift() != newShift) {
            if (workScheduleRepository.existsByDoctorIdAndWorkDateAndShiftAndIdNot(doctorId, newDate, newShift, id)) {
                throw new DuplicateRecordException("WorkSchedule", "doctor+date+shift", doctorId + "/" + newDate + "/" + newShift);
            }
        }

        schedule.setWorkDate(newDate);
        schedule.setShift(newShift);
        schedule.setAvailable(updated.isAvailable());

        // KHÔNG cho phép đổi bác sĩ của 1 schedule đã tồn tại vì đổi bác sĩ của lịch đã có TimeSlot/Appointment là nguy hiểm
        return workScheduleRepository.save(schedule);
    }

    @Override
    public void deleteSchedule(Long id) {
        WorkSchedule schedule = getScheduleById(id);

        for (TimeSlot timeSlot : schedule.getTimeSlots()) {
            if (timeSlot.getAppointments() != null && !timeSlot.getAppointments().isEmpty()) {
                throw new ScheduleInUseException("Không thể xóa lịch làm việc vì có khung giờ đã được đặt lịch hẹn.");
            }
        }

        // Xóa hết các khung giờ con trước khi xóa lịch làm việc
        timeSlotRepository.deleteAll(schedule.getTimeSlots());
        workScheduleRepository.delete(schedule);
    }
}
