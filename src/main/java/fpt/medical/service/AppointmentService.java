package fpt.medical.service;

import fpt.medical.entity.Appointment;
import fpt.medical.enums.AppointmentStatus;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    Appointment bookAppointment(Long userId, Long timeSlotId, String notes);

    List<Appointment> getPatientAppointments(Long userId);

    void cancelPatientAppointment(Long userId, Long appointmentId);

    // Get today's upcoming (not yet examined) appointments for the dashboard preview
    List<Appointment> getUpcomingTodayAppointments(Long doctorId);

    // Count today's patients waiting to be examined (PENDING + CONFIRMED)
    long countPendingToday(Long doctorId);

    // Count today's patients that have already been examined (COMPLETED)
    long countCompletedToday(Long doctorId);

    // Get a doctor's appointments on a specific date, optionally filtered by status.
    // When status is null, every appointment of that date is returned (used by Doctor Schedule).
    List<Appointment> getAppointmentsByDateAndStatus(Long doctorId, LocalDate date, AppointmentStatus status);

    // Get one appointment by id together with patient, doctor and time slot details
    Appointment getById(Long appointmentId);

    // Confirm a pending appointment (PENDING -> CONFIRMED) so it becomes ready to examine
    void confirmAppointment(Long appointmentId);
}
