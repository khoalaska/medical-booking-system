package fpt.medical.service.impl;
import fpt.medical.entity.Appointment;
import fpt.medical.enums.AppointmentStatus;
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



}
