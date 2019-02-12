package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.request.DeleteDocumentRequest;
import com.gentics.mesh.search.verticle.request.ElasticsearchRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gentics.mesh.core.rest.MeshEvent.GROUP_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_UPDATED;

@Singleton
public class GroupHandler implements EventHandler {
	private final MeshHelper helper;
	private final BootstrapInitializer boot;
	private final MeshEntities entities;

	@Inject
	public GroupHandler(MeshHelper helper, BootstrapInitializer boot, MeshEntities entities) {
		this.helper = helper;
		this.boot = boot;
		this.entities = entities;
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(GROUP_CREATED, GROUP_UPDATED, GROUP_DELETED);
	}

	@Override
	public List<ElasticsearchRequest> handle(MessageEvent messageEvent) {
		MeshEvent event = messageEvent.event;
		if (event == GROUP_CREATED || event == GROUP_UPDATED) {
			return helper.getDb().tx(() -> {
				// We also need to update all users of the group
				Group group = boot.groupRoot().findByUuid(messageEvent.message.getUuid());
				return Stream.concat(
					Stream.of(entities.createRequest(group)),
					group.getUsers().stream().map(entities::createRequest)
				).collect(Collectors.toList());
			});
		} else if (event == GROUP_DELETED) {
			return Collections.singletonList(new DeleteDocumentRequest(helper.prefixIndexName(Group.composeIndexName()), messageEvent.message.getUuid()));
		} else {
			throw new RuntimeException("Unexpected event " + event.address);
		}
	}
}
