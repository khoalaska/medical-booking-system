package fpt.medical.repository;

import fpt.medical.entity.MedicalRecord;
import fpt.medical.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    // Find the medical record of an appointment together with its prescriptions.
    // Returns null when the appointment has not been examined yet.
    @Query("SELECT mr FROM MedicalRecord mr " +
            "LEFT JOIN FETCH mr.prescriptions " +
            "WHERE mr.appointment.id = :appointmentId")
    MedicalRecord findByAppointmentId(@Param("appointmentId") Long appointmentId);

    // Get one page of patients that have at least one medical record at the clinic (any doctor),
    // filtered by patient name. An empty keyword matches every patient.
    // We use EXISTS instead of DISTINCT so each patient appears once AND we can safely sort by name
    // (SQL Server does not allow ordering by a column outside the SELECT list when DISTINCT is used).
    @Query(value = "SELECT p FROM Patient p " +
            "JOIN p.user u " +
            "WHERE EXISTS (SELECT 1 FROM MedicalRecord mr WHERE mr.patient = p) " +
            "AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY u.fullName",
            countQuery = "SELECT COUNT(p) FROM Patient p " +
            "JOIN p.user u " +
            "WHERE EXISTS (SELECT 1 FROM MedicalRecord mr WHERE mr.patient = p) " +
            "AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Patient> findPatientsWithHistory(@Param("keyword") String keyword, Pageable pageable);

    // Get the full examination history of a patient (records from every doctor), newest first,
    // together with their prescriptions, the visit date and the examining doctor.
    @Query("SELECT DISTINCT mr FROM MedicalRecord mr " +
            "LEFT JOIN FETCH mr.prescriptions " +
            "JOIN FETCH mr.appointment a " +
            "JOIN FETCH a.timeSlot ts " +
            "JOIN FETCH ts.workSchedule " +
            "JOIN FETCH mr.doctor doc " +
            "JOIN FETCH doc.user " +
            "WHERE mr.patient.id = :patientId " +
            "ORDER BY mr.createdAt DESC")
    List<MedicalRecord> findHistory(@Param("patientId") Long patientId);
}
