package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.core.data.search.request.DeleteDocumentRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import io.reactivex.Flowable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import static com.gentics.mesh.core.rest.MeshEvent.GROUP_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_UPDATED;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toStream;

@Singleton
public class GroupHandler implements EventHandler {
	private final MeshHelper helper;
	private final MeshEntities entities;

	@Inject
	public GroupHandler(MeshHelper helper, MeshEntities entities) {
		this.helper = helper;
		this.entities = entities;
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(GROUP_CREATED, GROUP_UPDATED, GROUP_DELETED);
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		MeshEvent event = messageEvent.event;
		if (event == GROUP_CREATED || event == GROUP_UPDATED) {
			return helper.getDb().tx(() -> {
				// We also need to update all users of the group
				Optional<Group> groupOptional = entities.group.getElement(messageEvent.message);

				return Stream.concat(
					toStream(groupOptional).map(entities::createRequest),
					toStream(groupOptional).flatMap(group -> group.getUsers().stream()).map(entities::createRequest)
				).collect(Util.toFlowable());
			});
		} else if (event == GROUP_DELETED) {
			// TODO Update users that were part of that group.
			// At the moment we cannot look up users that were in the group if the group is already deleted.
			return Flowable.just(new DeleteDocumentRequest(helper.prefixIndexName(Group.composeIndexName()), messageEvent.message.getUuid()));
		} else {
			throw new RuntimeException("Unexpected event " + event.address);
		}
	}
}
