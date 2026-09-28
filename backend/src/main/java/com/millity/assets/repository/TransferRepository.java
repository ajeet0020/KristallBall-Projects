package com.millity.assets.repository;
import com.millity.assets.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
public interface TransferRepository extends JpaRepository<Transfer, Long>, JpaSpecificationExecutor<Transfer> {
    @Query("select coalesce(sum(t.quantity), 0) from Transfer t where (:baseId is null or t.toBase.id = :baseId) and (:equipmentType is null or lower(t.equipmentType) = lower(:equipmentType)) and t.status = com.millity.assets.domain.TransferStatus.COMPLETED and t.date >= :fromDate")
    Long sumInboundOnOrAfter(@Param("baseId") Long baseId, @Param("equipmentType") String equipmentType, @Param("fromDate") LocalDate fromDate);
    @Query("select coalesce(sum(t.quantity), 0) from Transfer t where (:baseId is null or t.fromBase.id = :baseId) and (:equipmentType is null or lower(t.equipmentType) = lower(:equipmentType)) and t.status = com.millity.assets.domain.TransferStatus.COMPLETED and t.date >= :fromDate")
    Long sumOutboundOnOrAfter(@Param("baseId") Long baseId, @Param("equipmentType") String equipmentType, @Param("fromDate") LocalDate fromDate);
    @Query("select coalesce(sum(t.quantity), 0) from Transfer t where (:baseId is null or t.toBase.id = :baseId) and (:equipmentType is null or lower(t.equipmentType) = lower(:equipmentType)) and t.status = com.millity.assets.domain.TransferStatus.COMPLETED and t.date between :fromDate and :toDate")
    Long sumInboundBetween(@Param("baseId") Long baseId, @Param("equipmentType") String equipmentType, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
    @Query("select coalesce(sum(t.quantity), 0) from Transfer t where (:baseId is null or t.fromBase.id = :baseId) and (:equipmentType is null or lower(t.equipmentType) = lower(:equipmentType)) and t.status = com.millity.assets.domain.TransferStatus.COMPLETED and t.date between :fromDate and :toDate")
    Long sumOutboundBetween(@Param("baseId") Long baseId, @Param("equipmentType") String equipmentType, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
