package fpt.medical.repository;

import fpt.medical.entity.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {
}
