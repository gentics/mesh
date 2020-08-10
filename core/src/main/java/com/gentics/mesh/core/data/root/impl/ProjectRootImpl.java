package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PROJECT;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.Stack;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.root.MicroschemaRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * @see ProjectRoot
 */
public class ProjectRootImpl extends AbstractRootVertex<Project> implements ProjectRoot {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(ProjectRootImpl.class, MeshVertexImpl.class);
		type.createType(edgeType(HAS_PROJECT));
		index.createIndex(edgeIndex(HAS_PROJECT).withInOut().withOut());
	}

	@Override
	public Class<? extends Project> getPersistanceClass() {
		return ProjectImpl.class;
	}

	@Override
	public String getRootLabel() {
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

	@Override
	public Project findByName(String name) {
		return mesh().projectNameCache().get(name, n -> {
			return super.findByName(n);
		});
	}

	@Override
	public MeshVertex resolveToElement(Stack<String> stack) {
		if (stack.isEmpty()) {
			return this;
		} else {
			String uuidOrNameSegment = stack.pop();

			// Try to locate the project by name first.
			Project project = findByUuid(uuidOrNameSegment);
			if (project == null) {
				// Fallback to locate the project by name instead
				project = findByName(uuidOrNameSegment);
			}
			if (project == null) {
				return null;
			}

			if (stack.isEmpty()) {
				return project;
			} else {
				String nestedRootNode = stack.pop();
				switch (nestedRootNode) {
				case BranchRoot.TYPE:
					BranchRoot branchRoot = project.getBranchRoot();
					return branchRoot.resolveToElement(stack);
				case TagFamilyRoot.TYPE:
					TagFamilyRoot tagFamilyRoot = project.getTagFamilyRoot();
					return tagFamilyRoot.resolveToElement(stack);
				case SchemaRoot.TYPE:
					SchemaRoot schemaRoot = project.getSchemaContainerRoot();
					return schemaRoot.resolveToElement(stack);
				case MicroschemaRoot.TYPE:
					MicroschemaRoot microschemaRoot = project.getMicroschemaContainerRoot();
					return microschemaRoot.resolveToElement(stack);
				case NodeRoot.TYPE:
					NodeRoot nodeRoot = project.getNodeRoot();
					return nodeRoot.resolveToElement(stack);
				default:
					throw error(NOT_FOUND, "Unknown project element {" + nestedRootNode + "}");
				}
			}
		}

	}

	@Override
	public void delete(BulkActionContext bac) {
		throw new NotImplementedException("The project root should never be deleted.");
	}

	@Override
	public Project create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		throw new RuntimeException("Wrong invocation. Use dao instead.");
	}

	@Override
	public Project create(String projectName, String hostname, Boolean ssl, String pathPrefix, User creator,
		SchemaVersion schemaVersion, String uuid, EventQueueBatch batch) {
		throw new RuntimeException("Wrong invocation. Use dao instead.");
	}

	@Override
	public ProjectResponse transformToRestSync(Project element, InternalActionContext ac, int level, String... languageTags) {
		throw new RuntimeException("Wrong invocation. Use dao instead.");
	}

	@Override
	public String getSubETag(Project project, InternalActionContext ac) {
		return String.valueOf(project.getLastEditedTimestamp());
	}

	@Override
	public Project create() {
		return getGraph().addFramedVertex(ProjectImpl.class);
	}

}
