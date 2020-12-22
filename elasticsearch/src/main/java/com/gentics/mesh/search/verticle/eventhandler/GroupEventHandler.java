package com.gentics.mesh.search.verticle.eventhandler;

import static com.gentics.mesh.core.rest.MeshEvent.GROUP_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_UPDATED;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;

import io.reactivex.Flowable;

/**
 * Search index handler for group events.
 */
@Singleton
public class GroupEventHandler implements EventHandler {
	private final MeshHelper helper;
	private final MeshEntities entities;
	private final ComplianceMode complianceMode;

	@Inject
	public GroupEventHandler(MeshHelper helper, MeshEntities entities, MeshOptions options) {
		this.helper = helper;
		this.entities = entities;
		this.complianceMode = options.getSearchOptions().getComplianceMode();
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(GROUP_CREATED, GROUP_UPDATED, GROUP_DELETED);
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		return Flowable.defer(() -> {
			MeshEvent event = messageEvent.event;
			MeshElementEventModel model = requireType(MeshElementEventModel.class, messageEvent.message);
			if (event == GROUP_CREATED || event == GROUP_UPDATED) {
				return helper.getDb().tx(tx -> {
					// We also need to update all users of the group
					Optional<HibGroup> groupOptional = entities.group.getElement(model);
					GroupDaoWrapper groupDao = tx.groupDao();

					return Stream.concat(
						groupOptional.stream().map(entities::createRequest),
						groupOptional.stream().flatMap(group -> groupDao.getUsers(group).stream()).map(entities::createRequest))
						.collect(Util.toFlowable());
				});
			} else if (event == GROUP_DELETED) {
				// TODO Update users that were part of that group.
				// At the moment we cannot look up users that were in the group if the group is already deleted.
				return Flowable.just(helper.deleteDocumentRequest(Group.composeIndexName(), model.getUuid(), complianceMode));
			} else {
				throw new RuntimeException("Unexpected event " + event.address);
			}
		});
	}
}
