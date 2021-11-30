package com.gentics.mesh.core.data.root;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;

import java.util.function.Predicate;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicNonTransformablePageImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.rest.project.ProjectResponse;
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
	 * Remove the project from the aggregation vertex.
	 * 
	 * @param project
	 */
	void removeProject(Project project);

	/**
	 * Return a page with all projects.
	 * 
	 * @param ac
	 *            Action context to be used to handle permissions checks
	 * @param pagingInfo
	 *            Paging information
	 * @param extraFilter
	 *            Additional filter to be applied
	 * @return Page with loaded projects
	 */
	default Page<? extends HibProject> findAllWrapped(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibProject> extraFilter) {
		return new DynamicNonTransformablePageImpl<Project>(ac.getUser(), this, pagingInfo, READ_PERM, project -> {
			return extraFilter.test(project);
		}, true);
	}

	/**
	 * Add given the project to the aggregation vertex.
	 * 
	 * @param project
	 */
	void addProject(Project project);

	/**
	 * Return the sub etag for the project.
	 * 
	 * @param project
	 * @param ac
	 * @return
	 */
	String getSubETag(Project project, InternalActionContext ac);
}
