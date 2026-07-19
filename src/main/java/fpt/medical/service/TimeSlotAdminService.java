package fpt.medical.service;

import fpt.medical.entity.TimeSlot;
import java.util.List;

public interface TimeSlotAdminService {
    List<TimeSlot> getByScheduleId(Long scheduleId);
    TimeSlot getById(Long id);
    TimeSlot create(Long scheduleId, TimeSlot timeSlot);
    TimeSlot update(Long id, TimeSlot updated);
    void delete(Long id);
}
