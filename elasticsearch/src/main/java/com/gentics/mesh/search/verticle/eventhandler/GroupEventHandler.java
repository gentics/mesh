package com.gentics.mesh.search.verticle.eventhandler;

import static com.gentics.mesh.core.rest.MeshEvent.GROUP_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_UPDATED;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;
import static com.gentics.mesh.util.StreamUtil.toStream;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;
import com.gentics.mesh.util.RxUtil;

import io.reactivex.Flowable;

@Singleton
public class GroupEventHandler implements EventHandler {
	private final MeshHelper helper;
	private final MeshEntities entities;
	private final ComplianceMode complianceMode;

	@Inject
	public GroupEventHandler(MeshHelper helper, MeshEntities entities, MeshOptions options) {
		this.helper = helper;
		this.entities = entities;
		this.complianceMode  = options.getSearchOptions().getComplianceMode();
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(GROUP_CREATED, GROUP_UPDATED, GROUP_DELETED);
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		MeshEvent event = messageEvent.event;
		MeshElementEventModel model = requireType(MeshElementEventModel.class, messageEvent.message);
		if (event == GROUP_CREATED || event == GROUP_UPDATED) {
			return helper.getDb().singleTxImmediate(() -> {
				// We also need to update all users of the group
				Optional<Group> groupOptional = entities.group.getElement(model);

				return Stream.concat(
					toStream(groupOptional).map(entities::createRequest),
					toStream(groupOptional).flatMap(group -> group.getUsers().stream()).map(entities::createRequest)
				).collect(Util.toFlowable());
			}).flatMapPublisher(RxUtil.identity());
		} else if (event == GROUP_DELETED) {
			// TODO Update users that were part of that group.
			// At the moment we cannot look up users that were in the group if the group is already deleted.
			return Flowable.just(helper.deleteDocumentRequest(Group.composeIndexName(), model.getUuid(), complianceMode));
		} else {
			throw new RuntimeException("Unexpected event " + event.address);
		}
	}
}
