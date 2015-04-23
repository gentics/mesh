package com.gentics.cailun.core.data.service;

import org.springframework.data.domain.Page;

import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;
import com.gentics.cailun.core.rest.group.response.GroupResponse;
import com.gentics.cailun.paging.PagingInfo;

public interface GroupService extends GenericNodeService<Group> {

	public Group findByName(String name);

	public Group findByUUID(String uuid);

	public GroupResponse transformToRest(Group group, int depth);

	public Page<Group> findAllVisible(User requestUser, PagingInfo pagingInfo);

}
