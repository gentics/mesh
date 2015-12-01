package com.gentics.mesh.core.data.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

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
	public Observable<Path> findByProjectPath(InternalActionContext ac, String path) {
		Project project = ac.getProject();
		Node baseNode = project.getBaseNode();
		Path nodePath = new Path();
		nodePath.setTargetPath(path);

		// Handle path to project root (baseNode) 
		if ("/".equals(path)) {
			nodePath.addSegment(new PathSegment(baseNode, false, null));
			return Observable.just(nodePath);
		}

		// Prepare the stack which we use for resolving
		String sanitizedPath = path.replaceAll("^/+", "");
		String[] elements = sanitizedPath.split("\\/");
		List<String> list = Arrays.asList(elements);
		Stack<String> stack = new Stack<String>();
		Collections.reverse(list);
		stack.addAll(list);

		// Traverse the graph and buildup the result path while doing so
		Observable<Path> obsNode = baseNode.resolvePath(nodePath, stack);
		return obsNode;
	}

}
