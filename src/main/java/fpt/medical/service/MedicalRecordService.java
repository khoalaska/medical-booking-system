package fpt.medical.service;

import fpt.medical.dto.MedicalRecordDTO;
import fpt.medical.entity.MedicalRecord;
import fpt.medical.entity.Patient;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MedicalRecordService {

    // Get the medical record of an appointment, or null when it has not been examined yet
    MedicalRecord getByAppointmentId(Long appointmentId);

    // Create a new medical record with its prescriptions, then mark the appointment as completed
    void createDiagnosis(Long appointmentId, MedicalRecordDTO medicalRecordForm);

    // Update an existing medical record and replace all of its prescriptions
    void updateDiagnosis(Long appointmentId, MedicalRecordDTO medicalRecordForm);

    // Delete a medical record (and its prescriptions), then reopen the appointment for re-examination
    void deleteDiagnosis(Long appointmentId);

    // Get one page of patients that have any examination history, filtered by a name keyword
    Page<Patient> getPatientsWithHistory(String keyword, int page);

    // Get the full examination history of a patient (records from every doctor), newest first
    List<MedicalRecord> getHistory(Long patientId);

    // Get one patient by id (used for the patient history page header)
    Patient getPatientById(Long patientId);

    // Get the medicine name suggestions shown on the diagnosis form
    // (a list of common medicines plus every medicine already prescribed before)
    List<String> getMedicineSuggestions();
}
