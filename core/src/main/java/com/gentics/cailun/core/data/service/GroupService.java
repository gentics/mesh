package com.gentics.cailun.core.data.service;

import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;
import com.gentics.cailun.core.rest.response.RestGroup;

public interface GroupService extends GenericNodeService<Group> {

	public Group findByName(String name);

	public Group findByUUID(String uuid);

	public RestGroup transformToRest(Group group);

}
