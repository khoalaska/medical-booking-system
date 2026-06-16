package fpt.medical.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(nullable = false)
    private String specialization;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String bio;

    private Integer experienceYears;

    @Builder.Default
    private Double rating = 0.0;

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL)
    @Builder.Default
    private List<WorkSchedule> workSchedules = new ArrayList<>();

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();
}
