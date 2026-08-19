package com.nhat.workflowhub.workflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nhat.workflowhub.auth.entity.UserAccount;
import com.nhat.workflowhub.auth.entity.UserStatus;
import com.nhat.workflowhub.auth.security.JwtService;
import com.nhat.workflowhub.auth.repository.UserAccountRepository;
import com.nhat.workflowhub.membership.repository.MembershipRepository;
import com.nhat.workflowhub.notification.service.NotificationService;
import com.nhat.workflowhub.organization.entity.Organization;
import com.nhat.workflowhub.organization.service.OrganizationService;
import com.nhat.workflowhub.workflow.entity.WorkflowPriority;
import com.nhat.workflowhub.workflow.entity.WorkflowStatus;
import com.nhat.workflowhub.workflow.entity.WorkflowEvent;
import com.nhat.workflowhub.workflow.repository.WorkflowEventRepository;
import com.nhat.workflowhub.workflow.repository.WorkflowItemRepository;
import com.nhat.workflowhub.workflow.entity.WorkflowItem;
import com.nhat.workflowhub.workspace.repository.WorkspaceRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WorkflowControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private JwtService jwtService;
  @MockBean
  private UserAccountRepository userAccountRepository;
  @MockBean
  private OrganizationService organizationService;
  @MockBean
  private WorkspaceRepository workspaceRepository;
  @MockBean
  private MembershipRepository membershipRepository;
  @MockBean
  private WorkflowItemRepository workflowItemRepository;
  @MockBean
  private WorkflowEventRepository workflowEventRepository;
  @MockBean
  private NotificationService notificationService;

  @Test
  void createWorkflowItem_returnsCreatedWorkflowItem() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID workspaceId = UUID.randomUUID();
    UUID workflowItemId = UUID.randomUUID();

    UserAccount user = new UserAccount();
    user.setId(userId);
    user.setEmail("nhat@example.com");
    user.setFullName("Nhat");
    user.setPasswordHash("hashed");
    user.setStatus(UserStatus.ACTIVE);
    user.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

    Organization organization = new Organization();
    organization.setId(organizationId);
    organization.setSlug("acme");
    organization.setName("Acme");
    organization.setOwnerUserId(userId);

    when(jwtService.isValidAccessToken("valid-token")).thenReturn(true);
    when(jwtService.extractUserId("valid-token")).thenReturn(userId);
    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
    when(organizationService.requireAccessibleOrganization("acme", userId)).thenReturn(organization);
    when(workspaceRepository.findByOrganizationIdAndSlug(organizationId, "general")).thenReturn(Optional.of(new com.nhat.workflowhub.workspace.entity.Workspace() {{
      setId(workspaceId);
      setOrganizationId(organizationId);
      setName("General");
      setSlug("general");
    }}));
    when(workflowItemRepository.save(any())).thenAnswer(invocation -> {
      com.nhat.workflowhub.workflow.entity.WorkflowItem item = invocation.getArgument(0);
      item.setId(workflowItemId);
      item.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
      item.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
      return item;
    });

    mockMvc.perform(post("/api/organizations/acme/workflow-items")
            .with(csrf())
            .header("Authorization", "Bearer valid-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": "Payment is broken",
                  "description": "Customer cannot pay",
                  "status": "NEW",
                  "priority": "MEDIUM",
                  "workspaceSlug": "general"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(workflowItemId.toString()))
        .andExpect(jsonPath("$.title").value("Payment is broken"))
        .andExpect(jsonPath("$.status").value("NEW"))
        .andExpect(jsonPath("$.priority").value("MEDIUM"));
  }

  @Test
  void listWorkflowItems_returnsWorkflowItems() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID workflowItemId = UUID.randomUUID();

    UserAccount user = activeUser(userId);
    Organization organization = organization(userId, organizationId);
    WorkflowItem item = workflowItem(workflowItemId, organizationId, UUID.randomUUID(), userId, "Payment is broken");

    when(jwtService.isValidAccessToken("valid-token")).thenReturn(true);
    when(jwtService.extractUserId("valid-token")).thenReturn(userId);
    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
    when(organizationService.requireAccessibleOrganization("acme", userId)).thenReturn(organization);
    when(workflowItemRepository.findAllByOrganizationIdOrderByUpdatedAtDesc(organizationId)).thenReturn(List.of(item));

    mockMvc.perform(get("/api/organizations/acme/workflow-items")
            .header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(workflowItemId.toString()))
        .andExpect(jsonPath("$[0].title").value("Payment is broken"));
  }

  @Test
  void getWorkflowItem_returnsWorkflowItemDetail() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID workflowItemId = UUID.randomUUID();

    UserAccount user = activeUser(userId);
    Organization organization = organization(userId, organizationId);
    WorkflowItem item = workflowItem(workflowItemId, organizationId, UUID.randomUUID(), userId, "Payment is broken");

    when(jwtService.isValidAccessToken("valid-token")).thenReturn(true);
    when(jwtService.extractUserId("valid-token")).thenReturn(userId);
    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
    when(organizationService.requireAccessibleOrganization("acme", userId)).thenReturn(organization);
    when(workflowItemRepository.findByIdAndOrganizationId(workflowItemId, organizationId)).thenReturn(Optional.of(item));

    mockMvc.perform(get("/api/organizations/acme/workflow-items/{workflowItemId}", workflowItemId)
            .header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(workflowItemId.toString()))
        .andExpect(jsonPath("$.title").value("Payment is broken"));
  }

  @Test
  void deleteWorkflowItem_returnsNoContent() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID workflowItemId = UUID.randomUUID();

    UserAccount user = activeUser(userId);
    Organization organization = organization(userId, organizationId);
    WorkflowItem item = workflowItem(workflowItemId, organizationId, UUID.randomUUID(), userId, "Payment is broken");

    when(jwtService.isValidAccessToken("valid-token")).thenReturn(true);
    when(jwtService.extractUserId("valid-token")).thenReturn(userId);
    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
    when(organizationService.requireAccessibleOrganization("acme", userId)).thenReturn(organization);
    when(workflowItemRepository.findByIdAndOrganizationId(workflowItemId, organizationId)).thenReturn(Optional.of(item));

    mockMvc.perform(delete("/api/organizations/acme/workflow-items/{workflowItemId}", workflowItemId)
            .with(csrf())
            .header("Authorization", "Bearer valid-token"))
        .andExpect(status().isNoContent());
  }

  @Test
  void createWorkflowItem_returnsForbiddenForViewer() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();

    UserAccount user = activeUser(userId);
    Organization organization = organization(ownerId, organizationId);
    com.nhat.workflowhub.membership.entity.Membership viewerMembership = new com.nhat.workflowhub.membership.entity.Membership();
    viewerMembership.setId(UUID.randomUUID());
    viewerMembership.setOrganizationId(organizationId);
    viewerMembership.setWorkspaceId(null);
    viewerMembership.setUserId(userId);
    viewerMembership.setRole(com.nhat.workflowhub.auth.entity.UserRole.VIEWER);
    viewerMembership.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    viewerMembership.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

    when(jwtService.isValidAccessToken("valid-token")).thenReturn(true);
    when(jwtService.extractUserId("valid-token")).thenReturn(userId);
    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
    when(organizationService.requireAccessibleOrganization("acme", userId)).thenReturn(organization);
    when(membershipRepository.findAllByOrganizationIdAndUserId(organizationId, userId)).thenReturn(List.of(viewerMembership));

    mockMvc.perform(post("/api/organizations/acme/workflow-items")
            .with(csrf())
            .header("Authorization", "Bearer valid-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": "Payment is broken",
                  "description": "Customer cannot pay"
                }
                """))
        .andExpect(status().isForbidden());
  }

  @Test
  void getWorkflowItem_returnsNotFoundForMissingItem() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID workflowItemId = UUID.randomUUID();

    UserAccount user = activeUser(userId);
    Organization organization = organization(userId, organizationId);

    when(jwtService.isValidAccessToken("valid-token")).thenReturn(true);
    when(jwtService.extractUserId("valid-token")).thenReturn(userId);
    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
    when(organizationService.requireAccessibleOrganization("acme", userId)).thenReturn(organization);
    when(workflowItemRepository.findByIdAndOrganizationId(workflowItemId, organizationId)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/organizations/acme/workflow-items/{workflowItemId}", workflowItemId)
            .header("Authorization", "Bearer valid-token"))
        .andExpect(status().isNotFound());
  }

  @Test
  void listWorkflowEvents_returnsAuditTrail() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID workflowItemId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();

    UserAccount user = activeUser(userId);
    Organization organization = organization(userId, organizationId);
    WorkflowItem item = workflowItem(workflowItemId, organizationId, UUID.randomUUID(), userId, "Payment is broken");

    WorkflowEvent event = new WorkflowEvent();
    event.setId(eventId);
    event.setWorkflowItemId(workflowItemId);
    event.setOrganizationId(organizationId);
    event.setWorkspaceId(item.getWorkspaceId());
    event.setEventType("CREATED");
    event.setOldValue(null);
    event.setNewValue("{\"title\":\"Payment is broken\"}");
    event.setActorUserId(userId);
    event.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

    when(jwtService.isValidAccessToken("valid-token")).thenReturn(true);
    when(jwtService.extractUserId("valid-token")).thenReturn(userId);
    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
    when(organizationService.requireAccessibleOrganization("acme", userId)).thenReturn(organization);
    when(workflowItemRepository.findByIdAndOrganizationId(workflowItemId, organizationId)).thenReturn(Optional.of(item));
    when(workflowEventRepository.findAllByWorkflowItemIdOrderByCreatedAtDesc(workflowItemId)).thenReturn(List.of(event));

    mockMvc.perform(get("/api/organizations/acme/workflow-items/{workflowItemId}/events", workflowItemId)
            .header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(eventId.toString()))
        .andExpect(jsonPath("$[0].eventType").value("CREATED"));
  }

  private UserAccount activeUser(UUID userId) {
    UserAccount user = new UserAccount();
    user.setId(userId);
    user.setEmail("nhat@example.com");
    user.setFullName("Nhat");
    user.setPasswordHash("hashed");
    user.setStatus(UserStatus.ACTIVE);
    user.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    return user;
  }

  private Organization organization(UUID userId, UUID organizationId) {
    Organization organization = new Organization();
    organization.setId(organizationId);
    organization.setSlug("acme");
    organization.setName("Acme");
    organization.setOwnerUserId(userId);
    return organization;
  }

  private WorkflowItem workflowItem(UUID workflowItemId, UUID organizationId, UUID workspaceId, UUID userId, String title) {
    WorkflowItem item = new WorkflowItem();
    item.setId(workflowItemId);
    item.setOrganizationId(organizationId);
    item.setWorkspaceId(workspaceId);
    item.setCreatedByUserId(userId);
    item.setTitle(title);
    item.setDescription("Customer cannot pay");
    item.setStatus(WorkflowStatus.NEW);
    item.setPriority(WorkflowPriority.MEDIUM);
    item.setAssigneeUserId(null);
    item.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    item.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    return item;
  }
}
