package com.gentics.mesh.core.data.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;

@Singleton
public class WebRootService {

	@Inject
	public WebRootService() {
	}

	/**
	 * Find the element that corresponds to the given project webroot path.
	 * 
	 * @param ac
	 *            Action context
	 * @param path
	 *            Path string
	 * @return Resolved path object
	 */
	public Path findByProjectPath(InternalActionContext ac, String path) {
		Project project = ac.getProject();
		Node baseNode = project.getBaseNode();
		Path nodePath = new Path();
		nodePath.setTargetPath(path);

		// Handle path to project root (baseNode)
		if ("/".equals(path) || path.isEmpty()) {
			nodePath.addSegment(new PathSegment(baseNode, null, null));
			return nodePath;
		}

		// Prepare the stack which we use for resolving
		String sanitizedPath = path.replaceAll("^/+", "");
		String[] elements = sanitizedPath.split("\\/");
		List<String> list = Arrays.asList(elements);
		Stack<String> stack = new Stack<String>();
		Collections.reverse(list);
		stack.addAll(list);

		// Traverse the graph and buildup the result path while doing so
		return baseNode.resolvePath(ac.getRelease(null).getUuid(),
				ContainerType.forVersion(ac.getVersioningParameters().getVersion()), nodePath, stack);
	}

}
