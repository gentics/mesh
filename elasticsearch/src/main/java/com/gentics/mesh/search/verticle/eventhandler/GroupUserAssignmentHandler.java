package com.gentics.mesh.search.verticle.eventhandler;

import static com.gentics.mesh.core.rest.MeshEvent.GROUP_USER_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_USER_UNASSIGNED;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;

import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;

import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.group.GroupUserAssignModel;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;

import io.reactivex.Flowable;

/**
 * Search event handler for group<->user assignment events.
 */
public class GroupUserAssignmentHandler implements EventHandler {

	private final MeshHelper helper;
	private final MeshEntities entities;

	@Inject
	public GroupUserAssignmentHandler(MeshHelper helper, MeshEntities entities) {
		this.helper = helper;
		this.entities = entities;
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(GROUP_USER_ASSIGNED, GROUP_USER_UNASSIGNED);
	}

	@Override
	public Flowable<? extends SearchRequest> handle(MessageEvent messageEvent) {
		return Flowable.defer(() -> {
			GroupUserAssignModel model = requireType(GroupUserAssignModel.class, messageEvent.message);
			return Flowable.just(helper.getDb().tx(() -> {
				HibUser user = helper.getBoot().userDao().findByUuid(model.getUser().getUuid());
				return entities.createRequest(user);
			}));
		});
	}

}
