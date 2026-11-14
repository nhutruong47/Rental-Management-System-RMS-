package vn.rms.tenant;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TenantService {
    private final TenantRepository tenants;

    public TenantService(TenantRepository tenants) {
        this.tenants = tenants;
    }

    @Transactional(readOnly = true)
    public List<Tenant> list(UUID organizationId) {
        return tenants.findAllByOrganizationIdOrderByFullName(organizationId);
    }

    public Tenant create(UUID organizationId, String fullName, String phone, String email) {
        return tenants.save(new Tenant(organizationId, fullName.trim(), clean(phone), clean(email)));
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
