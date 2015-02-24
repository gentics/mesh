package com.gentics.cailun.core.repository;

import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.repository.generic.GenericNodeRepository;

public interface RoleRepository extends GenericNodeRepository<Role> {

	Role findByName(String string);

}
