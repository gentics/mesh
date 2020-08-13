package com.gentics.mesh.core.data.root;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;

import java.util.function.Predicate;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicNonTransformablePageImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Project Root Node domain model interface.
 * 
 * <pre>
* {@code
* 	(pr:ProjectRootImpl)-[r1:HAS_PROJECT]->(p1:ProjectImpl)
* 	(pr-[r2:HAS_PROJECT]->(p2:ProjectImpl)
 	(pr)-[r3:HAS_PROJECT]->(p3:ProjectImpl)
 * 	(mr:MeshRootImpl)-[r:HAS_PROJECT_ROOT]->(pr)
* }
 * </pre>
 *
 * <p>
 * <img src="http://getmesh.io/docs/javadoc/cypher/com.gentics.mesh.core.data.root.impl.ProjectRootImpl.jpg" alt="">
 * </p>
 */
public interface ProjectRoot extends RootVertex<Project>, TransformableElementRoot<Project, ProjectResponse> {

	public static final String TYPE = "projects";

	/**
	 * Create a new project with the given name and add it to the aggregation vertex. Assign the provided schema container version to the created initial branch
	 * of the project.
	 * 
	 * @param projectName
	 *            Name of the new project.
	 * @param hostname
	 *            Hostname of the project which will be assigend to the initial branch.
	 * @param ssl
	 *            SSL flag of the project which will be assigned to the initial branch.
	 * @param pathPrefix
	 *            Path prefix which will be assigned to the initial branch.
	 * @param creator
	 *            User that is being used to set the initial creator and editor references.
	 * @param schemaVersion
	 *            Schema container version which will be used to create the basenode
	 * @param batch
	 * @return
	 */
	@Deprecated
	default Project create(String projectName, String hostname, Boolean ssl, String pathPrefix, User creator,
		SchemaVersion schemaVersion, EventQueueBatch batch) {
		return create(projectName, hostname, ssl, pathPrefix, creator, schemaVersion, null, batch);
	}

	/**
	 * Create a new project with the given name and add it to the aggregation vertex. Assign the provided schema container version to the created initial branch
	 * of the project.
	 * 
	 * @param projectName
	 *            Name of the new project.
	 * @param hostname
	 *            Hostname of the project which will be assigned to the initial branch.
	 * @param ssl
	 *            SSL flag of the project which will be assigned to the initial branch.
	 * @param pathPrefix
	 *            Path prefix which will be assigned to the initial branch.
	 * @param creator
	 *            User that is being used to set the initial creator and editor references.
	 * @param schemaVersion
	 *            Schema container version which will be used to create the basenode
	 * @param uuid
	 *            Optional uuid
	 * @param batch
	 * @return
	 */
	Project create(String projectName, String hostname, Boolean ssl, String pathPrefix, User creator, SchemaVersion schemaVersion,
		String uuid, EventQueueBatch batch);

	/**
	 * Remove the project from the aggregation vertex.
	 * 
	 * @param project
	 */
	void removeProject(Project project);

	default	Page<? extends HibProject> findAllWrapped(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibProject> extraFilter) {
		return new DynamicNonTransformablePageImpl<Project>(ac.getUser(), this, pagingInfo, READ_PERM, project ->  { 
			return extraFilter.test(project);
		}, true);
	}

	/**
	 * Add given the project to the aggregation vertex.
	 * 
	 * @param project
	 */
	void addProject(Project project);

	String getSubETag(Project project, InternalActionContext ac);

	Project create();

}
