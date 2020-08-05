package com.gentics.mesh.search.verticle.eventhandler;

import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_UPDATED;
import static com.gentics.mesh.search.verticle.entity.MeshEntities.findElementByUuidStream;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;
import static com.gentics.mesh.util.StreamUtil.toStream;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshProjectElementEventModel;
import com.gentics.mesh.core.rest.event.ProjectEvent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;

import io.reactivex.Flowable;

@Singleton
public class TagFamilyEventHandler implements EventHandler {
	private final MeshHelper helper;
	private final MeshEntities entities;
	private final ComplianceMode complianceMode;

	@Inject
	public TagFamilyEventHandler(MeshHelper helper, MeshEntities entities, MeshOptions options) {
		this.helper = helper;
		this.entities = entities;
		this.complianceMode = options.getSearchOptions().getComplianceMode();
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(TAG_FAMILY_CREATED, TAG_FAMILY_UPDATED, TAG_FAMILY_DELETED);
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		return Flowable.defer(() -> {
			MeshEvent event = messageEvent.event;
			MeshProjectElementEventModel model = requireType(MeshProjectElementEventModel.class, messageEvent.message);
			String projectUuid = Util.requireType(ProjectEvent.class, messageEvent.message).getProject().getUuid();

			if (event == TAG_FAMILY_CREATED || event == TAG_FAMILY_UPDATED) {
				return helper.getDb().tx(() -> {
					// We also need to update all tags of this family
					Optional<TagFamily> tagFamily = entities.tagFamily.getElement(model);

					Stream<SearchRequest> tagFamilyUpdate = toStream(tagFamily)
						.map(tf -> entities.createRequest(tf, projectUuid));

					Stream<SearchRequest> tagUpdates = toStream(tagFamily)
						.flatMap(tf -> tf.findAll().stream())
						.map(t -> entities.createRequest(t, projectUuid));

					Stream<SearchRequest> nodeUpdates = toStream(tagFamily).flatMap(tf -> createNodeUpdates(model, tf));

					return Util.concat(tagFamilyUpdate, tagUpdates, nodeUpdates).collect(Util.toFlowable());
				});
			} else if (event == TAG_FAMILY_DELETED) {
				// We can omit the update of related elements for project deletion causes. The project handler will take care of removing the index
				if (EventCauseHelper.isProjectDeleteCause(messageEvent.message)) {
					return Flowable.empty();
				} else {
					// TODO Update related elements.
					// At the moment we cannot look up related elements, because the element was already deleted.
					return Flowable.just(helper.deleteDocumentRequest(TagFamily.composeIndexName(projectUuid), model.getUuid(), complianceMode));
				}
			} else {
				throw new RuntimeException("Unexpected event " + event.address);
			}
		});
	}

	/**
	 * Find all nodes that have been tagged by tags of the given tag family and create a stream of update requests.
	 * 
	 * @param model
	 * @param tagFamily
	 * @return
	 */
	private Stream<SearchRequest> createNodeUpdates(MeshProjectElementEventModel model, TagFamily tagFamily) {
		return findElementByUuidStream(Tx.get().data().projectDao(), model.getProject().getUuid())
			.flatMap(project -> project.getBranchRoot().findAll().stream()
				.flatMap(branch -> tagFamily.findAll().stream()
					.flatMap(tag -> tag.getNodes(branch).stream())
					.flatMap(node -> entities.generateNodeRequests(node.getUuid(), project, branch))));
	}
}
