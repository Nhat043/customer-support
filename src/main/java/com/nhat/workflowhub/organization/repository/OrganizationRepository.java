package com.nhat.workflowhub.organization.repository;

import com.nhat.workflowhub.organization.entity.Organization;
import java.util.Optional;

public interface OrganizationRepository {

  Optional<Organization> findBySlug(String slug);
}
