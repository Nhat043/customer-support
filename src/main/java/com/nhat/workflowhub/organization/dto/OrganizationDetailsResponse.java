package com.nhat.workflowhub.organization.dto;

import com.nhat.workflowhub.membership.dto.MembershipResponse;
import com.nhat.workflowhub.workspace.dto.WorkspaceResponse;
import java.util.List;

public record OrganizationDetailsResponse(
    OrganizationResponse organization,
    List<WorkspaceResponse> workspaces,
    List<MembershipResponse> memberships
) {
}
