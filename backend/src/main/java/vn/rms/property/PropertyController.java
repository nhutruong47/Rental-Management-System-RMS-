package vn.rms.property;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import vn.rms.identity.RmsUserPrincipal;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {
    private final PropertyService service;

    public PropertyController(PropertyService service) {
        this.service = service;
    }

    @GetMapping
    public List<PropertyService.PropertyResponse> list(@AuthenticationPrincipal RmsUserPrincipal principal) {
        return service.listProperties(principal.organizationId());
    }

    @GetMapping("/{propertyId}")
    public PropertyService.PropertyResponse get(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                @PathVariable UUID propertyId) {
        return service.getProperty(principal.organizationId(), propertyId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyService.PropertyResponse create(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                   @Valid @RequestBody CreatePropertyRequest request) {
        return service.createProperty(principal.organizationId(), request.name(), request.address());
    }

    @GetMapping("/{propertyId}/rooms")
    public List<PropertyService.RoomResponse> rooms(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                    @PathVariable UUID propertyId) {
        return service.listRooms(principal.organizationId(), propertyId);
    }

    @PostMapping("/{propertyId}/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyService.RoomResponse createRoom(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                   @PathVariable UUID propertyId,
                                                   @Valid @RequestBody CreateRoomRequest request) {
        return service.createRoom(principal.organizationId(), propertyId, request.roomCode(), request.areaSquareMeters(),
                request.capacity(), request.monthlyRent(), request.depositAmount());
    }

    @PostMapping("/{propertyId}/rooms/{roomId}/status")
    public PropertyService.RoomResponse updateRoomStatus(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                         @PathVariable UUID propertyId,
                                                         @PathVariable UUID roomId,
                                                         @Valid @RequestBody UpdateRoomStatusRequest request) {
        return service.updateRoomStatus(principal.organizationId(), propertyId, roomId, request.status());
    }

    public record UpdateRoomStatusRequest(@NotNull RoomStatus status) { }

    public record CreatePropertyRequest(@NotBlank @Size(max = 120) String name,
                                        @NotBlank @Size(max = 300) String address) { }

    public record CreateRoomRequest(@NotBlank @Size(max = 40) String roomCode,
                                    @NotNull @DecimalMin(value = "0.01") BigDecimal areaSquareMeters,
                                    @Positive short capacity,
                                    @NotNull @DecimalMin(value = "0") BigDecimal monthlyRent,
                                    @NotNull @DecimalMin(value = "0") BigDecimal depositAmount) { }
}
