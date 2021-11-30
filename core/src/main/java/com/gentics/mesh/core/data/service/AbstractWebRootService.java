package com.gentics.mesh.core.data.service;

import java.util.Iterator;
import java.util.Stack;

import com.gentics.mesh.cache.WebrootPathCache;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.webroot.PathPrefixUtil;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.impl.PathImpl;
import com.gentics.mesh.path.impl.PathSegmentImpl;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.URIUtils;

public abstract class AbstractWebRootService implements WebRootService {

	protected final Database database;

	protected final WebrootPathCache pathStore;

	public AbstractWebRootService(Database database, WebrootPathCache pathStore) {
		this.database = database;
		this.pathStore = pathStore;
	}

	@Override
	public Path findByProjectPath(InternalActionContext ac, String path, ContainerType type) {
		Tx tx = Tx.get();
		NodeDao nodeDao = tx.nodeDao();
		ContentDao contentDao = tx.contentDao();
		HibProject project = tx.getProject(ac);
		HibBranch branch = tx.getBranch(ac);

		Path cachedPath = pathStore.getPath(project, branch, type, path);
		if (cachedPath != null) {
			return cachedPath;
		}

		// First try to locate the content via the url path index (niceurl)

		// Check whether the path contains the branch path prefix. Return an empty node path in those cases. (e.g. Node was not found)
		if (!PathPrefixUtil.startsWithPrefix(branch, path)) {
			Path nodePath = new PathImpl();
			nodePath.setTargetPath(path);
			nodePath.setInitialStack(new Stack<>());
			nodePath.setPrefixMismatch(true);
			pathStore.store(project, branch, type, path, nodePath);
			return nodePath;
		}

		String strippedPath = PathPrefixUtil.strip(branch, path);
		HibNodeFieldContainer containerByWebUrlPath = findByUrlFieldPath(branch.getUuid(), strippedPath, type);
		if (containerByWebUrlPath != null) {
			Path resolvedPath = containerByWebUrlPath.getPath(ac);
			pathStore.store(project, branch, type, path, resolvedPath);
			return resolvedPath;
		}

		// Locating did not yield a result. Lets try the regular segment path info.
		Path nodePath = new PathImpl();
		HibNode baseNode = project.getBaseNode();
		nodePath.setTargetPath(strippedPath);
		Stack<String> stack = new Stack<>();

		// Handle path to project root (baseNode)
		if ("/".equals(strippedPath) || strippedPath.isEmpty()) {
			// TODO Why this container? Any other container would also be fine?
			Iterator<HibNodeFieldContainer> it = contentDao.getDraftFieldContainers(baseNode).iterator();
			HibNodeFieldContainer container = it.next();
			nodePath.addSegment(new PathSegmentImpl(container, null, null, "/"));
			stack.push("/");
			nodePath.setInitialStack(stack);
			pathStore.store(project, branch, type, path, nodePath);
			return nodePath;
		}

		// Prepare the stack which we use for resolving
		String sanitizedPath = strippedPath.replaceAll("^/+", "");
		String[] elements = sanitizedPath.split("\\/");

		StreamUtil.reverseOf(elements)
			.map(URIUtils::decodeSegment)
			.forEach(stack::add);

		Object clone = stack.clone();
		if (clone instanceof Stack) {
			nodePath.setInitialStack((Stack<String>) clone);
		}

		// Traverse the graph and buildup the result path while doing so
		Path resolvedPath = nodeDao.resolvePath(baseNode, tx.getBranch(ac).getUuid(), type, nodePath, stack);
		pathStore.store(project, branch, type, path, nodePath);
		return resolvedPath;
	}
}
