package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PROJECT;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
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
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.root.MicroschemaRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;

/**
 * @see ProjectRoot
 */
public class ProjectRootImpl extends AbstractRootVertex<Project> implements ProjectRoot {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
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
	public long globalCount() {
		return db().count(ProjectImpl.class);
	}

	@Override
	public Project findByName(String name) {
		HibProject project = mesh().projectNameCache().get(name, n -> {
			return super.findByName(n);
		});
		return toGraph(project);
	}

	@Override
	public HibBaseElement resolveToElement(Stack<String> stack) {
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
				case PermissionRoots.BRANCHES:
					BranchRoot branchRoot = project.getBranchRoot();
					return branchRoot.resolveToElement(stack);
				case PermissionRoots.TAG_FAMILIES:
					TagFamilyRoot tagFamilyRoot = project.getTagFamilyRoot();
					return tagFamilyRoot.resolveToElement(stack);
				case PermissionRoots.SCHEMAS:
					SchemaRoot schemaRoot = project.getSchemaContainerRoot();
					return schemaRoot.resolveToElement(stack);
				case PermissionRoots.MICROSCHEMAS:
					MicroschemaRoot microschemaRoot = project.getMicroschemaContainerRoot();
					return microschemaRoot.resolveToElement(stack);
				case PermissionRoots.NODES:
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
	public String getSubETag(Project project, InternalActionContext ac) {
		return String.valueOf(project.getLastEditedTimestamp());
	}

	@Override
	public Project create() {
		Project project = getGraph().addFramedVertex(ProjectImpl.class);
		project.generateBucketId();
		return project;
	}

}
