package com.millity.assets.repository;
import com.millity.assets.domain.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
public interface PurchaseRepository extends JpaRepository<Purchase, Long>, JpaSpecificationExecutor<Purchase> {
    @Query("select coalesce(sum(p.quantity), 0) from Purchase p where (:baseId is null or p.base.id = :baseId) and (:equipmentType is null or lower(p.equipmentType) = lower(:equipmentType)) and p.date >= :fromDate")
    Long sumOnOrAfter(@Param("baseId") Long baseId, @Param("equipmentType") String equipmentType, @Param("fromDate") LocalDate fromDate);
    @Query("select coalesce(sum(p.quantity), 0) from Purchase p where (:baseId is null or p.base.id = :baseId) and (:equipmentType is null or lower(p.equipmentType) = lower(:equipmentType)) and p.date between :fromDate and :toDate")
    Long sumBetween(@Param("baseId") Long baseId, @Param("equipmentType") String equipmentType, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
