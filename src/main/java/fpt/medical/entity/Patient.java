package fpt.medical.entity;

import fpt.medical.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String address;

    @Column(length = 5)
    private String bloodType; // A, B, AB, O

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();
}
