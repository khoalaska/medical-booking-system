package fpt.medical.repository;
import fpt.medical.entity.Appointment;
import fpt.medical.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

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



    @Query("SELECT COUNT(a) FROM Appointment a " +
            "JOIN a.timeSlot ts " +
            "JOIN ts.workSchedule ws " +
            "WHERE a.doctor.id = :doctorId " +
            "AND ws.workDate = :date")
    long countByDoctorIdAndDate(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date
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
}
