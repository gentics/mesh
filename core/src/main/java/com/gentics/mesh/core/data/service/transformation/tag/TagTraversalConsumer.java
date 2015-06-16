package com.gentics.mesh.core.data.service.transformation.tag;

import static com.gentics.mesh.core.data.model.relationship.Permission.READ_PERM;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Session;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Consumer;

import com.gentics.mesh.auth.MeshPermission;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.data.service.transformation.UuidRestModelComparator;
import com.gentics.mesh.core.rest.node.response.NodeResponse;
import com.gentics.mesh.core.rest.tag.response.TagResponse;

public class TagTraversalConsumer implements Consumer<Tag> {

	private TransformationInfo info;
	private int currentDepth;
	private NodeResponse tagContainer;
	private Set<ForkJoinTask<Void>> tasks;

	public TagTraversalConsumer(TransformationInfo info, int currentDepth, NodeResponse tagContainer, Set<ForkJoinTask<Void>> tasks) {
		this.info = info;
		this.currentDepth = currentDepth;
		this.tagContainer = tagContainer;
		this.tasks = tasks;
	}

	@Override
	public void accept(Tag tag) {
		String currentUuid = tag.getUuid();
		Session session = info.getRoutingContext().session();
		User user = info.getRoutingContext().user();
		user.isAuthorised(new MeshPermission(tag, READ_PERM).toString(), handler -> {
			if (handler.result()) {
//				try (Transaction tx = info.getGraphDb().beginTx()) {
//					Tag loadedTag = info.getNeo4jTemplate().fetch(tag);
					TagResponse currentRestTag = (TagResponse) info.getObject(currentUuid);
					if (currentRestTag == null) {
						currentRestTag = new TagResponse();
						/* info.addTag(currentUuid, currentRestTag); */
						TagTransformationTask subTask = new TagTransformationTask(tag, info, currentRestTag, currentDepth + 1);
						tasks.add(subTask.fork());

//						tx.success();
					}
					tagContainer.getTags().add(currentRestTag);
//				}
			}
		});
		Collections.sort(tagContainer.getTags(), new UuidRestModelComparator<TagResponse>());

	}

}
