package vn.rms.property;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    List<Room> findAllByProperty_IdAndOrganization_IdOrderByRoomCode(UUID propertyId, UUID organizationId);
    Optional<Room> findByProperty_IdAndIdAndOrganization_Id(UUID propertyId, UUID roomId, UUID organizationId);
    Optional<Room> findByProperty_Organization_IdAndId(UUID organizationId, UUID roomId);
    boolean existsByProperty_IdAndRoomCodeIgnoreCase(UUID propertyId, String roomCode);
}
