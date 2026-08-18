package com.nhat.workflowhub.organization.service;

import com.nhat.workflowhub.auth.entity.UserAccount;
import com.nhat.workflowhub.auth.entity.UserRole;
import com.nhat.workflowhub.auth.repository.UserAccountRepository;
import com.nhat.workflowhub.common.exception.ApiException;
import com.nhat.workflowhub.membership.dto.MembershipResponse;
import com.nhat.workflowhub.membership.entity.Membership;
import com.nhat.workflowhub.membership.repository.MembershipRepository;
import com.nhat.workflowhub.organization.dto.CreateOrganizationRequest;
import com.nhat.workflowhub.organization.dto.OrganizationDetailsResponse;
import com.nhat.workflowhub.organization.dto.OrganizationResponse;
import com.nhat.workflowhub.organization.entity.Organization;
import com.nhat.workflowhub.organization.repository.OrganizationRepository;
import com.nhat.workflowhub.workspace.dto.WorkspaceResponse;
import com.nhat.workflowhub.workspace.entity.Workspace;
import com.nhat.workflowhub.workspace.repository.WorkspaceRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class OrganizationService {

  private static final String DEFAULT_WORKSPACE_NAME = "General";
  private static final String DEFAULT_WORKSPACE_SLUG = "general";

  private final OrganizationRepository organizationRepository;
  private final WorkspaceRepository workspaceRepository;
  private final MembershipRepository membershipRepository;
  private final UserAccountRepository userAccountRepository;

  public OrganizationService(
      OrganizationRepository organizationRepository,
      WorkspaceRepository workspaceRepository,
      MembershipRepository membershipRepository,
      UserAccountRepository userAccountRepository
  ) {
    this.organizationRepository = organizationRepository;
    this.workspaceRepository = workspaceRepository;
    this.membershipRepository = membershipRepository;
    this.userAccountRepository = userAccountRepository;
  }

  @Transactional(readOnly = true)
  public List<OrganizationResponse> listMyOrganizations(UUID userId) {
    Map<UUID, Organization> uniqueOrganizations = new LinkedHashMap<>();

    organizationRepository.findAllByOwnerUserId(userId)
        .forEach(organization -> uniqueOrganizations.put(organization.getId(), organization));

    membershipRepository.findAllByUserId(userId).stream()
        .map(Membership::getOrganizationId)
        .distinct()
        .flatMap(organizationId -> organizationRepository.findById(organizationId).stream())
        .forEach(organization -> uniqueOrganizations.putIfAbsent(organization.getId(), organization));

    return uniqueOrganizations.values().stream()
        .map(this::toResponse)
        .toList();
  }

  public OrganizationDetailsResponse createOrganization(CreateOrganizationRequest request, UUID currentUserId) {
    requireUserExists(currentUserId);

    String slug = normalizeSlug(request.slug(), request.name());
    if (organizationRepository.existsBySlug(slug)) {
      throw new ApiException(HttpStatus.CONFLICT, "Organization slug already exists");
    }

    Organization organization = new Organization();
    organization.setId(UUID.randomUUID());
    organization.setName(request.name().trim());
    organization.setSlug(slug);
    organization.setOwnerUserId(currentUserId);
    organizationRepository.save(organization);

    Workspace defaultWorkspace = new Workspace();
    defaultWorkspace.setId(UUID.randomUUID());
    defaultWorkspace.setOrganizationId(organization.getId());
    defaultWorkspace.setName(DEFAULT_WORKSPACE_NAME);
    defaultWorkspace.setSlug(DEFAULT_WORKSPACE_SLUG);
    workspaceRepository.save(defaultWorkspace);

    Membership ownerMembership = new Membership();
    ownerMembership.setId(UUID.randomUUID());
    ownerMembership.setOrganizationId(organization.getId());
    ownerMembership.setWorkspaceId(null);
    ownerMembership.setUserId(currentUserId);
    ownerMembership.setRole(UserRole.OWNER);
    membershipRepository.save(ownerMembership);

    return toDetails(organization);
  }

  @Transactional(readOnly = true)
  public OrganizationDetailsResponse getOrganizationDetails(String slug, UUID currentUserId) {
    Organization organization = requireAccessibleOrganization(slug, currentUserId);
    return toDetails(organization);
  }

  @Transactional(readOnly = true)
  public Organization requireAccessibleOrganization(String slug, UUID currentUserId) {
    Organization organization = organizationRepository.findBySlug(slug)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organization not found"));

    if (isOwner(organization, currentUserId) || membershipRepository.existsByOrganizationIdAndUserId(organization.getId(), currentUserId)) {
      return organization;
    }

    throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this organization");
  }

  @Transactional(readOnly = true)
  public boolean canManageOrganization(Organization organization, UUID currentUserId) {
    if (isOwner(organization, currentUserId)) {
      return true;
    }

    return membershipRepository.findByOrganizationIdAndWorkspaceIdIsNullAndUserId(organization.getId(), currentUserId)
        .map(membership -> membership.getRole() == UserRole.OWNER || membership.getRole() == UserRole.ADMIN)
        .orElse(false);
  }

  @Transactional(readOnly = true)
  public OrganizationResponse toResponse(Organization organization) {
    return new OrganizationResponse(
        organization.getId(),
        organization.getName(),
        organization.getSlug(),
        organization.getOwnerUserId()
    );
  }

  @Transactional(readOnly = true)
  public List<WorkspaceResponse> listWorkspaces(UUID organizationId) {
    return workspaceRepository.findAllByOrganizationId(organizationId).stream()
        .map(this::toWorkspaceResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<MembershipResponse> listMemberships(UUID organizationId) {
    return membershipRepository.findAllByOrganizationId(organizationId).stream()
        .map(this::toMembershipResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public OrganizationDetailsResponse toDetails(Organization organization) {
    return new OrganizationDetailsResponse(
        toResponse(organization),
        listWorkspaces(organization.getId()),
        listMemberships(organization.getId())
    );
  }

  @Transactional(readOnly = true)
  public WorkspaceResponse toWorkspaceResponse(Workspace workspace) {
    return new WorkspaceResponse(
        workspace.getId(),
        workspace.getOrganizationId(),
        workspace.getName(),
        workspace.getSlug()
    );
  }

  @Transactional(readOnly = true)
  public MembershipResponse toMembershipResponse(Membership membership) {
    UserAccount user = userAccountRepository.findById(membership.getUserId()).orElse(null);
    return new MembershipResponse(
        membership.getId(),
        membership.getOrganizationId(),
        membership.getWorkspaceId(),
        membership.getUserId(),
        user == null ? null : user.getEmail(),
        user == null ? null : user.getFullName(),
        membership.getRole(),
        membership.getCreatedAt()
    );
  }

  private boolean isOwner(Organization organization, UUID userId) {
    return organization.getOwnerUserId().equals(userId);
  }

  private void requireUserExists(UUID userId) {
    if (userAccountRepository.findById(userId).isEmpty()) {
      throw new ApiException(HttpStatus.NOT_FOUND, "User not found");
    }
  }

  private String normalizeSlug(String explicitSlug, String fallbackName) {
    String source = StringUtils.hasText(explicitSlug) ? explicitSlug : fallbackName;
    String slug = source.trim().toLowerCase()
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("^-+|-+$", "");
    if (!StringUtils.hasText(slug)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Slug cannot be empty");
    }
    return slug;
  }
}
