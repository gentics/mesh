package com.gentics.mesh.core.data.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;
import rx.Single;

@Component
public class WebRootService {

	private static Logger log = LoggerFactory.getLogger(WebRootService.class);

	/**
	 * Find the element that corresponds to the given project webroot path.
	 * 
	 * @param ac
	 * @param path
	 * @return
	 */
	public Single<Path> findByProjectPath(InternalActionContext ac, String path) {
		Project project = ac.getProject();
		Node baseNode = project.getBaseNode();
		Path nodePath = new Path();
		nodePath.setTargetPath(path);

		// Handle path to project root (baseNode) 
		if ("/".equals(path) || path.isEmpty()) {
			nodePath.addSegment(new PathSegment(baseNode, null, null));
			return Single.just(nodePath);
		}

		// Prepare the stack which we use for resolving
		String sanitizedPath = path.replaceAll("^/+", "");
		String[] elements = sanitizedPath.split("\\/");
		List<String> list = Arrays.asList(elements);
		Stack<String> stack = new Stack<String>();
		Collections.reverse(list);
		stack.addAll(list);

		// Traverse the graph and buildup the result path while doing so
		Single<Path> obsNode = baseNode.resolvePath(ac.getRelease(null).getUuid(), ContainerType.forVersion(ac.getVersioningParameters().getVersion()),
				nodePath, stack);
		return obsNode;
	}

}
