package com.gentics.cailun.core.data.service.transformation.content;

import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Consumer;

import org.neo4j.graphdb.Transaction;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.service.transformation.TransformationInfo;
import com.gentics.cailun.core.rest.content.response.ContentResponse;
import com.gentics.cailun.core.rest.tag.response.TagResponse;

public class ContentTraversalConsumer implements Consumer<Content> {

	private TransformationInfo info;
	private int currentDepth;
	private TagResponse restTag;
	private Set<ForkJoinTask<Void>> tasks;

	public ContentTraversalConsumer(TransformationInfo info, int currentDepth, TagResponse restTag, Set<ForkJoinTask<Void>> tasks) {
		this.info = info;
		this.currentDepth = currentDepth;
		this.restTag = restTag;
		this.tasks = tasks;
	}

	@Override
	public void accept(Content content) {
		try (Transaction tx = info.getGraphDb().beginTx()) {
			String currentUuid = content.getUuid();
			info.getRoutingContext()
					.session()
					.hasPermission(
							new CaiLunPermission(content, PermissionType.READ).toString(),
							handler -> {
								try (Transaction tx2 = info.getGraphDb().beginTx()) {

									if (handler.result()) {
										Content loadedContent = info.getNeo4jTemplate().fetch(content);
										ContentResponse currentRestContent = (ContentResponse) info.getObject(currentUuid);
										if (currentRestContent == null) {
											currentRestContent = new ContentResponse();
											ContentTransformationTask subTask = new ContentTransformationTask(loadedContent, info,
													currentRestContent, currentDepth + 1);
											tasks.add(subTask.fork());
										}
										restTag.getContents().add(currentRestContent);

									}
									tx2.success();
								}
							});
			tx.success();
		}

	}
}