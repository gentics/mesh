package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PROJECT;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import java.util.Stack;

import javax.naming.InvalidNameException;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.error.NameConflictException;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.index.NodeIndexHandler;
import com.gentics.mesh.search.index.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.TagIndexHandler;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

/**
 * @see ProjectRoot
 */
public class ProjectRootImpl extends AbstractRootVertex<Project> implements ProjectRoot {

	private static final Logger log = LoggerFactory.getLogger(ProjectRootImpl.class);

	public static void checkIndices(Database database) {
		database.addVertexType(ProjectRootImpl.class, MeshVertexImpl.class);
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
	public Project create(String name, User creator, SchemaContainerVersion schemaContainerVersion) {
		Project project = getGraph().addFramedVertex(ProjectImpl.class);
		project.setName(name);
		project.getNodeRoot();
		project.getReleaseRoot().create(name, creator).setMigrated(true);

		// Assign the provided schema container to the project
		project.getSchemaContainerRoot().addItem(schemaContainerVersion.getSchemaContainer());
		project.getLatestRelease().assignSchemaVersion(schemaContainerVersion);
		project.createBaseNode(creator, schemaContainerVersion);

		project.setCreated(creator);
		project.setEditor(creator);
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
	public void delete(SearchQueueBatch batch) {
		throw new NotImplementedException("The project root should never be deleted.");
	}

	@Override
	public Observable<Project> create(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();
		RouterStorage routerStorage = RouterStorage.getIntance();
		BootstrapInitializer boot = BootstrapInitializer.getBoot();

		// TODO also create a default object schema for the project. Move this into service class
		// ObjectSchema defaultContentSchema = objectSchemaRoot.findByName(, name)
		ProjectCreateRequest requestModel = ac.fromJson(ProjectCreateRequest.class);
		String projectName = requestModel.getName();
		MeshAuthUser requestUser = ac.getUser();

		if (StringUtils.isEmpty(requestModel.getName())) {
			throw error(BAD_REQUEST, "project_missing_name");
		}
		return db.noTrx(() -> {
			if (!requestUser.hasPermissionSync(ac, boot.projectRoot(), CREATE_PERM)) {
				throw error(FORBIDDEN, "error_missing_perm", boot.projectRoot().getUuid());
			}
			// TODO instead of this check, a constraint in the db should be added
			Project conflictingProject = boot.projectRoot().findByName(requestModel.getName()).toBlocking().single();
			if (conflictingProject != null) {
				throw new NameConflictException("project_conflicting_name", projectName, conflictingProject.getUuid());
			}
			if (requestModel.getSchemaReference() == null || !requestModel.getSchemaReference().isSet()) {
				throw error(BAD_REQUEST, "project_error_no_schema_reference");
			}
			SchemaContainerVersion schemaContainerVersion = BootstrapInitializer.getBoot().schemaContainerRoot()
					.fromReference(requestModel.getSchemaReference()).toBlocking().single();
			
			Tuple<SearchQueueBatch, Project> tuple = db.trx(() -> {
				requestUser.reload();
				Project project = create(requestModel.getName(), requestUser, schemaContainerVersion);

				requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, project);
				requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, project.getBaseNode());
				requestUser.addPermissionsOnRole(this, CREATE_PERM, project.getBaseNode(), READ_PUBLISHED_PERM, PUBLISH_PERM);
				requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, project.getTagFamilyRoot());
				requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, project.getSchemaContainerRoot());
				// TODO add microschema root crud perms
				requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, project.getNodeRoot());

				SearchQueueBatch batch = project.createIndexBatch(STORE_ACTION);

				Release initialRelease = project.getInitialRelease();
				NodeIndexHandler nodeIndexHandler = NodeIndexHandler.getInstance();
				TagIndexHandler tagIndexHandler = TagIndexHandler.getInstance();
				TagFamilyIndexHandler tagFamilyIndexHandler = TagFamilyIndexHandler.getInstance();
				batch.addEntry(nodeIndexHandler.getIndexName(project.getUuid(), initialRelease.getUuid(), "draft"), Node.TYPE,
						SearchQueueEntryAction.CREATE_INDEX);
				batch.addEntry(nodeIndexHandler.getIndexName(project.getUuid(), initialRelease.getUuid(), "published"), Node.TYPE,
						SearchQueueEntryAction.CREATE_INDEX);
				batch.addEntry(tagIndexHandler.getIndexName(project.getUuid()), Tag.TYPE, SearchQueueEntryAction.CREATE_INDEX);
				batch.addEntry(tagFamilyIndexHandler.getIndexName(project.getUuid()), TagFamily.TYPE, SearchQueueEntryAction.CREATE_INDEX);

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
