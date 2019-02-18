package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.ProjectEvent;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.request.DeleteDocumentRequest;
import com.gentics.mesh.search.verticle.request.ElasticsearchRequest;
import io.reactivex.Flowable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_UPDATED;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toStream;

@Singleton
public class TagFamilyHandler implements EventHandler {
	private final MeshHelper helper;
	private final MeshEntities entities;

	@Inject
	public TagFamilyHandler(MeshHelper helper, MeshEntities entities) {
		this.helper = helper;
		this.entities = entities;
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(TAG_FAMILY_CREATED, TAG_FAMILY_UPDATED, TAG_FAMILY_DELETED);
	}

	@Override
	public Flowable<ElasticsearchRequest> handle(MessageEvent messageEvent) {
		MeshEvent event = messageEvent.event;
		String projectUuid = Util.requireType(ProjectEvent.class, messageEvent.message).getProject().getUuid();
		if (event == TAG_FAMILY_CREATED || event == TAG_FAMILY_UPDATED) {
			return helper.getDb().tx(() -> {
				// We also need to update all tags of this family

				Optional<TagFamily> tagFamily = entities.tagFamily.getElement(messageEvent.message);

				return Stream.concat(
					toStream(tagFamily).map(tf -> entities.createRequest(tf, projectUuid)),
					toStream(tagFamily).flatMap(tf -> tf.findAll().stream())
						.map(t -> entities.createRequest(t, projectUuid))
				).collect(Util.toFlowable());
			});
		} else if (event == TAG_FAMILY_DELETED) {
			// TODO Update related elements.
			// At the moment we cannot look up related elements, because the element was already deleted.
			return Flowable.just(new DeleteDocumentRequest(helper.prefixIndexName(TagFamily.composeIndexName(projectUuid)), messageEvent.message.getUuid()));
		} else {
			throw new RuntimeException("Unexpected event " + event.address);
		}
	}
}
