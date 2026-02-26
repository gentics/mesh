package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.Arrays;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.data.dao.PersistingProjectDao;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.dao.TagFamilyDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.PermissionType;
import com.gentics.mesh.hibernate.data.domain.HibProjectImpl;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;

import dagger.Lazy;
import io.vertx.core.Vertx;

/**
 * Project DAO implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class ProjectDaoImpl extends AbstractHibDaoGlobal<HibProject, ProjectResponse, HibProjectImpl> implements PersistingProjectDao {

	public static final String[] SORT_FIELDS = new String[] { "name" };
	private static final Logger log = LoggerFactory.getLogger(ProjectDaoImpl.class);

	@Inject
	public ProjectDaoImpl(DaoHelper<HibProject, HibProjectImpl> daoHelper, HibPermissionRoots permissionRoots,
			CommonDaoHelper commonDaoHelper, CurrentTransaction currentTransaction, EventFactory eventFactory,
			Lazy<Vertx> vertx) {
		super(daoHelper, permissionRoots, commonDaoHelper, currentTransaction, eventFactory, vertx);
	}

	@Override
	public Result<? extends HibNode> findNodes(HibProject project) {
		return HibernateTx.get().nodeDao().findAll(project);
	}

	@Override
	public HibBaseElement getTagFamilyPermissionRoot(HibProject project) {
		return permissionRoots.buildPermissionRootWithParent(project, PermissionType.TAG_FAMILY);
	}

	@Override
	public HibBaseElement getSchemaContainerPermissionRoot(HibProject project) {
		return permissionRoots.buildPermissionRootWithParent(project, PermissionType.SCHEMA);
	}

	@Override
	public HibBaseElement getMicroschemaContainerPermissionRoot(HibProject project) {
		return permissionRoots.buildPermissionRootWithParent(project, PermissionType.MICROSCHEMA);
	}

	@Override
	public HibBaseElement getBranchPermissionRoot(HibProject project) {
		return permissionRoots.buildPermissionRootWithParent(project, PermissionType.BRANCH);
	}

	@Override
	public HibBaseElement getNodePermissionRoot(HibProject project) {
		return permissionRoots.buildPermissionRootWithParent(project, PermissionType.NODE);
	}

	@Override
	public void delete(HibProject project) {
		if (log.isDebugEnabled()) {
			log.debug("Deleting project {" + project.getName() + "}");
		}
		NodeDaoImpl nodeDao = HibernateTx.get().nodeDao();
		SchemaDao schemaDao = CommonTx.get().schemaDao();
		MicroschemaDao microschemaDao = CommonTx.get().microschemaDao();
		TagFamilyDao tagFamilyDao = CommonTx.get().tagFamilyDao();
		BranchDao branchDao = CommonTx.get().branchDao();
		JobDaoImpl jobDao = HibernateTx.get().jobDao();

		// Remove the tagfamilies from the index
		tagFamilyDao.onRootDeleted(project);

		// Remove all the nodes
		HibernateTx.get().batch().add(nodeDao.onDeleted(project.getBaseNode()));
		project.setBaseNode(null);
		nodeDao.deleteAllFromProject(project);
		// Since we are about to do bulk deletions, we want to first flush all pending operations
		// and then removing everything from the persistence context (except for the project). This is because hibernate
		// doesn't know how to reflect hql deletion to the persistence context.
		// https://thorben-janssen.com/hibernate-tips-remove-entities-persistence-context/
		em().flush();
		em().clear();
		project = em().merge(project);

		// Unassign the schemas from the container
		for (HibSchema container : project.getSchemas().list()) {
			schemaDao.unassign(container, project, HibernateTx.get().batch());
		}

		// Unassign the microschemas from the container
		for (HibMicroschema container : project.getMicroschemas().list()) {
			microschemaDao.unassign(container, project, HibernateTx.get().batch());
		}

		// Remove the project schema root from the index
		schemaDao.onRootDeleted(project);

		// Remove the branch root and all branches
		branchDao.onRootDeleted(project);

		// Remove the project from the index
		HibernateTx.get().batch().add(project.onDeleted());

		// Remove all jobs referencing project
		jobDao.deleteByProject(project);

		// Remove the project node
		deletePersisted(project);

		CommonTx.get().data().maybeGetBulkActionContext().ifPresent(bac -> bac.process(true));
	}

	@Override
	public HibBaseElement resolveToElement(HibBaseElement permissionRoot, HibBaseElement root, Stack<String> stack) {
		if (stack.isEmpty()) {
			return permissionRoot;
		} else {
			String uuidOrNameSegment = stack.pop();

			// Try to locate the project by name first.
			HibProject project = findByUuid(uuidOrNameSegment);
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
				HibBaseElement projectSpecificPermissionRoot = permissionRoots.buildPermissionRootWithParent(project, nestedRootNode);
				switch (nestedRootNode) {
				case PermissionRoots.BRANCHES:
					return currentTransaction.getTx().branchDao().resolveToElement(projectSpecificPermissionRoot, project, stack);
				case PermissionRoots.TAG_FAMILIES:
					return currentTransaction.getTx().tagFamilyDao().resolveToElement(projectSpecificPermissionRoot, project, stack);
				case PermissionRoots.SCHEMAS:
					return currentTransaction.getTx().schemaDao().resolveToElement(projectSpecificPermissionRoot, project, stack);
				case PermissionRoots.MICROSCHEMAS:
					return currentTransaction.getTx().microschemaDao().resolveToElement(projectSpecificPermissionRoot, project, stack);
				case PermissionRoots.NODES:
					return currentTransaction.getTx().nodeDao().resolveToElement(projectSpecificPermissionRoot, project, stack);
				default:
					throw error(NOT_FOUND, "Unknown project element {" + nestedRootNode + "}");
				}
			}
		}
	}

	@Override
	public HibProject findByName(String name) {
		return HibernateTx.get().data().mesh().projectNameCache().get(name, pName -> {
			return super.findByName(pName);
		});
	}

	@Override
	public Result<? extends HibLanguage> findLanguages(HibProject project) {
		return project.getLanguages();
	}

	@Override
	public String[] getGraphQlSortingFieldNames(boolean noDependencies) {
		return Stream.of(
				Arrays.stream(super.getGraphQlSortingFieldNames(noDependencies)),
				Arrays.stream(SORT_FIELDS)					
			).flatMap(Function.identity()).toArray(String[]::new);
	}
}
