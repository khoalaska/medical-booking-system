package fpt.medical.service;

import fpt.medical.entity.Appointment;

import java.util.List;

public interface AppointmentService {
    //khoa

    List<Appointment> getTodayAppointments(Long doctorId);

    long countTodayAppointments(Long doctorId);

    long countPendingToday(Long doctorId);

    long countCompletedToday(Long doctorId);
}
