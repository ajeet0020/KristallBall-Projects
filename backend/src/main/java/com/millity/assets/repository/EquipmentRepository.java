package com.millity.assets.repository;
import com.millity.assets.domain.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Equipment> findFirstByBase_IdAndTypeIgnoreCase(Long baseId, String type);
    List<Equipment> findByBase_IdOrderByTypeAscNameAsc(Long baseId);
    @Query("select coalesce(sum(e.quantity), 0) from Equipment e where (:baseId is null or e.base.id = :baseId) and (:equipmentType is null or lower(e.type) = lower(:equipmentType))")
    Long sumCurrentQuantity(@Param("baseId") Long baseId, @Param("equipmentType") String equipmentType);
}
