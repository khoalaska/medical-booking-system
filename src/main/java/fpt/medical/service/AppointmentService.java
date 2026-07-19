package fpt.medical.service;

import fpt.medical.entity.Appointment;
import fpt.medical.enums.AppointmentStatus;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {
    //khoa

    // Get today's upcoming (not yet examined) appointments for the dashboard preview
    List<Appointment> getUpcomingTodayAppointments(Long doctorId);

    long countPendingToday(Long doctorId);

    long countCompletedToday(Long doctorId);

    // Get a doctor's appointments on a specific date, optionally filtered by status.
    // When status is null, every appointment of that date is returned (used by Doctor Schedule).
    List<Appointment> getAppointmentsByDateAndStatus(Long doctorId, LocalDate date, AppointmentStatus status);

    // Get one appointment by id together with patient, doctor and time slot details
    Appointment getById(Long appointmentId);
}
