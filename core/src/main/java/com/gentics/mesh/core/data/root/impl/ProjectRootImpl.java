package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_PROJECT;

import java.util.Stack;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class ProjectRootImpl extends AbstractRootVertex<Project>implements ProjectRoot {

	@Override
	protected Class<? extends Project> getPersistanceClass() {
		return ProjectImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_PROJECT;
	}

	@Override
	public void addProject(Project project) {
		addItem(project);
	}

	@Override
	public void removeProject(Project project) {
		removeItem(project);
	}

	// TODO unique

	@Override
	public Project create(String name, User creator) {
		Project project = getGraph().addFramedVertex(ProjectImpl.class);
		project.setName(name);
		project.getNodeRoot();
		project.createBaseNode(creator);

		project.setCreator(creator);
		project.setCreationTimestamp(System.currentTimeMillis());
		project.setEditor(creator);
		project.setLastEditedTimestamp(System.currentTimeMillis());

		project.getTagRoot();
		project.getSchemaContainerRoot();
		project.getTagFamilyRoot();

		addItem(project);

		return project;
	}

	@Override
	public void resolveToElement(Stack<String> stack, Handler<AsyncResult<? extends MeshVertex>> resultHandler) {
		if (stack.isEmpty()) {
			resultHandler.handle(Future.succeededFuture(this));
		} else {
			String uuidSegment = stack.pop();
			findByUuid(uuidSegment, rh -> {
				if (rh.succeeded()) {
					Project project = rh.result();
					if (stack.isEmpty()) {
						resultHandler.handle(Future.succeededFuture(project));
					} else {
						String nestedRootNode = stack.pop();
						switch (nestedRootNode) {
						case TagFamilyRoot.TYPE:
							TagFamilyRoot tagFamilyRoot = project.getTagFamilyRoot();
							tagFamilyRoot.resolveToElement(stack, resultHandler);
							break;
						case SchemaContainerRoot.TYPE:
							SchemaContainerRoot schemaRoot = project.getSchemaContainerRoot();
							schemaRoot.resolveToElement(stack, resultHandler);
							break;
						case MicroschemaContainerRoot.TYPE:
							// MicroschemaContainerRoot microschemaRoot = project.get
							// project.getMicroschemaRoot();
							throw new NotImplementedException();
							// break;
						case NodeRoot.TYPE:
							NodeRoot nodeRoot = project.getNodeRoot();
							nodeRoot.resolveToElement(stack, resultHandler);
							break;
						case TagRoot.TYPE:
							TagRoot tagRoot = project.getTagRoot();
							tagRoot.resolveToElement(stack, resultHandler);
							return;
						default:
							resultHandler.handle(Future.failedFuture("Unknown project element {" + nestedRootNode + "}"));
							return;
						}
					}
				} else {
					resultHandler.handle(Future.failedFuture(rh.cause()));
					return;
				}
			});
		}
	}

	@Override
	public void delete() {
		throw new NotImplementedException("The project root should never be deleted.");
	}
}
