package fpt.medical.repository;

import fpt.medical.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    // Get the distinct medicine names that have already been prescribed, sorted alphabetically.
    // Used to suggest medicine names on the diagnosis form.
    @Query("SELECT DISTINCT p.medicineName FROM Prescription p ORDER BY p.medicineName")
    List<String> findDistinctMedicineNames();
}
