package fpt.medical.controller;
import fpt.medical.dto.MedicalRecordDTO;
import fpt.medical.dto.PrescriptionDTO;
import fpt.medical.entity.Appointment;
import fpt.medical.entity.Doctor;
import fpt.medical.entity.MedicalRecord;
import fpt.medical.entity.Patient;
import fpt.medical.entity.Prescription;
import fpt.medical.enums.AppointmentStatus;
import fpt.medical.security.CustomUserDetails;
import fpt.medical.service.AppointmentService;
import fpt.medical.service.DoctorService;
import fpt.medical.service.MedicalRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.util.List;
@Controller
@RequestMapping("/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final MedicalRecordService medicalRecordService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails,
                            Model model) {

        // Find the current doctor from the logged-in account
        Doctor doctor = doctorService.getByUserId(userDetails.getUser().getId());
        Long doctorId = doctor.getId();

        // Count waiting patients (PENDING + CONFIRMED) and patients already examined today
        long pendingCount = appointmentService.countPendingToday(doctorId);
        long completedCount = appointmentService.countCompletedToday(doctorId);

        // "Today's appointments" only counts active ones (waiting + examined), NOT cancelled ones
        long todayCount = pendingCount + completedCount;

        // Preview: today's next patients that have not been examined yet
        List<Appointment> upcomingAppointments = appointmentService.getUpcomingTodayAppointments(doctorId);

        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("doctorName", doctor.getUser().getFullName());
        model.addAttribute("todayAppointments", todayCount);
        model.addAttribute("pendingPatients", pendingCount);
        model.addAttribute("completedToday", completedCount);
        model.addAttribute("upcomingAppointments", upcomingAppointments);

        return "doctor/dashboard";
    }

    // Show the daily schedule of the logged-in doctor for a selected date and status (UC06)
    @GetMapping("/schedule")
    public String schedule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "status", required = false) String status,
            Model model) {

        // When the page is opened without a date, default to today
        if (date == null) {
            date = LocalDate.now();
        }

        // Convert the status text from the dropdown into an enum; ignore an invalid value
        AppointmentStatus statusFilter = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusFilter = AppointmentStatus.valueOf(status);
            } catch (IllegalArgumentException exception) {
                statusFilter = null;
            }
        }

        // Find the current doctor from the logged-in account
        Doctor doctor = doctorService.getByUserId(userDetails.getUser().getId());
        Long doctorId = doctor.getId();

        // Load this doctor's appointments on the selected date, optionally filtered by status
        List<Appointment> appointments = appointmentService.getAppointmentsByDateAndStatus(doctorId, date, statusFilter);

        // Push data to the view
        model.addAttribute("activeMenu", "schedule");
        model.addAttribute("selectedDate", date);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("appointments", appointments);
        return "doctor/schedule";
    }

    // Show the diagnosis screen for one appointment (UC07 Record Diagnosis + UC08 Prescribe Medication)
    @GetMapping("/diagnosis/{appointmentId}")
    public String showDiagnosisForm(@PathVariable Long appointmentId, Model model) {

        // Load the appointment with patient and time slot details
        Appointment appointment = appointmentService.getById(appointmentId);

        // Check whether this appointment already has a medical record
        MedicalRecord existingRecord = medicalRecordService.getByAppointmentId(appointmentId);

        // Add the shared data used by the page
        model.addAttribute("activeMenu", "schedule");
        model.addAttribute("appointment", appointment);
        model.addAttribute("existingRecord", existingRecord);

        // When there is no record yet, prepare an empty create form with 5 blank prescription rows
        if (existingRecord == null) {
            MedicalRecordDTO medicalRecordForm = new MedicalRecordDTO();
            medicalRecordForm.setAppointmentId(appointmentId);

            // Create 5 empty rows so the doctor can enter up to 5 medicines
            for (int index = 0; index < 5; index++) {
                medicalRecordForm.getPrescriptions().add(new PrescriptionDTO());
            }
            model.addAttribute("medicalRecordForm", medicalRecordForm);
            model.addAttribute("formMode", "create");
            model.addAttribute("formAction", "/doctor/diagnosis/" + appointmentId);
            model.addAttribute("medicineSuggestions", medicalRecordService.getMedicineSuggestions());
        } else {
            // A record already exists: show it in read-only view with edit/delete options
            model.addAttribute("formMode", "view");
        }

        return "doctor/diagnosis-form";
    }

    // Save the diagnosis and its prescriptions, then mark the appointment as completed
    @PostMapping("/diagnosis/{appointmentId}")
    public String saveDiagnosis(
            @PathVariable Long appointmentId,
            @Valid @ModelAttribute("medicalRecordForm") MedicalRecordDTO medicalRecordForm,
            BindingResult bindingResult,
            Model model) {

        // Load the appointment again so we can redisplay the patient info if needed
        Appointment appointment = appointmentService.getById(appointmentId);

        // When the diagnosis field is invalid, show the create form again with the error message
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "schedule");
            model.addAttribute("appointment", appointment);
            model.addAttribute("existingRecord", null);
            model.addAttribute("formMode", "create");
            model.addAttribute("formAction", "/doctor/diagnosis/" + appointmentId);
            model.addAttribute("medicineSuggestions", medicalRecordService.getMedicineSuggestions());
            return "doctor/diagnosis-form";
        }

        // Make sure the form points to the correct appointment
        medicalRecordForm.setAppointmentId(appointmentId);

        // Create the medical record together with its prescriptions
        medicalRecordService.createDiagnosis(appointmentId, medicalRecordForm);

        // Go back to the schedule of the same day after saving
        LocalDate workDate = appointment.getTimeSlot().getWorkSchedule().getWorkDate();
        return "redirect:/doctor/schedule?date=" + workDate;
    }

    // Show the diagnosis form pre-filled with the saved record so the doctor can edit it
    @GetMapping("/diagnosis/{appointmentId}/edit")
    public String showEditForm(@PathVariable Long appointmentId, Model model) {

        // Load the appointment and the existing record
        Appointment appointment = appointmentService.getById(appointmentId);
        MedicalRecord existingRecord = medicalRecordService.getByAppointmentId(appointmentId);

        // If there is no record to edit, go back to the diagnosis view
        if (existingRecord == null) {
            return "redirect:/doctor/diagnosis/" + appointmentId;
        }

        // Build a form pre-filled with the saved diagnosis data
        MedicalRecordDTO medicalRecordForm = new MedicalRecordDTO();
        medicalRecordForm.setAppointmentId(appointmentId);
        medicalRecordForm.setDiagnosis(existingRecord.getDiagnosis());
        medicalRecordForm.setTreatment(existingRecord.getTreatment());
        medicalRecordForm.setNotes(existingRecord.getNotes());

        // Copy each saved prescription into a form row
        for (Prescription prescription : existingRecord.getPrescriptions()) {
            PrescriptionDTO prescriptionRow = new PrescriptionDTO();
            prescriptionRow.setMedicineName(prescription.getMedicineName());
            prescriptionRow.setDosage(prescription.getDosage());
            prescriptionRow.setDurationDays(prescription.getDurationDays());
            prescriptionRow.setInstructions(prescription.getInstructions());
            medicalRecordForm.getPrescriptions().add(prescriptionRow);
        }

        // Add empty rows so there are at least 5 rows to fill in
        while (medicalRecordForm.getPrescriptions().size() < 5) {
            medicalRecordForm.getPrescriptions().add(new PrescriptionDTO());
        }

        model.addAttribute("activeMenu", "schedule");
        model.addAttribute("appointment", appointment);
        model.addAttribute("medicalRecordForm", medicalRecordForm);
        model.addAttribute("formMode", "edit");
        model.addAttribute("formAction", "/doctor/diagnosis/" + appointmentId + "/edit");
        model.addAttribute("medicineSuggestions", medicalRecordService.getMedicineSuggestions());
        return "doctor/diagnosis-form";
    }

    // Save the edited diagnosis and replace its prescriptions
    @PostMapping("/diagnosis/{appointmentId}/edit")
    public String updateDiagnosis(
            @PathVariable Long appointmentId,
            @Valid @ModelAttribute("medicalRecordForm") MedicalRecordDTO medicalRecordForm,
            BindingResult bindingResult,
            Model model) {

        // Load the appointment so we can redisplay the patient info if needed
        Appointment appointment = appointmentService.getById(appointmentId);

        // When the diagnosis field is invalid, show the edit form again with the error message
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "schedule");
            model.addAttribute("appointment", appointment);
            model.addAttribute("formMode", "edit");
            model.addAttribute("formAction", "/doctor/diagnosis/" + appointmentId + "/edit");
            model.addAttribute("medicineSuggestions", medicalRecordService.getMedicineSuggestions());
            return "doctor/diagnosis-form";
        }

        // Make sure the form points to the correct appointment
        medicalRecordForm.setAppointmentId(appointmentId);

        // Update the record and its prescriptions
        medicalRecordService.updateDiagnosis(appointmentId, medicalRecordForm);

        // Go back to the read-only view after updating
        return "redirect:/doctor/diagnosis/" + appointmentId;
    }

    // Show a confirmation page before actually deleting a medical record
    @GetMapping("/diagnosis/{appointmentId}/delete")
    public String confirmDeleteDiagnosis(@PathVariable Long appointmentId, Model model) {

        // Load the appointment and the record that would be deleted
        Appointment appointment = appointmentService.getById(appointmentId);
        MedicalRecord existingRecord = medicalRecordService.getByAppointmentId(appointmentId);

        // If there is no record to delete, go back to the diagnosis view
        if (existingRecord == null) {
            return "redirect:/doctor/diagnosis/" + appointmentId;
        }

        model.addAttribute("activeMenu", "schedule");
        model.addAttribute("appointment", appointment);
        model.addAttribute("existingRecord", existingRecord);
        return "doctor/diagnosis-delete-confirm";
    }

    // Delete the medical record and reopen the appointment for re-examination
    @PostMapping("/diagnosis/{appointmentId}/delete")
    public String deleteDiagnosis(@PathVariable Long appointmentId) {

        // Read the work date before deleting so we can return to the correct schedule day
        Appointment appointment = appointmentService.getById(appointmentId);
        LocalDate workDate = appointment.getTimeSlot().getWorkSchedule().getWorkDate();

        // Delete the record and reopen the appointment
        medicalRecordService.deleteDiagnosis(appointmentId);

        return "redirect:/doctor/schedule?date=" + workDate;
    }

    // Show the list of patients who have any examination history, with a name search + paging (UC10)
    @GetMapping("/patient-history")
    public String patientHistory(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            Model model) {

        // Load one page of patients that have visit history, filtered by the search keyword
        Page<Patient> patientPage = medicalRecordService.getPatientsWithHistory(keyword, page);

        // Push data to the view
        model.addAttribute("activeMenu", "patient-history");
        model.addAttribute("keyword", keyword);
        model.addAttribute("patientPage", patientPage);
        return "doctor/patient-history";
    }

    // Show the full examination history of one patient (UC10)
    @GetMapping("/patient-history/{patientId}")
    public String patientHistoryDetail(
            @PathVariable Long patientId,
            Model model) {

        // Load the patient (for the header) and the patient's full history from every doctor
        Patient patient = medicalRecordService.getPatientById(patientId);
        List<MedicalRecord> records = medicalRecordService.getHistory(patientId);

        // Push data to the view
        model.addAttribute("activeMenu", "patient-history");
        model.addAttribute("patient", patient);
        model.addAttribute("records", records);
        return "doctor/patient-history-detail";
    }
}
