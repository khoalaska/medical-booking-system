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
    @Override
    @Transactional(readOnly = true)
    public List<Appointment> getTodayAppointments(Long doctorId) {

        return appointmentRepository.findByDoctorIdAndDate(doctorId, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long countTodayAppointments(Long doctorId) {
        return appointmentRepository.countByDoctorIdAndDate(doctorId, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingToday(Long doctorId) {

        long pending = appointmentRepository.countByDoctorIdAndDateAndStatus(
                doctorId, LocalDate.now(), AppointmentStatus.PENDING);
        long confirmed = appointmentRepository.countByDoctorIdAndDateAndStatus(
                doctorId, LocalDate.now(), AppointmentStatus.CONFIRMED);
        return pending + confirmed;
    }

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

}
