package vn.rms.maintenance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, UUID> {
    List<MaintenanceTicket> findAllByOrganization_IdOrderByCreatedAtDesc(UUID organizationId);
    List<MaintenanceTicket> findAllByOrganization_IdAndRoom_IdInOrderByCreatedAtDesc(UUID organizationId, java.util.Collection<UUID> roomIds);
    Optional<MaintenanceTicket> findByOrganization_IdAndId(UUID organizationId, UUID id);
}
