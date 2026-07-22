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
    private final fpt.medical.repository.DoctorRepository doctorRepository;

    //Yen
    @Override
    @Transactional(readOnly = true)
    public List<WorkSchedule> getSchedulesFromToday(Long doctorId) {
        return workScheduleRepository
                .findByDoctorIdAndAvailableTrueAndPublishedTrueAndWorkDateGreaterThanEqualOrderByWorkDateAsc(
                        doctorId, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkSchedule> getSchedulesByDate(Long doctorId, LocalDate workDate) {
        return workScheduleRepository.findByDoctorIdAndWorkDateAndAvailableTrueAndPublishedTrueOrderByShiftAsc(doctorId, workDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkSchedule> getSchedulesByShift(Long doctorId, LocalDate workDate, ShiftType shift) {
        return workScheduleRepository.findByDoctorIdAndWorkDateAndShiftAndAvailableTrueAndPublishedTrue(
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
        return timeSlotRepository.findByWorkScheduleDoctorIdAndWorkScheduleWorkDateAndWorkScheduleShiftAndWorkSchedulePublishedTrueOrderByStartTimeAsc(
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

    @Override
    public fpt.medical.dto.PublishScheduleResultDTO publishSchedules(
            Long doctorId, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("Khoảng ngày không hợp lệ");
        }
        List<WorkSchedule> targets;
        if (doctorId != null) {
            targets = workScheduleRepository.findByDoctorIdAndWorkDateBetween(doctorId, fromDate, toDate);
        } else {
            targets = workScheduleRepository.findByWorkDateBetween(fromDate, toDate);
        }
        int count = 0;
        for (WorkSchedule ws : targets) {
            if (!ws.isPublished()) {
                ws.setPublished(true);
                count++;
            }
        }
        workScheduleRepository.saveAll(targets);
        return fpt.medical.dto.PublishScheduleResultDTO.builder()
                .publishedCount(count)
                .build();
    }

    @Override
    public fpt.medical.dto.AutoAssignResultDTO autoAssignSchedules(
            Long departmentId, LocalDate fromDate, LocalDate toDate) {

        if (departmentId == null) {
            throw new IllegalArgumentException("Vui lòng chọn khoa");
        }
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("Khoảng ngày không hợp lệ");
        }

        final int MIN_DAYS = 4;
        final int MAX_DAYS = 5;
        final int MAX_CONSECUTIVE_FULL = 2;

        List<fpt.medical.entity.Doctor> doctors = doctorRepository.findByDepartmentIdWithUser(departmentId);
        if (doctors.isEmpty()) {
            return fpt.medical.dto.AutoAssignResultDTO.builder()
                    .createdCount(0)
                    .unfilledWarnings(java.util.List.of("Khoa này chưa có bác sĩ nào."))
                    .build();
        }

        // 1. Danh sách ngày làm việc (bỏ T7, CN)
        List<LocalDate> workDays = new java.util.ArrayList<>();
        LocalDate cursor = fromDate;
        while (!cursor.isAfter(toDate)) {
            java.time.DayOfWeek dow = cursor.getDayOfWeek();
            if (dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY) {
                workDays.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }

        // 2. Chia thành các block tuần: nếu <=7 ngày làm việc thì 1 block,
        //    ngược lại chia theo tuần lịch (Thứ 2 - Chủ nhật)
        List<List<LocalDate>> weekBlocks = new java.util.ArrayList<>();
        if (workDays.size() <= 7) {
            weekBlocks.add(workDays);
        } else {
            java.util.Map<Integer, List<LocalDate>> byWeek = new java.util.LinkedHashMap<>();
            for (LocalDate d : workDays) {
                int weekKey = d.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())
                        + d.get(java.time.temporal.WeekFields.ISO.weekBasedYear()) * 100;
                byWeek.computeIfAbsent(weekKey, k -> new java.util.ArrayList<>()).add(d);
            }
            weekBlocks.addAll(byWeek.values());
        }

        List<Long> doctorIds = doctors.stream().map(fpt.medical.entity.Doctor::getId).collect(java.util.stream.Collectors.toList());
        List<WorkSchedule> existing = workScheduleRepository.findByDoctorIdInAndWorkDateBetween(doctorIds, fromDate, toDate);

        // Map doctorId -> set các ngày đã CÓ lịch (không rỗng) trước khi chạy thuật toán
        java.util.Map<Long, java.util.Set<LocalDate>> occupiedDays = new java.util.HashMap<>();
        // Map "date|shift" -> đã có bác sĩ trực chưa (dựa trên dữ liệu có sẵn)
        java.util.Set<String> coveredSlots = new java.util.HashSet<>();
        for (WorkSchedule ws : existing) {
            occupiedDays.computeIfAbsent(ws.getDoctor().getId(), k -> new java.util.HashSet<>()).add(ws.getWorkDate());
            coveredSlots.add(ws.getWorkDate() + "|" + ws.getShift().name());
        }

        List<WorkSchedule> toCreate = new java.util.ArrayList<>();
        List<String> warnings = new java.util.ArrayList<>();
        java.util.Random random = new java.util.Random();

        for (List<LocalDate> block : weekBlocks) {
            // Ngày trống của từng bác sĩ trong block này (chưa có lịch nào)
            java.util.Map<Long, List<LocalDate>> emptyDaysByDoctor = new java.util.HashMap<>();
            for (fpt.medical.entity.Doctor doc : doctors) {
                List<LocalDate> empty = new java.util.ArrayList<>();
                for (LocalDate d : block) {
                    java.util.Set<LocalDate> occ = occupiedDays.getOrDefault(doc.getId(), java.util.Set.of());
                    if (!occ.contains(d)) {
                        empty.add(d);
                    }
                }
                emptyDaysByDoctor.put(doc.getId(), empty);
            }

            // Kết quả tạm trong block: doctorId -> ngày -> "MORNING"/"AFTERNOON"/"FULL"
            java.util.Map<Long, java.util.Map<LocalDate, String>> assignedInBlock = new java.util.HashMap<>();
            for (fpt.medical.entity.Doctor doc : doctors) {
                assignedInBlock.put(doc.getId(), new java.util.LinkedHashMap<>());
            }

            // Random số ngày mục tiêu (4 hoặc 5) cho mỗi bác sĩ trong block này
            java.util.Map<Long, Integer> targetDaysByDoctor = new java.util.HashMap<>();
            for (fpt.medical.entity.Doctor doc : doctors) {
                int target = MIN_DAYS + random.nextInt(MAX_DAYS - MIN_DAYS + 1);
                targetDaysByDoctor.put(doc.getId(), target);
            }

            // BƯỚC A: đảm bảo mỗi ngày+ca trong block có ít nhất 1 bác sĩ
            for (LocalDate d : block) {
                for (ShiftType shift : ShiftType.values()) {
                    String key = d + "|" + shift.name();
                    if (coveredSlots.contains(key)) continue;

                    // Tìm bác sĩ còn trống ngày d, chưa đạt target, để gán
                    List<fpt.medical.entity.Doctor> candidates = new java.util.ArrayList<>();
                    for (fpt.medical.entity.Doctor doc : doctors) {
                        List<LocalDate> empty = emptyDaysByDoctor.get(doc.getId());
                        java.util.Map<LocalDate, String> assigned = assignedInBlock.get(doc.getId());
                        if (empty.contains(d) && !assigned.containsKey(d)
                                && assigned.size() < targetDaysByDoctor.get(doc.getId())) {
                            candidates.add(doc);
                        }
                    }
                    if (candidates.isEmpty()) {
                        warnings.add("Không đủ bác sĩ trực " + (shift == ShiftType.MORNING ? "ca sáng" : "ca chiều")
                                + " ngày " + d);
                        continue;
                    }
                    fpt.medical.entity.Doctor chosen = candidates.get(random.nextInt(candidates.size()));
                    assignedInBlock.get(chosen.getId()).put(d, shift.name());
                    coveredSlots.add(key);
                }
            }

            // BƯỚC B: lấp ngẫu nhiên cho đủ target mỗi bác sĩ
            for (fpt.medical.entity.Doctor doc : doctors) {
                java.util.Map<LocalDate, String> assigned = assignedInBlock.get(doc.getId());
                List<LocalDate> empty = new java.util.ArrayList<>(emptyDaysByDoctor.get(doc.getId()));
                empty.removeAll(assigned.keySet());
                java.util.Collections.shuffle(empty, random);

                int target = targetDaysByDoctor.get(doc.getId());
                for (LocalDate d : empty) {
                    if (assigned.size() >= target) break;

                    // Kiểm tra rule tối đa 2 ngày liên tiếp "Cả ngày"
                    List<LocalDate> sortedAssignedDays = new java.util.ArrayList<>(assigned.keySet());
                    java.util.Collections.sort(sortedAssignedDays);
                    int consecutiveFull = 0;
                    for (int i = sortedAssignedDays.size() - 1; i >= 0; i--) {
                        LocalDate ad = sortedAssignedDays.get(i);
                        if ("FULL".equals(assigned.get(ad))) {
                            consecutiveFull++;
                        } else {
                            break;
                        }
                        if (consecutiveFull >= MAX_CONSECUTIVE_FULL) break;
                    }

                    String pick;
                    int roll = random.nextInt(3); // 0=MORNING,1=AFTERNOON,2=FULL
                    if (consecutiveFull >= MAX_CONSECUTIVE_FULL) {
                        pick = random.nextBoolean() ? "MORNING" : "AFTERNOON";
                    } else {
                        pick = roll == 0 ? "MORNING" : (roll == 1 ? "AFTERNOON" : "FULL");
                    }
                    assigned.put(d, pick);
                }
            }

            // Chuyển kết quả block thành WorkSchedule entity
            for (fpt.medical.entity.Doctor doc : doctors) {
                for (java.util.Map.Entry<LocalDate, String> e : assignedInBlock.get(doc.getId()).entrySet()) {
                    LocalDate d = e.getKey();
                    String v = e.getValue();
                    if ("FULL".equals(v)) {
                        toCreate.add(WorkSchedule.builder().doctor(doc).workDate(d)
                                .shift(ShiftType.MORNING).available(true).published(false).build());
                        toCreate.add(WorkSchedule.builder().doctor(doc).workDate(d)
                                .shift(ShiftType.AFTERNOON).available(true).published(false).build());
                    } else {
                        toCreate.add(WorkSchedule.builder().doctor(doc).workDate(d)
                                .shift(ShiftType.valueOf(v)).available(true).published(false).build());
                    }
                }
            }
        }

        workScheduleRepository.saveAll(toCreate);

        return fpt.medical.dto.AutoAssignResultDTO.builder()
                .createdCount(toCreate.size())
                .unfilledWarnings(warnings)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public fpt.medical.dto.WeeklyScheduleGridDTO getWeeklyGrid(
            Long departmentId, String keyword, LocalDate weekStart) {

        LocalDate resolvedStart = (weekStart != null ? weekStart : LocalDate.now())
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate resolvedEnd = resolvedStart.plusDays(6);

        List<fpt.medical.entity.Doctor> doctors;
        if (departmentId != null) {
            doctors = doctorRepository.findByDepartmentIdWithUser(departmentId);
        } else {
            doctors = doctorRepository.findAllWithUser();
        }

        if (keyword != null && !keyword.trim().isBlank()) {
            String kw = keyword.trim().toLowerCase();
            doctors = doctors.stream()
                    .filter(d -> d.getUser().getFullName().toLowerCase().contains(kw))
                    .collect(java.util.stream.Collectors.toList());
        }

        List<LocalDate> days = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) {
            days.add(resolvedStart.plusDays(i));
        }

        if (doctors.isEmpty()) {
            return fpt.medical.dto.WeeklyScheduleGridDTO.builder()
                    .weekStart(resolvedStart)
                    .weekEnd(resolvedEnd)
                    .days(days)
                    .rows(java.util.List.of())
                    .build();
        }

        List<Long> doctorIds = doctors.stream()
                .map(fpt.medical.entity.Doctor::getId)
                .collect(java.util.stream.Collectors.toList());

        List<WorkSchedule> schedules = workScheduleRepository
                .findByDoctorIdInAndWorkDateBetween(doctorIds, resolvedStart, resolvedEnd);

        java.util.Map<Long, java.util.Map<LocalDate, List<WorkSchedule>>> grouped = new java.util.HashMap<>();
        for (WorkSchedule ws : schedules) {
            grouped
                .computeIfAbsent(ws.getDoctor().getId(), k -> new java.util.HashMap<>())
                .computeIfAbsent(ws.getWorkDate(), k -> new java.util.ArrayList<>())
                .add(ws);
        }

        List<fpt.medical.dto.DoctorWeekRowDTO> rows = new java.util.ArrayList<>();
        for (fpt.medical.entity.Doctor doc : doctors) {
            java.util.Map<LocalDate, List<WorkSchedule>> byDate =
                    grouped.getOrDefault(doc.getId(), new java.util.HashMap<>());
            rows.add(fpt.medical.dto.DoctorWeekRowDTO.builder()
                    .doctor(doc)
                    .schedulesByDate(byDate)
                    .build());
        }

        return fpt.medical.dto.WeeklyScheduleGridDTO.builder()
                .weekStart(resolvedStart)
                .weekEnd(resolvedEnd)
                .days(days)
                .rows(rows)
                .build();
    }

    @Override
    public fpt.medical.dto.MoveScheduleResultDTO moveOrSwapSchedules(
            fpt.medical.dto.MoveScheduleRequestDTO req) {

        if (req.getSourceDoctorId() == null || req.getSourceDate() == null
                || req.getTargetDoctorId() == null || req.getTargetDate() == null) {
            return fpt.medical.dto.MoveScheduleResultDTO.builder()
                    .success(false).message("Thiếu thông tin di chuyển.").build();
        }
        if (req.getSourceDoctorId().equals(req.getTargetDoctorId())
                && req.getSourceDate().equals(req.getTargetDate())) {
            return fpt.medical.dto.MoveScheduleResultDTO.builder()
                    .success(false).message("Vị trí nguồn và đích giống nhau.").build();
        }

        List<WorkSchedule> sourceList = workScheduleRepository
                .findByDoctorIdAndWorkDate(req.getSourceDoctorId(), req.getSourceDate());
        List<WorkSchedule> targetList = workScheduleRepository
                .findByDoctorIdAndWorkDate(req.getTargetDoctorId(), req.getTargetDate());

        if (sourceList.isEmpty()) {
            return fpt.medical.dto.MoveScheduleResultDTO.builder()
                    .success(false).message("Ô nguồn không có lịch làm việc để di chuyển.").build();
        }

        for (WorkSchedule ws : sourceList) {
            for (TimeSlot ts : ws.getTimeSlots()) {
                if (ts.getAppointments() != null && !ts.getAppointments().isEmpty()) {
                    return fpt.medical.dto.MoveScheduleResultDTO.builder().success(false)
                            .message("Không thể di chuyển vì đã có bệnh nhân đặt lịch ở ô nguồn.").build();
                }
            }
        }
        for (WorkSchedule ws : targetList) {
            for (TimeSlot ts : ws.getTimeSlots()) {
                if (ts.getAppointments() != null && !ts.getAppointments().isEmpty()) {
                    return fpt.medical.dto.MoveScheduleResultDTO.builder().success(false)
                            .message("Không thể di chuyển vì ô đích đã có bệnh nhân đặt lịch.").build();
                }
            }
        }

        fpt.medical.entity.Doctor sourceDoctor = doctorRepository.findById(req.getSourceDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", req.getSourceDoctorId()));
        fpt.medical.entity.Doctor targetDoctor = doctorRepository.findById(req.getTargetDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", req.getTargetDoctorId()));

        // Di chuyển nội dung ô đích về tọa độ ô nguồn TRƯỚC (giải phóng ô đích)
        for (WorkSchedule ws : targetList) {
            ws.setDoctor(sourceDoctor);
            ws.setWorkDate(req.getSourceDate());
        }
        workScheduleRepository.saveAll(targetList);

        // Sau đó di chuyển ô nguồn sang tọa độ đích (đã trống)
        for (WorkSchedule ws : sourceList) {
            ws.setDoctor(targetDoctor);
            ws.setWorkDate(req.getTargetDate());
        }
        workScheduleRepository.saveAll(sourceList);

        String msg = targetList.isEmpty()
                ? "Đã di chuyển lịch làm việc thành công."
                : "Đã hoán đổi lịch làm việc giữa hai ô thành công.";

        return fpt.medical.dto.MoveScheduleResultDTO.builder().success(true).message(msg).build();
    }

    @Override
    public List<WorkSchedule> createFullDaySchedule(WorkSchedule template) {
        if (template.getDoctor() == null || template.getDoctor().getId() == null) {
            throw new IllegalArgumentException("Bác sĩ là bắt buộc khi tạo lịch làm việc");
        }
        Long doctorId = template.getDoctor().getId();
        LocalDate workDate = template.getWorkDate();

        if (workScheduleRepository.existsByDoctorIdAndWorkDateAndShift(doctorId, workDate, ShiftType.MORNING)) {
            throw new DuplicateRecordException("WorkSchedule", "doctor+date+shift",
                    doctorId + "/" + workDate + "/MORNING");
        }
        if (workScheduleRepository.existsByDoctorIdAndWorkDateAndShift(doctorId, workDate, ShiftType.AFTERNOON)) {
            throw new DuplicateRecordException("WorkSchedule", "doctor+date+shift",
                    doctorId + "/" + workDate + "/AFTERNOON");
        }

        WorkSchedule morning = WorkSchedule.builder()
                .doctor(template.getDoctor()).workDate(workDate)
                .shift(ShiftType.MORNING).available(template.isAvailable()).published(false).build();
        WorkSchedule afternoon = WorkSchedule.builder()
                .doctor(template.getDoctor()).workDate(workDate)
                .shift(ShiftType.AFTERNOON).available(template.isAvailable()).published(false).build();

        return workScheduleRepository.saveAll(List.of(morning, afternoon));
    }
}
