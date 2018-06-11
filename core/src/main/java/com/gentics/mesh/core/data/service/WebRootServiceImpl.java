package com.gentics.mesh.core.data.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;

@Singleton
public class WebRootServiceImpl implements WebRootService {

	@Inject
	public Database database;

	@Inject
	public WebRootServiceImpl() {
	}

	@Override
	public Path findByProjectPath(InternalActionContext ac, String path) {
		Project project = ac.getProject();

		// First try to locate the content via the url path index
		ContainerType type = ContainerType.forVersion(ac.getVersioningParameters().getVersion());
		NodeGraphFieldContainer containerByWebUrlPath = findByPath(ac.getBranch().getUuid(), path, type);
		if (containerByWebUrlPath != null) {
			return containerByWebUrlPath.getPath(ac);
		}

		// Locating did not yield a result. Lets try the regular segmentpath info.
		Path nodePath = new Path();
		Node baseNode = project.getBaseNode();
		nodePath.setTargetPath(path);

		// Handle path to project root (baseNode)
		if ("/".equals(path) || path.isEmpty()) {
			// TODO Why this container? Any other container would also be fine?
			Iterator<? extends NodeGraphFieldContainer> it = baseNode.getDraftGraphFieldContainers().iterator();
			NodeGraphFieldContainer container = it.next();
			nodePath.addSegment(new PathSegment(container, null, null));
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
		return baseNode.resolvePath(ac.getBranch().getUuid(), ContainerType.forVersion(ac.getVersioningParameters().getVersion()), nodePath, stack);
	}

	@Override
	public NodeGraphFieldContainer findByPath(String branchUuid, String path, ContainerType type) {

		String fieldKey = NodeGraphFieldContainer.WEBROOT_URLFIELD_PROPERTY_KEY;
		if (type == ContainerType.PUBLISHED) {
			fieldKey = NodeGraphFieldContainer.PUBLISHED_WEBROOT_URLFIELD_PROPERTY_KEY;
		}

		// Prefix each path with the branchuuid in order to scope the paths by branch
		String key = branchUuid + path;
		return database.findVertex(fieldKey, key, NodeGraphFieldContainerImpl.class);
	}

}
