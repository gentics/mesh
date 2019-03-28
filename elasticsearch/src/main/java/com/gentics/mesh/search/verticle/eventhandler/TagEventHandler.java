package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshProjectElementEventModel;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;
import io.reactivex.Flowable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import static com.gentics.mesh.core.rest.MeshEvent.TAG_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_UPDATED;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toStream;

@Singleton
public class TagEventHandler implements EventHandler {
	private final MeshHelper helper;
	private final MeshEntities entities;

	@Inject
	public TagEventHandler(MeshHelper helper, MeshEntities entities) {
		this.helper = helper;
		this.entities = entities;
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(TAG_CREATED, TAG_UPDATED, TAG_DELETED);
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		return Flowable.defer(() -> {
			MeshEvent event = messageEvent.event;
			MeshProjectElementEventModel model = requireType(MeshProjectElementEventModel.class, messageEvent.message);
			String projectUuid = model.getProject().getUuid();

			if (event == TAG_CREATED || event == TAG_UPDATED) {
				return helper.getDb().tx(() -> {
					// We also need to update the tag family
					Optional<Tag> tag = entities.tag.getElement(model);
					Optional<TagFamily> tagFamily = tag.map(Tag::getTagFamily);

					return Stream.concat(
						toStream(tag).map(t -> entities.createRequest(t, projectUuid)),
						toStream(tagFamily).map(tf -> entities.createRequest(tf, projectUuid))).collect(Util.toFlowable());
				});
			} else if (event == TAG_DELETED) {
				// TODO Update related elements.
				// The tag was deleted via a project deletion. The project handler takes care of deleting the tag index.
				if (EventCauseHelper.isProjectDeleteCause(model)) {
					return Flowable.empty();
				} else {
					// At the moment we cannot look up related elements, because the element was already deleted.
					return Flowable.just(helper.deleteDocumentRequest(Tag.composeIndexName(projectUuid), model.getUuid()));
				}

			} else {
				throw new RuntimeException("Unexpected event " + event.address);
			}
		});
	}
}
