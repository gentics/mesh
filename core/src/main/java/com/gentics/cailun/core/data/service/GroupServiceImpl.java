package com.gentics.cailun.core.data.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.service.generic.GenericNodeServiceImpl;

@Component
@Transactional
public class GroupServiceImpl extends GenericNodeServiceImpl<Group> implements GroupService {

}
