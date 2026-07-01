package fpt.medical.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên phòng ban không được để trống")
    @Size(max = 255, message = "Tên phòng ban tối đa 255 ký tự")
    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(length = 500)
    private String imageUrl;

    @OneToMany(mappedBy = "department")
    @Builder.Default
    private List<Doctor> doctors = new ArrayList<>();
}
