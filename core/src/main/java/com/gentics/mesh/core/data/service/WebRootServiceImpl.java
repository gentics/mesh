package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.core.data.GraphFieldContainerEdge.WEBROOT_URLFIELD_INDEX_NAME;
import static com.gentics.mesh.util.URIUtils.decodeSegment;

import java.util.Iterator;
import java.util.Stack;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.WebrootPathCache;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.webroot.PathPrefixUtil;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class WebRootServiceImpl implements WebRootService {

	private static final Logger log = LoggerFactory.getLogger(WebRootServiceImpl.class);

	private final Database database;

	private final WebrootPathCache pathStore;

	@Inject
	public WebRootServiceImpl(Database database, WebrootPathCache pathStore) {
		this.database = database;
		this.pathStore = pathStore;
	}

	@Override
	public Path findByProjectPath(InternalActionContext ac, String path, ContainerType type) {
		HibProject project = ac.getProject();
		HibBranch branch = ac.getBranch();

		Path cachedPath = pathStore.getPath(project, branch, type, path);
		if (cachedPath != null) {
			return cachedPath;
		}

		// First try to locate the content via the url path index (niceurl)

		// Check whether the path contains the branch path prefix. Return an empty node path in those cases. (e.g. Node was not found)
		if (!PathPrefixUtil.startsWithPrefix(branch, path)) {
			Path nodePath = new Path();
			nodePath.setTargetPath(path);
			nodePath.setInitialStack(new Stack<>());
			nodePath.setPrefixMismatch(true);
			pathStore.store(project, branch, type, path, nodePath);
			return nodePath;
		}

		String strippedPath = PathPrefixUtil.strip(branch, path);
		NodeGraphFieldContainer containerByWebUrlPath = findByUrlFieldPath(branch.getUuid(), strippedPath, type);
		if (containerByWebUrlPath != null) {
			Path resolvedPath = containerByWebUrlPath.getPath(ac);
			pathStore.store(project, branch, type, path, resolvedPath);
			return resolvedPath;
		}

		// Locating did not yield a result. Lets try the regular segment path info.
		Path nodePath = new Path();
		Node baseNode = project.getBaseNode();
		nodePath.setTargetPath(strippedPath);
		Stack<String> stack = new Stack<>();

		// Handle path to project root (baseNode)
		if ("/".equals(strippedPath) || strippedPath.isEmpty()) {
			// TODO Why this container? Any other container would also be fine?
			Iterator<? extends NodeGraphFieldContainer> it = baseNode.getDraftGraphFieldContainers().iterator();
			NodeGraphFieldContainer container = it.next();
			nodePath.addSegment(new PathSegment(container, null, null, "/"));
			stack.push("/");
			nodePath.setInitialStack(stack);
			pathStore.store(project, branch, type, path, nodePath);
			return nodePath;
		}

		// Prepare the stack which we use for resolving
		String sanitizedPath = strippedPath.replaceAll("^/+", "");
		String[] elements = sanitizedPath.split("\\/");

		IntStream.iterate(elements.length - 1, i -> i - 1)
			.limit(elements.length)
			.mapToObj(i -> decodeSegment(elements[i]))
			.forEach(stack::add);

		Object clone = stack.clone();
		if (clone instanceof Stack) {
			nodePath.setInitialStack((Stack<String>) clone);
		}

		// Traverse the graph and buildup the result path while doing so
		Path resolvedPath = baseNode.resolvePath(ac.getBranch().getUuid(), type, nodePath, stack);
		pathStore.store(project, branch, type, path, nodePath);
		return resolvedPath;
	}

	@Override
	public NodeGraphFieldContainer findByUrlFieldPath(String branchUuid, String path, ContainerType type) {
		Object key = GraphFieldContainerEdgeImpl.composeWebrootUrlFieldIndexKey(database, path, branchUuid, type);
		GraphFieldContainerEdge edge = database.findEdge(WEBROOT_URLFIELD_INDEX_NAME, key, GraphFieldContainerEdgeImpl.class);
		if (edge != null) {
			return edge.getNodeContainer();
		} else {
			return null;
		}
	}

}
