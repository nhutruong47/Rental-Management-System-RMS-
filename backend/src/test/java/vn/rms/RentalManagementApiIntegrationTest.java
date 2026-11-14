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
