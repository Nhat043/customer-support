package com.nhat.workflowhub.membership.controller;

import com.nhat.workflowhub.auth.entity.UserRole;
import com.nhat.workflowhub.auth.security.AuthenticatedUser;
import com.nhat.workflowhub.membership.dto.MembershipResponse;
import com.nhat.workflowhub.membership.dto.UpsertMembershipRequest;
import com.nhat.workflowhub.membership.service.MembershipService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations/{orgSlug}/memberships")
public class MembershipController {

  private final MembershipService membershipService;

  public MembershipController(MembershipService membershipService) {
    this.membershipService = membershipService;
  }

  @GetMapping
  public List<MembershipResponse> list(@PathVariable("orgSlug") String orgSlug, Authentication authentication) {
    return membershipService.listMemberships(orgSlug, currentUserId(authentication));
  }

  @PostMapping
  public MembershipResponse add(
      @PathVariable("orgSlug") String orgSlug,
      @Valid @RequestBody UpsertMembershipRequest request,
      Authentication authentication
  ) {
    return membershipService.addMember(orgSlug, request, currentUserId(authentication));
  }

  @PatchMapping("/{membershipId}/role/{role}")
  public MembershipResponse updateRole(
      @PathVariable("orgSlug") String orgSlug,
      @PathVariable("membershipId") UUID membershipId,
      @PathVariable("role") UserRole role,
      Authentication authentication
  ) {
    return membershipService.updateRole(orgSlug, membershipId, role, currentUserId(authentication));
  }

  @DeleteMapping("/{membershipId}")
  public ResponseEntity<Void> remove(
      @PathVariable("orgSlug") String orgSlug,
      @PathVariable("membershipId") UUID membershipId,
      Authentication authentication
  ) {
    membershipService.removeMember(orgSlug, membershipId, currentUserId(authentication));
    return ResponseEntity.noContent().build();
  }

  private UUID currentUserId(Authentication authentication) {
    return ((AuthenticatedUser) authentication.getPrincipal()).userId();
  }
}
