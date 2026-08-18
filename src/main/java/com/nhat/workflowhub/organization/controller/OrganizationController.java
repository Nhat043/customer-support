package com.nhat.workflowhub.organization.controller;

import com.nhat.workflowhub.auth.security.AuthenticatedUser;
import com.nhat.workflowhub.organization.dto.CreateOrganizationRequest;
import com.nhat.workflowhub.organization.dto.OrganizationDetailsResponse;
import com.nhat.workflowhub.organization.dto.OrganizationResponse;
import com.nhat.workflowhub.organization.service.OrganizationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

  private final OrganizationService organizationService;

  public OrganizationController(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  @GetMapping
  public List<OrganizationResponse> list(Authentication authentication) {
    return organizationService.listMyOrganizations(currentUserId(authentication));
  }

  @PostMapping
  public ResponseEntity<OrganizationDetailsResponse> create(
      @Valid @RequestBody CreateOrganizationRequest request,
      Authentication authentication
  ) {
    OrganizationDetailsResponse response = organizationService.createOrganization(request, currentUserId(authentication));
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{slug}")
        .buildAndExpand(response.organization().slug())
        .toUri();
    return ResponseEntity.created(location).body(response);
  }

  @GetMapping("/{slug}")
  public OrganizationDetailsResponse get(
      @org.springframework.web.bind.annotation.PathVariable("slug") String slug,
      Authentication authentication
  ) {
    return organizationService.getOrganizationDetails(slug, currentUserId(authentication));
  }

  private UUID currentUserId(Authentication authentication) {
    return ((AuthenticatedUser) authentication.getPrincipal()).userId();
  }
}
