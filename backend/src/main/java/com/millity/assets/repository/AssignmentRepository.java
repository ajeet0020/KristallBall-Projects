package com.millity.assets.repository;
import com.millity.assets.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
public interface AssignmentRepository extends JpaRepository<Assignment, Long>, JpaSpecificationExecutor<Assignment> {
    @Query("select coalesce(sum(a.quantity), 0) from Assignment a where (:baseId is null or a.base.id = :baseId) and (:equipmentType is null or lower(a.equipmentType) = lower(:equipmentType)) and a.date >= :fromDate")
    Long sumOnOrAfter(@Param("baseId") Long baseId, @Param("equipmentType") String equipmentType, @Param("fromDate") LocalDate fromDate);
    @Query("select coalesce(sum(a.quantity), 0) from Assignment a where (:baseId is null or a.base.id = :baseId) and (:equipmentType is null or lower(a.equipmentType) = lower(:equipmentType)) and a.date between :fromDate and :toDate")
    Long sumBetween(@Param("baseId") Long baseId, @Param("equipmentType") String equipmentType, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
    @Query("select coalesce(sum(a.quantity), 0) from Assignment a where (:baseId is null or a.base.id = :baseId) and (:equipmentType is null or lower(a.equipmentType) = lower(:equipmentType)) and a.expended = true and a.date between :fromDate and :toDate")
    Long sumExpendedBetween(@Param("baseId") Long baseId, @Param("equipmentType") String equipmentType, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
