package fpt.medical.service.impl;
import fpt.medical.entity.Appointment;
import fpt.medical.enums.AppointmentStatus;
import fpt.medical.exception.ResourceNotFoundException;
import fpt.medical.repository.AppointmentRepository;
import fpt.medical.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentServiceImpl implements AppointmentService {


    private final AppointmentRepository appointmentRepository;
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
