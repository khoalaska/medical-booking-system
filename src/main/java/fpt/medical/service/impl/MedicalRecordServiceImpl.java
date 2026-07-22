package fpt.medical.service.impl;

import fpt.medical.dto.MedicalRecordDTO;
import fpt.medical.dto.PrescriptionDTO;
import fpt.medical.entity.Appointment;
import fpt.medical.entity.MedicalRecord;
import fpt.medical.entity.Patient;
import fpt.medical.entity.Prescription;
import fpt.medical.enums.AppointmentStatus;
import fpt.medical.exception.DuplicateRecordException;
import fpt.medical.exception.ResourceNotFoundException;
import fpt.medical.repository.AppointmentRepository;
import fpt.medical.repository.MedicalRecordRepository;
import fpt.medical.repository.PatientRepository;
import fpt.medical.repository.PrescriptionRepository;
import fpt.medical.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
@Transactional
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;

    // A fixed list of common medicines that are always suggested on the diagnosis form
    private static final List<String> COMMON_MEDICINES = List.of(
            "Paracetamol 500mg",
            "Efferalgan 500mg",
            "Ibuprofen 400mg",
            "Aspirin 81mg",
            "Amoxicillin 500mg",
            "Cephalexin 500mg",
            "Azithromycin 250mg",
            "Loratadin 10mg",
            "Cetirizin 10mg",
            "Omeprazol 20mg",
            "Vitamin C 1000mg",
            "Vitamin B Complex",
            "Oresol (bù nước điện giải)",
            "Berberin",
            "Smecta (trị tiêu chảy)",
            "Men tiêu hóa",
            "Dextromethorphan (thuốc ho)",
            "Salbutamol",
            "Amlodipin 5mg",
            "Metformin 500mg"
    );

    // Return the medical record of an appointment, or null when it does not exist yet
    @Override
    @Transactional(readOnly = true)
    public MedicalRecord getByAppointmentId(Long appointmentId) {
        return medicalRecordRepository.findByAppointmentId(appointmentId);
    }

    // Create a new medical record with its prescriptions, then mark the appointment as completed
    @Override
    public void createDiagnosis(Long appointmentId, MedicalRecordDTO medicalRecordForm) {

        // Load the appointment with its patient and doctor
        Appointment appointment = appointmentRepository.findByIdWithDetails(appointmentId);

        // Stop when the appointment does not exist
        if (appointment == null) {
            throw new ResourceNotFoundException("Appointment", "id", appointmentId);
        }

        // Stop when this appointment already has a medical record (one record per appointment)
        MedicalRecord existingRecord = medicalRecordRepository.findByAppointmentId(appointmentId);
        if (existingRecord != null) {
            throw new DuplicateRecordException("This appointment has already been examined.");
        }

        // Build the medical record from the form data
        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setAppointment(appointment);
        medicalRecord.setDoctor(appointment.getDoctor());
        medicalRecord.setPatient(appointment.getPatient());
        medicalRecord.setDiagnosis(medicalRecordForm.getDiagnosis());
        medicalRecord.setTreatment(medicalRecordForm.getTreatment());
        medicalRecord.setNotes(medicalRecordForm.getNotes());
        medicalRecord.setCreatedAt(LocalDateTime.now());

        // Build the prescription list from the form (blank rows are skipped)
        medicalRecord.setPrescriptions(buildPrescriptionsFromForm(medicalRecord, medicalRecordForm));

        // Save the record; the prescriptions are saved together because of cascade = ALL
        medicalRecordRepository.save(medicalRecord);

        // Mark the appointment as completed after the diagnosis is saved
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);
    }

    // Update an existing medical record and replace all of its prescriptions
    @Override
    public void updateDiagnosis(Long appointmentId, MedicalRecordDTO medicalRecordForm) {

        // Load the existing record; it must already exist to be updated
        MedicalRecord medicalRecord = medicalRecordRepository.findByAppointmentId(appointmentId);
        if (medicalRecord == null) {
            throw new ResourceNotFoundException("MedicalRecord", "appointmentId", appointmentId);
        }

        // Update the diagnosis fields
        medicalRecord.setDiagnosis(medicalRecordForm.getDiagnosis());
        medicalRecord.setTreatment(medicalRecordForm.getTreatment());
        medicalRecord.setNotes(medicalRecordForm.getNotes());

        // Replace all prescriptions with the ones from the form.
        // We clear the existing list (orphanRemoval deletes the old rows) and add the new ones.
        medicalRecord.getPrescriptions().clear();
        medicalRecord.getPrescriptions().addAll(buildPrescriptionsFromForm(medicalRecord, medicalRecordForm));

        medicalRecordRepository.save(medicalRecord);
    }

    // Delete a medical record (and its prescriptions), then reopen the appointment for re-examination
    @Override
    public void deleteDiagnosis(Long appointmentId) {

        // Load the existing record; it must already exist to be deleted
        MedicalRecord medicalRecord = medicalRecordRepository.findByAppointmentId(appointmentId);
        if (medicalRecord == null) {
            throw new ResourceNotFoundException("MedicalRecord", "appointmentId", appointmentId);
        }

        // Delete the record; its prescriptions are removed too because of cascade = ALL
        medicalRecordRepository.delete(medicalRecord);

        // Reopen the appointment (back to CONFIRMED) so the doctor can examine it again
        Appointment appointment = appointmentRepository.findByIdWithDetails(appointmentId);
        if (appointment != null) {
            appointment.setStatus(AppointmentStatus.CONFIRMED);
            appointmentRepository.save(appointment);
        }
    }

    // Build the prescription entities from the form rows, keeping only the rows the doctor filled in.
    // The given medical record is set as the owner of each prescription.
    private List<Prescription> buildPrescriptionsFromForm(MedicalRecord medicalRecord, MedicalRecordDTO medicalRecordForm) {

        List<Prescription> result = new ArrayList<>();
        for (PrescriptionDTO prescriptionRow : medicalRecordForm.getPrescriptions()) {

            // Skip a row that has no medicine name
            if (isBlank(prescriptionRow.getMedicineName())) {
                continue;
            }

            // Skip a row that has no dosage, because dosage is required in the database
            if (isBlank(prescriptionRow.getDosage())) {
                continue;
            }

            // Create one prescription from this row
            Prescription prescription = new Prescription();
            prescription.setMedicalRecord(medicalRecord);
            prescription.setMedicineName(prescriptionRow.getMedicineName().trim());
            prescription.setDosage(prescriptionRow.getDosage().trim());
            prescription.setInstructions(prescriptionRow.getInstructions());

            // Keep duration only when it is a positive number, otherwise store null
            if (prescriptionRow.getDurationDays() != null && prescriptionRow.getDurationDays() > 0) {
                prescription.setDurationDays(prescriptionRow.getDurationDays());
            } else {
                prescription.setDurationDays(null);
            }

            result.add(prescription);
        }
        return result;
    }

    // Return one page of patients that have any examination history, filtered by a name keyword
    @Override
    @Transactional(readOnly = true)
    public Page<Patient> getPatientsWithHistory(String keyword, int page) {

        // Treat a missing keyword as an empty text so the query matches every patient
        String safeKeyword = keyword;
        if (safeKeyword == null) {
            safeKeyword = "";
        }

        // Show 15 patients per page (the sort order is defined inside the query)
        Pageable pageable = PageRequest.of(page, 15);
        return medicalRecordRepository.findPatientsWithHistory(safeKeyword.trim(), pageable);
    }

    // Return the full examination history of a patient (records from every doctor), newest first
    @Override
    @Transactional(readOnly = true)
    public List<MedicalRecord> getHistory(Long patientId) {
        return medicalRecordRepository.findHistory(patientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalRecord> getPatientHistory(Long userId) {
        return medicalRecordRepository.findPatientHistory(userId);
    }

    // Return one patient by id, or report a clear "not found" error
    @Override
    @Transactional(readOnly = true)
    public Patient getPatientById(Long patientId) {

        // Load the patient using the built-in findById, then check it exists
        Patient patient = patientRepository.findById(patientId).orElse(null);
        if (patient == null) {
            throw new ResourceNotFoundException("Patient", "id", patientId);
        }
        return patient;
    }

    // Return the medicine suggestions: common medicines plus every medicine prescribed before
    @Override
    @Transactional(readOnly = true)
    public List<String> getMedicineSuggestions() {

        // Use a TreeSet so the names are unique and sorted alphabetically
        Set<String> names = new TreeSet<>();

        // Always include the common medicines
        names.addAll(COMMON_MEDICINES);

        // Also include every medicine that has already been prescribed before
        names.addAll(prescriptionRepository.findDistinctMedicineNames());

        return new ArrayList<>(names);
    }

    // Small helper: check whether a text value is null or only spaces
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
