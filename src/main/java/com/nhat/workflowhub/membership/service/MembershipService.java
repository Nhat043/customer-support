package com.nhat.workflowhub.membership.service;

import com.nhat.workflowhub.auth.entity.UserAccount;
import com.nhat.workflowhub.auth.entity.UserRole;
import com.nhat.workflowhub.auth.repository.UserAccountRepository;
import com.nhat.workflowhub.common.exception.ApiException;
import com.nhat.workflowhub.membership.dto.MembershipResponse;
import com.nhat.workflowhub.membership.dto.UpsertMembershipRequest;
import com.nhat.workflowhub.membership.entity.Membership;
import com.nhat.workflowhub.membership.repository.MembershipRepository;
import com.nhat.workflowhub.organization.entity.Organization;
import com.nhat.workflowhub.organization.service.OrganizationService;
import com.nhat.workflowhub.workspace.entity.Workspace;
import com.nhat.workflowhub.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class MembershipService {

  private final OrganizationService organizationService;
  private final MembershipRepository membershipRepository;
  private final WorkspaceRepository workspaceRepository;
  private final UserAccountRepository userAccountRepository;

  public MembershipService(
      OrganizationService organizationService,
      MembershipRepository membershipRepository,
      WorkspaceRepository workspaceRepository,
      UserAccountRepository userAccountRepository
  ) {
    this.organizationService = organizationService;
    this.membershipRepository = membershipRepository;
    this.workspaceRepository = workspaceRepository;
    this.userAccountRepository = userAccountRepository;
  }

  @Transactional(readOnly = true)
  public List<MembershipResponse> listMemberships(String organizationSlug, UUID currentUserId) {
    Organization organization = organizationService.requireAccessibleOrganization(organizationSlug, currentUserId);
    return membershipRepository.findAllByOrganizationId(organization.getId()).stream()
        .map(organizationService::toMembershipResponse)
        .toList();
  }

  public MembershipResponse addMember(String organizationSlug, UpsertMembershipRequest request, UUID currentUserId) {
    Organization organization = organizationService.requireAccessibleOrganization(organizationSlug, currentUserId);
    requireManagePermission(organization, currentUserId);

    UserAccount user = userAccountRepository.findByEmail(request.email().trim().toLowerCase())
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

    UUID workspaceId = resolveWorkspaceId(organization.getId(), request.workspaceSlug());
    Membership membership = workspaceId == null
        ? membershipRepository.findByOrganizationIdAndWorkspaceIdIsNullAndUserId(organization.getId(), user.getId())
            .orElseGet(Membership::new)
        : membershipRepository.findByOrganizationIdAndWorkspaceIdAndUserId(organization.getId(), workspaceId, user.getId())
            .orElseGet(Membership::new);

    if (membership.getId() == null) {
      membership.setId(UUID.randomUUID());
      membership.setOrganizationId(organization.getId());
      membership.setWorkspaceId(workspaceId);
      membership.setUserId(user.getId());
    }
    membership.setRole(request.role());
    membershipRepository.save(membership);
    return organizationService.toMembershipResponse(membership);
  }

  public MembershipResponse updateRole(String organizationSlug, UUID membershipId, UserRole role, UUID currentUserId) {
    Organization organization = organizationService.requireAccessibleOrganization(organizationSlug, currentUserId);
    requireManagePermission(organization, currentUserId);

    Membership membership = membershipRepository.findById(membershipId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));
    ensureMembershipBelongsToOrganization(membership, organization.getId());

    membership.setRole(role);
    membershipRepository.save(membership);
    return organizationService.toMembershipResponse(membership);
  }

  public void removeMember(String organizationSlug, UUID membershipId, UUID currentUserId) {
    Organization organization = organizationService.requireAccessibleOrganization(organizationSlug, currentUserId);
    requireManagePermission(organization, currentUserId);

    Membership membership = membershipRepository.findById(membershipId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));
    ensureMembershipBelongsToOrganization(membership, organization.getId());

    if (organization.getOwnerUserId().equals(membership.getUserId()) && membership.getWorkspaceId() == null) {
      throw new ApiException(HttpStatus.FORBIDDEN, "Cannot remove the organization owner");
    }

    membershipRepository.delete(membership);
  }

  private void ensureMembershipBelongsToOrganization(Membership membership, UUID organizationId) {
    if (!organizationId.equals(membership.getOrganizationId())) {
      throw new ApiException(HttpStatus.NOT_FOUND, "Membership not found");
    }
  }

  private void requireManagePermission(Organization organization, UUID currentUserId) {
    if (!organizationService.canManageOrganization(organization, currentUserId)) {
      throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to manage members");
    }
  }

  private UUID resolveWorkspaceId(UUID organizationId, String workspaceSlug) {
    if (!StringUtils.hasText(workspaceSlug)) {
      return null;
    }

    Workspace workspace = workspaceRepository.findByOrganizationIdAndSlug(organizationId, workspaceSlug)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Workspace not found"));
    return workspace.getId();
  }
}
