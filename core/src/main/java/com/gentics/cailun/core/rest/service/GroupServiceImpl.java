package com.gentics.cailun.core.rest.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.rest.model.auth.Group;
import com.gentics.cailun.core.rest.service.generic.GenericNodeServiceImpl;

@Component
@Transactional
public class GroupServiceImpl extends GenericNodeServiceImpl<Group> implements GroupService {

}
