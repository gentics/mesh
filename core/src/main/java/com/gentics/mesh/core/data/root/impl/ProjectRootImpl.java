package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PROJECT;
import static com.gentics.mesh.core.rest.error.HttpConflictErrorException.conflict;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.errorObservable;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import java.util.Stack;

import javax.naming.InvalidNameException;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

/**
 * Project Root Node domain model implementation.
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
public class ProjectRootImpl extends AbstractRootVertex<Project> implements ProjectRoot {

	private static final Logger log = LoggerFactory.getLogger(ProjectRootImpl.class);

	public static void checkIndices(Database database) {
		database.addVertexType(ProjectRootImpl.class);
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

	// TODO unique

	@Override
	public Project create(String name, User creator) {
		Project project = getGraph().addFramedVertex(ProjectImpl.class);
		project.setName(name);
		project.getNodeRoot();
		project.createBaseNode(creator);

		project.setCreated(creator);
		project.getSchemaContainerRoot();
		project.getTagFamilyRoot();

		addItem(project);

		return project;
	}

	@Override
	public Observable<? extends MeshVertex> resolveToElement(Stack<String> stack) {
		if (stack.isEmpty()) {
			return Observable.just(this);
		} else {
			String uuidSegment = stack.pop();
			return findByUuid(uuidSegment).flatMap(project -> {
				if (stack.isEmpty()) {
					return Observable.just(project);
				} else {
					String nestedRootNode = stack.pop();
					switch (nestedRootNode) {
					case TagFamilyRoot.TYPE:
						TagFamilyRoot tagFamilyRoot = project.getTagFamilyRoot();
						return tagFamilyRoot.resolveToElement(stack);
					case SchemaContainerRoot.TYPE:
						SchemaContainerRoot schemaRoot = project.getSchemaContainerRoot();
						return schemaRoot.resolveToElement(stack);
					case MicroschemaContainerRoot.TYPE:
						// MicroschemaContainerRoot microschemaRoot =
						// project.get
						// project.getMicroschemaRoot();
						throw new NotImplementedException();
						// break;
					case NodeRoot.TYPE:
						NodeRoot nodeRoot = project.getNodeRoot();
						return nodeRoot.resolveToElement(stack);
					default:
						return Observable.error(new Exception("Unknown project element {" + nestedRootNode + "}"));
					}
				}
			});
		}

	}

	@Override
	public void delete() {
		throw new NotImplementedException("The project root should never be deleted.");
	}

	@Override
	public Observable<Project> create(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();
		RouterStorage routerStorage = RouterStorage.getRouterStorage();
		BootstrapInitializer boot = BootstrapInitializer.getBoot();

		// TODO also create a default object schema for the project. Move this into service class
		// ObjectSchema defaultContentSchema = objectSchemaRoot.findByName(, name)
		ProjectCreateRequest requestModel = ac.fromJson(ProjectCreateRequest.class);
		String projectName = requestModel.getName();
		MeshAuthUser requestUser = ac.getUser();

		if (StringUtils.isEmpty(requestModel.getName())) {
			return errorObservable(BAD_REQUEST, "project_missing_name");
		}
		return db.noTrx(() -> {
			if (!requestUser.hasPermissionSync(ac, boot.projectRoot(), CREATE_PERM)) {
				throw error(FORBIDDEN, "error_missing_perm", boot.projectRoot().getUuid());
			}
			// TODO instead of this check, a constraint in the db should be added
			Project conflictingProject = boot.projectRoot().findByName(requestModel.getName()).toBlocking().single();
			if (conflictingProject != null) {
				throw conflict(conflictingProject.getUuid(), projectName, "project_conflicting_name");
			}
			Tuple<SearchQueueBatch, Project> tuple = db.trx(() -> {
				requestUser.reload();
				Project project = create(requestModel.getName(), requestUser);

				requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, project);
				requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, project.getBaseNode());
				requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, project.getTagFamilyRoot());
				requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, project.getSchemaContainerRoot());
				// TODO add microschema root crud perms
				requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, project.getNodeRoot());

				SearchQueueBatch batch = project.addIndexBatch(SearchQueueEntryAction.CREATE_ACTION);
				return Tuple.tuple(batch, project);
			});

			Project project = tuple.v2();
			SearchQueueBatch batch = tuple.v1();

			try {
				//TODO BUG project should only be added to router when trx and ES finished successfully
				routerStorage.addProjectRouter(project.getName());
				if (log.isInfoEnabled()) {
					log.info("Registered project {" + project.getName() + "}");
				}
			} catch (InvalidNameException e) {
				// TODO should we really fail here?
				return Observable.error(error(BAD_REQUEST, "Error while adding project to router storage", e));
			}

			return batch.process().map(t -> project);

		});
	}

}
