package fpt.medical.repository;

import fpt.medical.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    //Yen:

    @Query("select d from Doctor d join fetch d.user left join fetch d.department where d.department.id = :departmentId order by d.user.fullName")
    List<Doctor> findByDepartmentIdWithUser(@Param("departmentId") Long departmentId);

    @Query("select d from Doctor d join fetch d.user left join fetch d.department where d.id = :id")
    Doctor findByIdWithUser(@Param("id") Long id);
}
