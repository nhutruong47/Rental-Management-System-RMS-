package vn.rms.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.rms.identity.OrganizationRepository;

@Service
@Transactional
public class AuditService {
    private final AuditLogRepository logs;
    private final OrganizationRepository organizations;

    public AuditService(AuditLogRepository logs, OrganizationRepository organizations) {
        this.logs = logs;
        this.organizations = organizations;
    }

    public void log(UUID organizationId, UUID actorUserId, String actorEmail, String action,
                    String targetType, String targetId, String detail) {
        logs.save(new AuditLog(organizations.getReferenceById(organizationId),
                actorUserId, actorEmail, action, targetType, targetId, detail));
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> list(UUID organizationId) {
        return logs.findTop50ByOrganization_IdOrderByCreatedAtDesc(organizationId)
                .stream().map(AuditLogResponse::from).toList();
    }

    public record AuditLogResponse(UUID id, UUID actorUserId, String actorEmail, String action,
                                   String targetType, String targetId, String detail, Instant createdAt) {
        public static AuditLogResponse from(AuditLog entry) {
            return new AuditLogResponse(entry.getId(), entry.getActorUserId(), entry.getActorEmail(),
                    entry.getAction(), entry.getTargetType(), entry.getTargetId(), entry.getDetail(),
                    entry.getCreatedAt());
        }
    }
}
