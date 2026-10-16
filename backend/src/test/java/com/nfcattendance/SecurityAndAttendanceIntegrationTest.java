package com.nfcattendance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nfcattendance.entity.*;
import com.nfcattendance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAndAttendanceIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired RoleRepository roleRepository;
    @Autowired UserAccountRepository userAccountRepository;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired WorkplaceRepository workplaceRepository;
    @Autowired NfcStationRepository nfcStationRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        employeeRepository.deleteAll();
        nfcStationRepository.deleteAll();
        userAccountRepository.deleteAll();
        departmentRepository.deleteAll();
        workplaceRepository.deleteAll();
        roleRepository.deleteAll();

        Role adminRole = roleRepository.save(Role.builder().name(RoleName.ADMIN).build());
        Role employeeRole = roleRepository.save(Role.builder().name(RoleName.EMPLOYEE).build());
        Department dept = departmentRepository.save(Department.builder().name("ייצור").code("PROD").active(true).build());
        Workplace wp = workplaceRepository.save(Workplace.builder()
                .name("HQ").code("HQ").timezone("Asia/Jerusalem").active(true).build());

        UserAccount admin = userAccountRepository.save(UserAccount.builder()
                .username("admin")
                .email("admin@test.com")
                .passwordHash(passwordEncoder.encode("Admin123!"))
                .enabled(true)
                .roles(new HashSet<>(List.of(adminRole)))
                .build());

        UserAccount empUser = userAccountRepository.save(UserAccount.builder()
                .username("emp")
                .email("emp@test.com")
                .passwordHash(passwordEncoder.encode("Employee123!"))
                .enabled(true)
                .roles(new HashSet<>(List.of(employeeRole)))
                .build());

        employeeRepository.save(Employee.builder()
                .user(empUser)
                .employeeNumber("E1")
                .fullName("Test Emp")
                .email("emp@test.com")
                .department(dept)
                .workplace(wp)
                .employmentStatus(EmploymentStatus.ACTIVE)
                .build());

        nfcStationRepository.save(NfcStation.builder()
                .stationCode("GATE-01")
                .name("Gate")
                .workplace(wp)
                .registrationValue("NFC-" + UUID.randomUUID().toString().substring(0, 8))
                .status(NfcStationStatus.ACTIVE)
                .build());

        assertThat(admin.getId()).isNotNull();
    }

    @Test
    void employeeCannotAccessAdminEmployees() throws Exception {
        String token = login("emp", "Employee123!");
        mockMvc.perform(get("/api/admin/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListEmployees() throws Exception {
        String token = login("admin", "Admin123!");
        mockMvc.perform(get("/api/admin/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeNumber").value("E1"));
    }

    @Test
    void clockInAndPreventDuplicateOpenSession() throws Exception {
        String token = login("emp", "Employee123!");
        String body = "{\"stationCode\":\"GATE-01\",\"clientRequestId\":\"req-1\"}";

        mockMvc.perform(post("/api/attendance/clock-in")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventType").value("CLOCK_IN"));

        mockMvc.perform(post("/api/attendance/clock-in")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stationCode\":\"GATE-01\",\"clientRequestId\":\"req-2\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/attendance/clock-out")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stationCode\":\"GATE-01\",\"clientRequestId\":\"req-3\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventType").value("CLOCK_OUT"));
    }

    @Test
    void clockOutWithoutOpenSessionFails() throws Exception {
        String token = login("emp", "Employee123!");
        mockMvc.perform(post("/api/attendance/clock-out")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stationCode\":\"GATE-01\"}"))
                .andExpect(status().isConflict());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("accessToken").asText();
    }
}
