package com.gentics.cailun.core.repository;

import com.gentics.cailun.core.repository.generic.GenericNodeRepository;
import com.gentics.cailun.core.rest.model.auth.Role;

public interface RoleRepository extends GenericNodeRepository<Role> {

	Role findByName(String string);

}
