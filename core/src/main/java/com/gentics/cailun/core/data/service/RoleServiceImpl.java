package com.gentics.cailun.core.data.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.service.generic.GenericNodeServiceImpl;

@Component
@Transactional
public class RoleServiceImpl extends GenericNodeServiceImpl<Role> implements RoleService {

}
