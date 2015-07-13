package com.gentics.mesh.core.data.service.transformation.node;

import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Consumer;

import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.service.transformation.TransformationParameters;
import com.gentics.mesh.core.rest.tag.TagResponse;

public class NodeTraversalConsumer implements Consumer<NodeImpl> {

	private TransformationParameters info;
	private int currentDepth;
	private TagResponse restTag;
	private Set<ForkJoinTask<Void>> tasks;

	public NodeTraversalConsumer(TransformationParameters info, int currentDepth, TagResponse restTag, Set<ForkJoinTask<Void>> tasks) {
		this.info = info;
		this.currentDepth = currentDepth;
		this.restTag = restTag;
		this.tasks = tasks;
	}

	@Override
	public void accept(NodeImpl content) {
//		try (Transaction tx = info.getGraphDb().beginTx()) {
//			String currentUuid = content.getUuid();
//			info.getRoutingContext()
//					.session()
//					.hasPermission(
//							new MeshPermission(content, PermissionType.READ).toString(),
//							handler -> {
//								try (Transaction tx2 = info.getGraphDb().beginTx()) {
//
//									if (handler.result()) {
//										Content loadedContent = info.getNeo4jTemplate().fetch(content);
//										ContentResponse currentRestContent = (ContentResponse) info.getObject(currentUuid);
//										if (currentRestContent == null) {
//											currentRestContent = new ContentResponse();
//											ContentTransformationTask subTask = new ContentTransformationTask(loadedContent, info,
//													currentRestContent, currentDepth + 1);
//											tasks.add(subTask.fork());
//										}
//										restTag.getContents().add(currentRestContent);
//
//									}
//									tx2.success();
//								}
//							});
//			tx.success();
//		}

	}
}