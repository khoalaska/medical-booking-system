package fpt.medical.service.impl;
import fpt.medical.entity.Appointment;
import fpt.medical.entity.Patient;
import fpt.medical.entity.TimeSlot;
import fpt.medical.enums.AppointmentStatus;
import fpt.medical.exception.ResourceNotFoundException;
import fpt.medical.repository.AppointmentRepository;
import fpt.medical.repository.PatientRepository;
import fpt.medical.repository.TimeSlotRepository;
import fpt.medical.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentServiceImpl implements AppointmentService {


    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final TimeSlotRepository timeSlotRepository;

    @Override
    public Appointment bookAppointment(Long userId, Long timeSlotId, String notes) {
        Patient patient = patientRepository.findByUserId(userId);
        if (patient == null) {
            throw new IllegalArgumentException("Không tìm thấy hồ sơ bệnh nhân.");
        }

        TimeSlot timeSlot = timeSlotRepository.findByIdForBooking(timeSlotId);
        if (timeSlot == null) {
            throw new ResourceNotFoundException("TimeSlot", "id", timeSlotId);
        }

        if (!timeSlot.getWorkSchedule().isAvailable()
                || timeSlot.getWorkSchedule().getWorkDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Lịch khám này không còn khả dụng.");
        }

        if (!timeSlot.isBookable()) {
            throw new IllegalArgumentException("Khung giờ này đã hết chỗ.");
        }

        boolean alreadyBooked = appointmentRepository
                .existsByPatientIdAndTimeSlotIdAndStatusNot(
                        patient.getId(), timeSlotId, AppointmentStatus.CANCELLED);
        if (alreadyBooked) {
            throw new IllegalArgumentException("Bạn đã đặt khung giờ này rồi.");
        }

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(timeSlot.getWorkSchedule().getDoctor())
                .timeSlot(timeSlot)
                .notes(cleanNotes(notes))
                .status(AppointmentStatus.PENDING)
                .build();

        timeSlot.setBookedCapacity(timeSlot.getBookedCapacity() + 1);
        if (timeSlot.getBookedCapacity() >= timeSlot.getMaxCapacity()) {
            timeSlot.setStatus("FULL");
        }

        timeSlotRepository.save(timeSlot);
        return appointmentRepository.save(appointment);
    }

    private String cleanNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        return notes.trim();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> getPatientAppointments(Long userId) {
        return appointmentRepository.findPatientAppointments(userId);
    }

    @Override
    public void cancelPatientAppointment(Long userId, Long appointmentId) {
        Appointment appointment = appointmentRepository
                .findPatientAppointment(appointmentId, userId);
        if (appointment == null) {
            throw new ResourceNotFoundException("Không tìm thấy lịch khám của bạn.");
        }

        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new IllegalArgumentException("Lịch khám này không thể hủy.");
        }

        TimeSlot timeSlot = timeSlotRepository
                .findByIdForBooking(appointment.getTimeSlot().getId());
        LocalDateTime appointmentTime = LocalDateTime.of(
                timeSlot.getWorkSchedule().getWorkDate(), timeSlot.getStartTime());
        if (!appointmentTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Không thể hủy lịch đã bắt đầu hoặc đã qua.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        timeSlot.setBookedCapacity(Math.max(timeSlot.getBookedCapacity() - 1, 0));
        if (timeSlot.getBookedCapacity() < timeSlot.getMaxCapacity()) {
            timeSlot.setStatus("AVAILABLE");
        }

        timeSlotRepository.save(timeSlot);
        appointmentRepository.save(appointment);
    }

    // Return today's upcoming (not yet examined) appointments: status PENDING or CONFIRMED
    @Override
    @Transactional(readOnly = true)
    public List<Appointment> getUpcomingTodayAppointments(Long doctorId) {

        // "Upcoming" means the patient has not been examined yet
        List<AppointmentStatus> upcomingStatuses = List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);
        return appointmentRepository.findByDoctorIdAndDateAndStatusIn(doctorId, LocalDate.now(), upcomingStatuses);
    }

    // Count today's patients waiting to be examined: PENDING plus CONFIRMED appointments
    @Override
    @Transactional(readOnly = true)
    public long countPendingToday(Long doctorId) {

        long pending = appointmentRepository.countByDoctorIdAndDateAndStatus(
                doctorId, LocalDate.now(), AppointmentStatus.PENDING);
        long confirmed = appointmentRepository.countByDoctorIdAndDateAndStatus(
                doctorId, LocalDate.now(), AppointmentStatus.CONFIRMED);
        return pending + confirmed;
    }

    // Count today's appointments that have already been examined (COMPLETED)
    @Override
    @Transactional(readOnly = true)
    public long countCompletedToday(Long doctorId) {
        return appointmentRepository.countByDoctorIdAndDateAndStatus(
                doctorId, LocalDate.now(), AppointmentStatus.COMPLETED);
    }

    // Return a doctor's appointments on the given date, optionally filtered by status
    @Override
    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsByDateAndStatus(Long doctorId, LocalDate date, AppointmentStatus status) {

        // When no status is chosen, return every appointment of that day
        if (status == null) {
            return appointmentRepository.findByDoctorIdAndDate(doctorId, date);
        }

        // Otherwise return only the appointments that have the chosen status
        return appointmentRepository.findByDoctorIdAndDateAndStatus(doctorId, date, status);
    }

    // Return one appointment with all details, or throw when it does not exist
    @Override
    @Transactional(readOnly = true)
    public Appointment getById(Long appointmentId) {

        // Load the appointment together with patient, doctor and time slot
        Appointment appointment = appointmentRepository.findByIdWithDetails(appointmentId);

        // When the appointment is not found, report a clear "not found" error
        if (appointment == null) {
            throw new ResourceNotFoundException("Appointment", "id", appointmentId);
        }

        return appointment;
    }

    // Confirm a pending appointment so it becomes ready to be examined
    @Override
    public void confirmAppointment(Long appointmentId) {

        // Load the appointment
        Appointment appointment = appointmentRepository.findByIdWithDetails(appointmentId);
        if (appointment == null) {
            throw new ResourceNotFoundException("Appointment", "id", appointmentId);
        }

        // Only a pending appointment can be confirmed
        if (appointment.getStatus() == AppointmentStatus.PENDING) {
            appointment.setStatus(AppointmentStatus.CONFIRMED);
            appointmentRepository.save(appointment);
        }
    }

}
