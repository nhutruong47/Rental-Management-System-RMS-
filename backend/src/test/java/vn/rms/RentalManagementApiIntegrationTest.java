package vn.rms;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RentalManagementApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void csrfTokenIsAvailableBeforeLogin() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void healthEndpointIsAvailableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void anonymousUserCannotReadProperties() throws Exception {
        mockMvc.perform(get("/api/properties"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCanRegisterSignInAndCreatePropertyAndRoom() throws Exception {
        MockHttpSession session = registerAndSignIn("owner-" + UUID.randomUUID() + "@example.test", "Nhà trọ Mặt Trời");
        MvcResult property = mockMvc.perform(authenticated(post("/api/properties"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nhà trọ Bình Minh","address":"12 Đường Mai, Hà Nội"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Nhà trọ Bình Minh"))
                .andReturn();
        String propertyId = jsonValue(property, "$.id");

        mockMvc.perform(authenticated(post("/api/properties/" + propertyId + "/rooms"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomJson("P101", 3500000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomCode").value("P101"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.monthlyRent").value(3500000));

        mockMvc.perform(authenticated(get("/api/properties/" + propertyId + "/rooms"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomCode").value("P101"));
    }

    @Test
    void roomCodeMustBeUniqueWithinProperty() throws Exception {
        MockHttpSession session = registerAndSignIn("owner-" + UUID.randomUUID() + "@example.test", "Nhà trọ Xanh");
        String propertyId = createProperty(session);
        createRoom(session, propertyId, "A01", 2200000).andExpect(status().isCreated());
        createRoom(session, propertyId, "A01", 2200000).andExpect(status().isConflict());
    }

    @Test
    void negativeRentIsRejected() throws Exception {
        MockHttpSession session = registerAndSignIn("owner-" + UUID.randomUUID() + "@example.test", "Nhà trọ Sen");
        String propertyId = createProperty(session);
        createRoom(session, propertyId, "B01", -1).andExpect(status().isBadRequest());
    }

    @Test
    void ownerCannotReadAnotherOrganizationsProperty() throws Exception {
        MockHttpSession firstOwner = registerAndSignIn("first-" + UUID.randomUUID() + "@example.test", "Chủ nhà Một");
        String propertyId = createProperty(firstOwner);
        MockHttpSession secondOwner = registerAndSignIn("second-" + UUID.randomUUID() + "@example.test", "Chủ nhà Hai");

        mockMvc.perform(authenticated(get("/api/properties/" + propertyId), secondOwner))
                .andExpect(status().isNotFound());
    }

    @Test
    void registrationCreatesAnOrganizationOwnerAndRejectsDuplicateEmail() throws Exception {
        String email = "owner-" + UUID.randomUUID() + "@example.test";
        register(email, "Chủ nhà Bạc Liêu").andExpect(status().isCreated());
        register(email, "Tổ chức khác").andExpect(status().isConflict());
    }

    @Test
    void invalidCredentialsAreRejected() throws Exception {
        registerAndSignIn("valid-" + UUID.randomUUID() + "@example.test", "Nhà hợp lệ");
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"missing@example.test","password":"WrongPass!234"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCanCreateTenantAndActivateAnExclusiveRoomContract() throws Exception {
        MockHttpSession session = registerAndSignIn("lease-" + UUID.randomUUID() + "@example.test", "Chủ nhà Hạ Long");
        String propertyId = createProperty(session);
        createRoom(session, propertyId, "C01", 4000000).andExpect(status().isCreated());
        String roomId = jsonValue(mockMvc.perform(authenticated(get("/api/properties/" + propertyId + "/rooms"), session))
                .andExpect(status().isOk()).andReturn(), "$[0].id");
        String tenantId = createTenant(session);
        String contractId = createContract(session, roomId, tenantId, "2026-12-01", "2027-11-30");

        mockMvc.perform(authenticated(post("/api/contracts/" + contractId + "/activate"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.monthlyRent").value(4000000));
        mockMvc.perform(authenticated(get("/api/properties/" + propertyId + "/rooms"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("OCCUPIED"));
        mockMvc.perform(authenticated(get("/api/contracts"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(contractId))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        String overlappingId = createContract(session, roomId, tenantId, "2027-01-01", "2027-12-31");
        mockMvc.perform(authenticated(post("/api/contracts/" + overlappingId + "/activate"), session))
                .andExpect(status().isConflict());
    }

    @Test
    void historicalMeterReadingCannotBeInsertedAfterANewerPeriod() throws Exception {
        MockHttpSession session = registerAndSignIn("meter-order-" + UUID.randomUUID() + "@example.test", "Chủ nhà Đồng Nai");
        String propertyId = createProperty(session);
        createRoom(session, propertyId, "M01", 3500000).andExpect(status().isCreated());
        String roomId = jsonValue(mockMvc.perform(authenticated(get("/api/properties/" + propertyId + "/rooms"), session))
                .andExpect(status().isOk()).andReturn(), "$[0].id");
        mockMvc.perform(authenticated(post("/api/utility-tariffs"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"utilityType":"ELECTRICITY","pricePerUnit":4000,"effectiveFrom":"2026-01-01"}
                                """))
                .andExpect(status().isCreated());

        createMeterReading(session, roomId, "ELECTRICITY", 100).andExpect(status().isCreated());
        mockMvc.perform(authenticated(post("/api/meter-readings"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":"%s","utilityType":"ELECTRICITY","billingPeriod":"2026-11","currentReading":90}
                                """.formatted(roomId)))
                .andExpect(status().isConflict());
    }

    @Test
    void ownerCanGenerateInvoiceFromUtilityReadingsAndRecordPartialPayment() throws Exception {
        MockHttpSession session = registerAndSignIn("billing-" + UUID.randomUUID() + "@example.test", "Chủ nhà Cần Thơ");
        String propertyId = createProperty(session);
        createRoom(session, propertyId, "D01", 4000000).andExpect(status().isCreated());
        String roomId = jsonValue(mockMvc.perform(authenticated(get("/api/properties/" + propertyId + "/rooms"), session))
                .andExpect(status().isOk()).andReturn(), "$[0].id");
        String tenantId = createTenant(session);
        String contractId = createContract(session, roomId, tenantId, "2026-12-01", "2027-11-30");
        mockMvc.perform(authenticated(post("/api/contracts/" + contractId + "/activate"), session))
                .andExpect(status().isOk());

        mockMvc.perform(authenticated(post("/api/utility-tariffs"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"utilityType":"ELECTRICITY","pricePerUnit":4000,"effectiveFrom":"2026-12-01"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(authenticated(post("/api/utility-tariffs"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"utilityType":"WATER","pricePerUnit":10000,"effectiveFrom":"2026-12-01"}
                                """))
                .andExpect(status().isCreated());
        createMeterReading(session, roomId, "ELECTRICITY", 100)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usage").value(100))
                .andExpect(jsonPath("$.charge").value(400000));
        createMeterReading(session, roomId, "WATER", 5)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.charge").value(50000));
        createMeterReading(session, roomId, "WATER", 6).andExpect(status().isConflict());
        mockMvc.perform(authenticated(post("/api/invoices/generate"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"billingPeriod":"2026-12"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());

        MvcResult generated = mockMvc.perform(authenticated(post("/api/invoices/generate"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"billingPeriod":"2026-12"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalAmount").value(4450000))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andReturn();
        String invoiceId = jsonValue(generated, "$[0].id");
        mockMvc.perform(authenticated(post("/api/invoices/" + invoiceId + "/issue"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"));
        String idempotencyKey = UUID.randomUUID().toString();
        String paymentJson = """
                {"amount":1500000,"method":"BANK_TRANSFER","reference":"DEMO-TRANSFER-001","idempotencyKey":"%s"}
                """.formatted(idempotencyKey);
        mockMvc.perform(authenticated(post("/api/invoices/" + invoiceId + "/payments"), session)
                        .contentType(MediaType.APPLICATION_JSON).content(paymentJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceStatus").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.balanceDue").value(2950000));
        mockMvc.perform(authenticated(post("/api/invoices/" + invoiceId + "/payments"), session)
                        .contentType(MediaType.APPLICATION_JSON).content(paymentJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceDue").value(2950000));
        mockMvc.perform(authenticated(post("/api/invoices/" + invoiceId + "/payments"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentJson.replace("1500000", "1000000")))
                .andExpect(status().isConflict());
        mockMvc.perform(authenticated(post("/api/invoices/" + invoiceId + "/payments"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentJson.replace("1500000", "4000000").replace(idempotencyKey, UUID.randomUUID().toString())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invoiceCannotBeIssuedOrPaidByAnotherOrganization() throws Exception {
        MockHttpSession owner = registerAndSignIn("billing-owner-" + UUID.randomUUID() + "@example.test", "Chủ nhà Hải Phòng");
        String propertyId = createProperty(owner);
        createRoom(owner, propertyId, "I01", 3000000).andExpect(status().isCreated());
        String roomId = jsonValue(mockMvc.perform(authenticated(get("/api/properties/" + propertyId + "/rooms"), owner))
                .andExpect(status().isOk()).andReturn(), "$[0].id");
        String tenantId = createTenant(owner);
        String contractId = createContract(owner, roomId, tenantId, "2026-12-01", "2027-11-30");
        mockMvc.perform(authenticated(post("/api/contracts/" + contractId + "/activate"), owner))
                .andExpect(status().isOk());
        MvcResult generated = mockMvc.perform(authenticated(post("/api/invoices/generate"), owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"billingPeriod":"2026-12"}
                                """))
                .andExpect(status().isOk()).andReturn();
        String invoiceId = jsonValue(generated, "$[0].id");
        mockMvc.perform(authenticated(post("/api/invoices/" + invoiceId + "/issue"), owner))
                .andExpect(status().isOk());
        MockHttpSession anotherOwner = registerAndSignIn("billing-other-" + UUID.randomUUID() + "@example.test", "Chủ nhà Huế");
        mockMvc.perform(authenticated(post("/api/invoices/" + invoiceId + "/issue"), anotherOwner))
                .andExpect(status().isNotFound());
        mockMvc.perform(authenticated(post("/api/invoices/" + invoiceId + "/payments"), anotherOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100000,"method":"CASH","reference":"WRONG-ORG","idempotencyKey":"%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());

        mockMvc.perform(authenticated(post("/api/utility-tariffs"), owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"utilityType":"WATER","pricePerUnit":10000,"effectiveFrom":"2026-12-01"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(authenticated(post("/api/meter-readings"), owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":"%s","utilityType":"WATER","billingPeriod":"2026-12","currentReading":-1}
                                """.formatted(roomId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(authenticated(post("/api/utility-tariffs"), owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"utilityType":"ELECTRICITY","pricePerUnit":-50,"effectiveFrom":"2026-12-01"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidTariffAndNegativeMeterReadingAreRejected() throws Exception {
        MockHttpSession session = registerAndSignIn("invalid-utility-" + UUID.randomUUID() + "@example.test", "Chủ nhà Đà Lạt");
        mockMvc.perform(authenticated(post("/api/utility-tariffs"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"utilityType":"WATER","pricePerUnit":-50,"effectiveFrom":"2026-12-01"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tenantsAreIsolatedBetweenOrganizations() throws Exception {
        MockHttpSession owner = registerAndSignIn("tenant-a-" + UUID.randomUUID() + "@example.test", "Nhà Một");
        createTenant(owner);
        MockHttpSession otherOwner = registerAndSignIn("tenant-b-" + UUID.randomUUID() + "@example.test", "Nhà Hai");

        mockMvc.perform(authenticated(get("/api/tenants"), otherOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void currentSessionUserAndUtilityTariffsAreScopedToOrganization() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());

        MockHttpSession owner = registerAndSignIn("me-" + UUID.randomUUID() + "@example.test", "Nhà Trọ Sài Gòn");
        mockMvc.perform(authenticated(get("/api/auth/me"), owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationName").value("Nhà Trọ Sài Gòn"))
                .andExpect(jsonPath("$.role").value("OWNER"));

        mockMvc.perform(authenticated(post("/api/utility-tariffs"), owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"utilityType":"ELECTRICITY","pricePerUnit":3800,"effectiveFrom":"2026-11-01"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(authenticated(get("/api/utility-tariffs"), owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].utilityType").value("ELECTRICITY"))
                .andExpect(jsonPath("$[0].pricePerUnit").value(3800));

        MockHttpSession otherOwner = registerAndSignIn("me-other-" + UUID.randomUUID() + "@example.test", "Nhà Khác");
        mockMvc.perform(authenticated(get("/api/utility-tariffs"), otherOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void missingCsrfReturnsForbiddenAndInvalidRegisterReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nocsrf@example.test","password":"LocalPass!234","fullName":"Như","organizationName":"Nhà"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bad","password":"short","fullName":"","organizationName":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void fullWorkflowSucceedsWithoutOuterTransactionWhenOpenInViewIsDisabled() throws Exception {
        MockHttpSession session = registerAndSignIn("no-osiv-" + UUID.randomUUID() + "@example.test", "Nhà Không OSIV");
        mockMvc.perform(authenticated(get("/api/auth/me"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationName").value("Nhà Không OSIV"));

        String propertyId = createProperty(session);
        createRoom(session, propertyId, "N01", 3200000).andExpect(status().isCreated());
        String roomId = jsonValue(mockMvc.perform(authenticated(get("/api/properties/" + propertyId + "/rooms"), session))
                .andExpect(status().isOk()).andReturn(), "$[0].id");
        String tenantId = createTenant(session);
        String contractId = createContract(session, roomId, tenantId, "2026-11-01", "2027-10-31");
        mockMvc.perform(authenticated(post("/api/contracts/" + contractId + "/activate"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(roomId))
                .andExpect(jsonPath("$.tenantId").value(tenantId));
        mockMvc.perform(authenticated(get("/api/contracts"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomId").value(roomId));

        mockMvc.perform(authenticated(post("/api/utility-tariffs"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"utilityType":"ELECTRICITY","pricePerUnit":3500,"effectiveFrom":"2026-11-01"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(authenticated(get("/api/utility-tariffs"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pricePerUnit").value(3500));
        mockMvc.perform(authenticated(post("/api/meter-readings"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":"%s","utilityType":"ELECTRICITY","billingPeriod":"2026-11","currentReading":50}
                                """.formatted(roomId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value(roomId));
        mockMvc.perform(authenticated(get("/api/meter-readings?billingPeriod=2026-11"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomId").value(roomId));

        MvcResult generated = mockMvc.perform(authenticated(post("/api/invoices/generate"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"billingPeriod":"2026-11"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomId").value(roomId))
                .andExpect(jsonPath("$[0].totalAmount").value(3375000))
                .andReturn();
        String invoiceId = jsonValue(generated, "$[0].id");
        mockMvc.perform(authenticated(get("/api/invoices?billingPeriod=2026-11"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tenantId").value(tenantId));
        mockMvc.perform(authenticated(post("/api/invoices/" + invoiceId + "/issue"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"));
    }

    @Test
    void roomStatusCannotBeManuallyChangedWhileOccupied() throws Exception {
        MockHttpSession session = registerAndSignIn("room-status-" + UUID.randomUUID() + "@example.test", "Nhà Trạng Thái");
        String propertyId = createProperty(session);
        createRoom(session, propertyId, "S01", 3000000).andExpect(status().isCreated());
        String roomId = jsonValue(mockMvc.perform(authenticated(get("/api/properties/" + propertyId + "/rooms"), session))
                .andExpect(status().isOk()).andReturn(), "$[0].id");

        mockMvc.perform(authenticated(post("/api/properties/" + propertyId + "/rooms/" + roomId + "/status"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"MAINTENANCE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));

        mockMvc.perform(authenticated(post("/api/properties/" + propertyId + "/rooms/" + roomId + "/status"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"AVAILABLE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        String tenantId = createTenant(session);
        String contractId = createContract(session, roomId, tenantId, "2026-11-01", "2027-10-31");
        mockMvc.perform(authenticated(post("/api/contracts/" + contractId + "/activate"), session))
                .andExpect(status().isOk());

        mockMvc.perform(authenticated(post("/api/properties/" + propertyId + "/rooms/" + roomId + "/status"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"AVAILABLE"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void ownerCanTerminateActiveContractAfterSettlingInvoicesAndReleaseRoom() throws Exception {
        MockHttpSession session = registerAndSignIn("checkout-" + UUID.randomUUID() + "@example.test", "Nhà Bàn Giao");
        String propertyId = createProperty(session);
        createRoom(session, propertyId, "H01", 3000000).andExpect(status().isCreated());
        String roomId = jsonValue(mockMvc.perform(authenticated(get("/api/properties/" + propertyId + "/rooms"), session))
                .andExpect(status().isOk()).andReturn(), "$[0].id");
        String tenantId = createTenant(session);
        String contractId = createContract(session, roomId, tenantId, "2026-11-01", "2027-10-31");
        mockMvc.perform(authenticated(post("/api/contracts/" + contractId + "/activate"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.depositAmount").value(2200000));

        MvcResult generated = mockMvc.perform(authenticated(post("/api/invoices/generate"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"billingPeriod":"2026-11"}
                                """))
                .andExpect(status().isOk()).andReturn();
        String invoiceId = jsonValue(generated, "$[0].id");
        mockMvc.perform(authenticated(post("/api/invoices/" + invoiceId + "/issue"), session))
                .andExpect(status().isOk());

        mockMvc.perform(authenticated(post("/api/contracts/" + contractId + "/terminate"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"terminatedDate":"2026-11-30","depositDeducted":200000,"checklistNotes":"Chưa thanh toán hóa đơn"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(authenticated(post("/api/invoices/" + invoiceId + "/payments"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":3000000,"method":"CASH","reference":"FULL-SETTLE","idempotencyKey":"%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated());

        mockMvc.perform(authenticated(post("/api/contracts/" + contractId + "/terminate"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"terminatedDate":"2026-11-30","depositDeducted":2500000,"checklistNotes":"Vượt tiền cọc"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(authenticated(post("/api/contracts/" + contractId + "/terminate"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"terminatedDate":"2026-11-30","depositDeducted":200000,"finalElectricityReading":120,"finalWaterReading":8,"checklistNotes":"Bàn giao đủ chìa khóa"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.depositHeld").value(2200000))
                .andExpect(jsonPath("$.depositDeducted").value(200000))
                .andExpect(jsonPath("$.depositRefunded").value(2000000));

        mockMvc.perform(authenticated(get("/api/properties/" + propertyId + "/rooms"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));

        MockHttpSession otherOwner = registerAndSignIn("checkout-other-" + UUID.randomUUID() + "@example.test", "Nhà Khác");
        mockMvc.perform(authenticated(get("/api/contracts/" + contractId + "/handover"), otherOwner))
                .andExpect(status().isNotFound());
    }

    @Test
    void maintenanceTicketWorkflowEnforcesTransitionsAndOrganizationIsolation() throws Exception {
        MockHttpSession session = registerAndSignIn("maint-" + UUID.randomUUID() + "@example.test", "Nhà Bảo Trì");
        String propertyId = createProperty(session);
        createRoom(session, propertyId, "M101", 3000000).andExpect(status().isCreated());
        String roomId = jsonValue(mockMvc.perform(authenticated(get("/api/properties/" + propertyId + "/rooms"), session))
                .andExpect(status().isOk()).andReturn(), "$[0].id");

        MvcResult created = mockMvc.perform(authenticated(post("/api/maintenance-tickets"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":"%s","title":"Rò rỉ vòi nước","description":"Vòi lavabo nhỏ giọt","priority":"HIGH","estimatedCost":150000}
                                """.formatted(roomId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();
        String ticketId = jsonValue(created, "$.id");

        mockMvc.perform(authenticated(post("/api/maintenance-tickets/" + ticketId + "/status"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RESOLVED","actualCost":120000,"resolutionNotes":"Chưa tiếp nhận"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(authenticated(post("/api/maintenance-tickets/" + ticketId + "/assign"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assigneeName":"Anh Tùng Kỹ Thuật"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assigneeName").value("Anh Tùng Kỹ Thuật"));

        mockMvc.perform(authenticated(post("/api/maintenance-tickets/" + ticketId + "/status"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_PROGRESS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(authenticated(post("/api/maintenance-tickets/" + ticketId + "/status"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RESOLVED","actualCost":120000,"resolutionNotes":"Đã thay ron cao su"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.actualCost").value(120000));

        mockMvc.perform(authenticated(post("/api/maintenance-tickets/" + ticketId + "/status"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CLOSED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(authenticated(get("/api/notifications"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());

        MockHttpSession otherOwner = registerAndSignIn("maint-other-" + UUID.randomUUID() + "@example.test", "Nhà Ngoài");
        mockMvc.perform(authenticated(get("/api/maintenance-tickets"), otherOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(authenticated(post("/api/maintenance-tickets/" + ticketId + "/assign"), otherOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assigneeName":"Intruder"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCanInviteStaffAndProvisionTenantPortalWithStrictRbacAndIdorIsolation() throws Exception {
        MockHttpSession owner = registerAndSignIn("rbac-owner-" + UUID.randomUUID() + "@example.test", "Nhà Phân Quyền");
        String propertyId = createProperty(owner);
        createRoom(owner, propertyId, "R101", 3500000).andExpect(status().isCreated());
        createRoom(owner, propertyId, "R102", 3500000).andExpect(status().isCreated());
        MvcResult roomsResult = mockMvc.perform(authenticated(get("/api/properties/" + propertyId + "/rooms"), owner))
                .andExpect(status().isOk()).andReturn();
        String room1Id = jsonValue(roomsResult, "$[0].id");
        String room2Id = jsonValue(roomsResult, "$[1].id");
        String tenantId = createTenant(owner);
        String contractId = createContract(owner, room1Id, tenantId, "2026-11-01", "2027-10-31");
        mockMvc.perform(authenticated(post("/api/contracts/" + contractId + "/activate"), owner))
                .andExpect(status().isOk());

        String staffEmail = "staff-" + UUID.randomUUID() + "@example.test";
        mockMvc.perform(authenticated(post("/api/members"), owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"StaffPass!2345","fullName":"Nhân Viên A","role":"STAFF"}
                                """.formatted(staffEmail)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STAFF"));

        MockHttpSession staffSession = signInWith(staffEmail, "StaffPass!2345");
        mockMvc.perform(authenticated(post("/api/properties"), staffSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Blocked","address":"Addr"}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(authenticated(get("/api/audit-logs"), staffSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(authenticated(post("/api/maintenance-tickets"), staffSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":"%s","title":"Đèn cầu thang","description":"Kiểm tra bóng đèn","priority":"LOW","estimatedCost":50000}
                                """.formatted(room1Id)))
                .andExpect(status().isCreated());

        String tenantEmail = "tenant-portal-" + UUID.randomUUID() + "@example.test";
        mockMvc.perform(authenticated(post("/api/tenants/" + tenantId + "/portal-account"), owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"TenantPass!234"}
                                """.formatted(tenantEmail)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("TENANT"));

        MockHttpSession tenantSession = signInWith(tenantEmail, "TenantPass!234");
        mockMvc.perform(authenticated(get("/api/properties"), tenantSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(authenticated(get("/api/tenant-portal/overview"), tenantSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contracts[0].roomId").value(room1Id));
        mockMvc.perform(authenticated(post("/api/tenant-portal/tickets"), tenantSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":"%s","title":"Báo hỏng khóa","description":"Không khóa được","priority":"HIGH"}
                                """.formatted(room1Id)))
                .andExpect(status().isCreated());
        mockMvc.perform(authenticated(post("/api/tenant-portal/tickets"), tenantSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":"%s","title":"IDOR test","description":"Phòng người khác","priority":"LOW"}
                                """.formatted(room2Id)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanConfigureSettingsAdjustDraftInvoiceAndInspectAuditLogs() throws Exception {
        MockHttpSession owner = registerAndSignIn("settings-" + UUID.randomUUID() + "@example.test", "Nhà Dịch Vụ");
        mockMvc.perform(authenticated(post("/api/settings"), owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"billingDueDay":10,"internetFee":100000,"garbageFee":30000,"parkingFee":50000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.internetFee").value(100000));

        String propertyId = createProperty(owner);
        createRoom(owner, propertyId, "D101", 3000000).andExpect(status().isCreated());
        String roomId = jsonValue(mockMvc.perform(authenticated(get("/api/properties/" + propertyId + "/rooms"), owner))
                .andExpect(status().isOk()).andReturn(), "$[0].id");
        String tenantId = createTenant(owner);
        String contractId = createContract(owner, roomId, tenantId, "2026-11-01", "2027-10-31");
        mockMvc.perform(authenticated(post("/api/contracts/" + contractId + "/activate"), owner))
                .andExpect(status().isOk());

        MvcResult generated = mockMvc.perform(authenticated(post("/api/invoices/generate"), owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"billingPeriod":"2026-11"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceAmount").value(180000))
                .andExpect(jsonPath("$[0].totalAmount").value(3180000))
                .andReturn();
        String invoiceId = jsonValue(generated, "$[0].id");

        mockMvc.perform(authenticated(post("/api/invoices/" + invoiceId + "/adjust"), owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceAmount":200000,"discountAmount":100000,"adjustmentNotes":"Giảm phí tháng đầu"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(3100000));

        mockMvc.perform(authenticated(post("/api/invoices/" + invoiceId + "/issue"), owner))
                .andExpect(status().isOk());

        mockMvc.perform(authenticated(post("/api/invoices/" + invoiceId + "/adjust"), owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceAmount":0,"discountAmount":0,"adjustmentNotes":"Sửa sau phát hành"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(authenticated(get("/api/audit-logs"), owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").isNotEmpty());
    }

    @Test
    void loginRateLimiterBlocksExcessiveAttemptsAndUserCanChangePassword() throws Exception {
        String email = "ratelimit-" + UUID.randomUUID() + "@example.test";
        MockHttpSession session = registerAndSignIn(email, "Nhà Bảo Mật");

        mockMvc.perform(authenticated(post("/api/auth/change-password"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"LocalPass!234","newPassword":"NewSecurePass!999"}
                                """))
                .andExpect(status().isNoContent());

        signInWith(email, "NewSecurePass!999");

        String rateTarget = "victim-" + UUID.randomUUID() + "@example.test";
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"%s","password":"WrongPassword123!"}
                                    """.formatted(rateTarget)))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"WrongPassword123!"}
                                """.formatted(rateTarget)))
                .andExpect(status().isTooManyRequests());
    }

    private MockHttpSession signInWith(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private ResultActions createMeterReading(MockHttpSession session, String roomId, String utilityType,
                                             long currentReading) throws Exception {
        return mockMvc.perform(authenticated(post("/api/meter-readings"), session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"roomId":"%s","utilityType":"%s","billingPeriod":"2026-12","currentReading":%d}
                        """.formatted(roomId, utilityType, currentReading)));
    }

    private String createTenant(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(authenticated(post("/api/tenants"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Nguyễn Thị Lan","phone":"0901234567"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return jsonValue(result, "$.id");
    }

    private String createContract(MockHttpSession session, String roomId, String tenantId,
                                  String startDate, String endDate) throws Exception {
        MvcResult result = mockMvc.perform(authenticated(post("/api/contracts"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":"%s","tenantId":"%s","startDate":"%s","endDate":"%s"}
                                """.formatted(roomId, tenantId, startDate, endDate)))
                .andExpect(status().isCreated())
                .andReturn();
        return jsonValue(result, "$.id");
    }

    private MockHttpSession registerAndSignIn(String email, String organizationName) throws Exception {
        register(email, organizationName).andExpect(status().isCreated());
        MvcResult result = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"LocalPass!234"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private ResultActions register(String email, String organizationName) throws Exception {
        return mockMvc.perform(post("/api/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"LocalPass!234","fullName":"Như Trương","organizationName":"%s"}
                        """.formatted(email, organizationName)));
    }

    private String createProperty(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(authenticated(post("/api/properties"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nhà trọ kiểm thử","address":"Phường 1, Cà Mau"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return jsonValue(result, "$.id");
    }

    private ResultActions createRoom(MockHttpSession session, String propertyId, String roomCode, long rent) throws Exception {
        return mockMvc.perform(authenticated(post("/api/properties/" + propertyId + "/rooms"), session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(roomJson(roomCode, rent)));
    }

    private String roomJson(String roomCode, long rent) {
        return """
                {"roomCode":"%s","areaSquareMeters":18,"capacity":1,"monthlyRent":%d,"depositAmount":2200000}
                """.formatted(roomCode, rent);
    }

    private String jsonValue(MvcResult result, String path) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request, MockHttpSession session) {
        return request.session(session).with(csrf());
    }
}
