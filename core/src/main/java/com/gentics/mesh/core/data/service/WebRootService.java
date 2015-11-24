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

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

@Component
public class WebRootService {

	private static Logger log = LoggerFactory.getLogger(WebRootService.class);

	@Autowired
	private BootstrapInitializer boot;

	public Observable<Path> findByProjectPath(InternalActionContext ac, String projectName, String path) {
		Project project = ac.getProject();
		Node baseNode = project.getBaseNode();

		// Prepare the stack which we use for resolving
		path = path.replaceAll("^/+", "");
		String[] elements = path.split("\\/");
		List<String> list = Arrays.asList(elements);
		Stack<String> stack = new Stack<String>();
		Collections.reverse(list);
		stack.addAll(list);

		Path nodePath = new Path();
		nodePath.setTargetPath(path);

		// Traverse the graph and buildup the result path while doing so
		Observable<Path> obsNode = baseNode.resolvePath(nodePath, stack);
		return obsNode;
	}

}
