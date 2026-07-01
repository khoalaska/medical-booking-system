package fpt.medical.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "time_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private WorkSchedule workSchedule;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    @Builder.Default
    private Integer bookedCapacity = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxCapacity = 3;

    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = "AVAILABLE";

    @OneToMany(mappedBy = "timeSlot")
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();

    public boolean isBookable() {
        return "AVAILABLE".equals(status) && bookedCapacity < maxCapacity;
    }

    public int getRemainingCapacity() {
        return Math.max(maxCapacity - bookedCapacity, 0);
    }
}
