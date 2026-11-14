package vn.rms.property;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import vn.rms.identity.Organization;
import vn.rms.identity.OrganizationRepository;

@Service
public class PropertyService {
    private final PropertyRepository properties;
    private final RoomRepository rooms;
    private final OrganizationRepository organizations;

    public PropertyService(PropertyRepository properties, RoomRepository rooms, OrganizationRepository organizations) {
        this.properties = properties;
        this.rooms = rooms;
        this.organizations = organizations;
    }

    @Transactional(readOnly = true)
    public List<PropertyResponse> listProperties(UUID organizationId) {
        return properties.findAllByOrganizationIdOrderByName(organizationId).stream()
                .map(PropertyResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PropertyResponse getProperty(UUID organizationId, UUID propertyId) {
        return PropertyResponse.from(requireProperty(organizationId, propertyId));
    }

    @Transactional
    public PropertyResponse createProperty(UUID organizationId, String name, String address) {
        if (properties.existsByOrganizationIdAndNameIgnoreCaseAndAddressIgnoreCase(organizationId, name.trim(), address.trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Property already exists");
        }
        Organization organization = organizations.findById(organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return PropertyResponse.from(properties.save(new Property(organization, name.trim(), address.trim())));
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> listRooms(UUID organizationId, UUID propertyId) {
        requireProperty(organizationId, propertyId);
        return rooms.findAllByProperty_IdAndOrganization_IdOrderByRoomCode(propertyId, organizationId).stream()
                .map(RoomResponse::from).toList();
    }

    @Transactional
    public RoomResponse createRoom(UUID organizationId, UUID propertyId, String roomCode,
                                   BigDecimal area, short capacity, BigDecimal rent, BigDecimal deposit) {
        Property property = requireProperty(organizationId, propertyId);
        if (rooms.existsByProperty_IdAndRoomCodeIgnoreCase(propertyId, roomCode.trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room code already exists");
        }
        Room room = new Room(property.getOrganization(), property, roomCode.trim(), area, capacity, rent, deposit);
        return RoomResponse.from(rooms.save(room));
    }

    @Transactional
    public RoomResponse updateRoomStatus(UUID organizationId, UUID propertyId, UUID roomId, RoomStatus targetStatus) {
        requireProperty(organizationId, propertyId);
        Room room = rooms.findByProperty_IdAndIdAndOrganization_Id(propertyId, roomId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        try {
            room.setOperationalStatus(targetStatus);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        return RoomResponse.from(room);
    }

    private Property requireProperty(UUID organizationId, UUID propertyId) {
        return properties.findByIdAndOrganizationId(propertyId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
    }

    public record PropertyResponse(UUID id, String name, String address) {
        static PropertyResponse from(Property property) {
            return new PropertyResponse(property.getId(), property.getName(), property.getAddress());
        }
    }

    public record RoomResponse(UUID id, UUID propertyId, String roomCode, BigDecimal areaSquareMeters,
                               short capacity, BigDecimal monthlyRent, BigDecimal depositAmount, RoomStatus status) {
        static RoomResponse from(Room room) {
            return new RoomResponse(room.getId(), room.getPropertyId(), room.getRoomCode(), room.getAreaSquareMeters(),
                    room.getCapacity(), room.getMonthlyRent(), room.getDepositAmount(), room.getStatus());
        }
    }
}
