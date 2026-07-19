package fpt.medical.service.impl;

import fpt.medical.entity.TimeSlot;
import fpt.medical.entity.WorkSchedule;
import fpt.medical.exception.ResourceNotFoundException;
import fpt.medical.exception.ScheduleInUseException;
import fpt.medical.repository.TimeSlotRepository;
import fpt.medical.repository.WorkScheduleRepository;
import fpt.medical.service.TimeSlotAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TimeSlotAdminServiceImpl implements TimeSlotAdminService {

    private final TimeSlotRepository timeSlotRepository;
    private final WorkScheduleRepository workScheduleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlot> getByScheduleId(Long scheduleId) {
        return timeSlotRepository.findByWorkScheduleIdOrderByStartTimeAsc(scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public TimeSlot getById(Long id) {
        return timeSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TimeSlot", "id", id));
    }

    @Override
    public TimeSlot create(Long scheduleId, TimeSlot timeSlot) {
        if (!timeSlot.getEndTime().isAfter(timeSlot.getStartTime())) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");
        }

        WorkSchedule schedule = workScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkSchedule", "id", scheduleId));

        LocalTime newStart = timeSlot.getStartTime();
        LocalTime newEnd = timeSlot.getEndTime();

        List<TimeSlot> existingSlots = timeSlotRepository.findByWorkScheduleIdOrderByStartTimeAsc(scheduleId);
        for (TimeSlot existing : existingSlots) {
            if (newStart.isBefore(existing.getEndTime()) && newEnd.isAfter(existing.getStartTime())) {
                throw new IllegalArgumentException("Khung giờ bị trùng với khung giờ đã có: " + existing.getStartTime() + "-" + existing.getEndTime());
            }
        }

        timeSlot.setWorkSchedule(schedule);
        timeSlot.setBookedCapacity(0);
        if (timeSlot.getStatus() == null) {
            timeSlot.setStatus("AVAILABLE");
        }

        return timeSlotRepository.save(timeSlot);
    }

    @Override
    public TimeSlot update(Long id, TimeSlot updated) {
        TimeSlot current = getById(id);

        if (current.getAppointments() != null && !current.getAppointments().isEmpty()) {
            throw new ScheduleInUseException("Không thể sửa khung giờ đã có người đặt lịch.");
        }

        if (!updated.getEndTime().isAfter(updated.getStartTime())) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");
        }

        LocalTime newStart = updated.getStartTime();
        LocalTime newEnd = updated.getEndTime();
        Long scheduleId = current.getWorkSchedule().getId();

        List<TimeSlot> existingSlots = timeSlotRepository.findByWorkScheduleIdOrderByStartTimeAsc(scheduleId);
        for (TimeSlot slot : existingSlots) {
            if (!slot.getId().equals(id)) {
                if (newStart.isBefore(slot.getEndTime()) && newEnd.isAfter(slot.getStartTime())) {
                    throw new IllegalArgumentException("Khung giờ bị trùng với khung giờ đã có: " + slot.getStartTime() + "-" + slot.getEndTime());
                }
            }
        }

        current.setStartTime(newStart);
        current.setEndTime(newEnd);
        current.setMaxCapacity(updated.getMaxCapacity());
        current.setStatus(updated.getStatus());

        return timeSlotRepository.save(current);
    }

    @Override
    public void delete(Long id) {
        TimeSlot current = getById(id);

        if (current.getAppointments() != null && !current.getAppointments().isEmpty()) {
            throw new ScheduleInUseException("Không thể xóa khung giờ đã có người đặt lịch.");
        }

        timeSlotRepository.delete(current);
    }
}
