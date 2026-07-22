package fpt.medical.repository;
import fpt.medical.entity.Appointment;
import fpt.medical.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsByPatientIdAndTimeSlotIdAndStatusNot(
            Long patientId, Long timeSlotId, AppointmentStatus status);

    @Query("SELECT a FROM Appointment a " +
            "JOIN FETCH a.doctor d " +
            "JOIN FETCH d.user " +
            "JOIN FETCH d.department " +
            "JOIN FETCH a.timeSlot ts " +
            "JOIN FETCH ts.workSchedule ws " +
            "WHERE a.patient.user.id = :userId " +
            "ORDER BY ws.workDate DESC, ts.startTime DESC")
    List<Appointment> findPatientAppointments(@Param("userId") Long userId);

    @Query("SELECT a FROM Appointment a " +
            "JOIN FETCH a.timeSlot ts " +
            "JOIN FETCH ts.workSchedule " +
            "WHERE a.id = :appointmentId AND a.patient.user.id = :userId")
    Appointment findPatientAppointment(
            @Param("appointmentId") Long appointmentId,
            @Param("userId") Long userId);

    //Khoa
    @Query("SELECT a FROM Appointment a " +
            "JOIN FETCH a.patient p " +
            "JOIN FETCH p.user " +
            "JOIN FETCH a.timeSlot ts " +
            "JOIN FETCH ts.workSchedule ws " +
            "WHERE a.doctor.id = :doctorId " +
            "AND ws.workDate = :date " +
            "ORDER BY ts.startTime")
    List<Appointment> findByDoctorIdAndDate(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date
    );

    // Same as findByDoctorIdAndDate, but also keeps only the appointments with a given status.
    @Query("SELECT a FROM Appointment a " +
            "JOIN FETCH a.patient p " +
            "JOIN FETCH p.user " +
            "JOIN FETCH a.timeSlot ts " +
            "JOIN FETCH ts.workSchedule ws " +
            "WHERE a.doctor.id = :doctorId " +
            "AND ws.workDate = :date " +
            "AND a.status = :status " +
            "ORDER BY ts.startTime")
    List<Appointment> findByDoctorIdAndDateAndStatus(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("status") AppointmentStatus status
    );



    @Query("SELECT COUNT(a) FROM Appointment a " +
            "JOIN a.timeSlot ts " +
            "JOIN ts.workSchedule ws " +
            "WHERE a.doctor.id = :doctorId " +
            "AND ws.workDate = :date " +
            "AND a.status = :status")
    long countByDoctorIdAndDateAndStatus(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("status") AppointmentStatus status
    );

    // Get a doctor's appointments on a date that have one of the given statuses.
    // Used by the dashboard preview to show only the "upcoming" (not yet examined) patients.
    @Query("SELECT a FROM Appointment a " +
            "JOIN FETCH a.patient p " +
            "JOIN FETCH p.user " +
            "JOIN FETCH a.timeSlot ts " +
            "JOIN FETCH ts.workSchedule ws " +
            "WHERE a.doctor.id = :doctorId " +
            "AND ws.workDate = :date " +
            "AND a.status IN :statuses " +
            "ORDER BY ts.startTime")
    List<Appointment> findByDoctorIdAndDateAndStatusIn(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("statuses") Collection<AppointmentStatus> statuses
    );

    // Load one appointment together with the patient, doctor and time slot details.
    // Used by the diagnosis screen so all information is available without lazy-loading errors.
    @Query("SELECT a FROM Appointment a " +
            "JOIN FETCH a.patient p " +
            "JOIN FETCH p.user " +
            "JOIN FETCH a.doctor d " +
            "JOIN FETCH d.user " +
            "JOIN FETCH a.timeSlot ts " +
            "JOIN FETCH ts.workSchedule " +
            "WHERE a.id = :appointmentId")
    Appointment findByIdWithDetails(@Param("appointmentId") Long appointmentId);
}
