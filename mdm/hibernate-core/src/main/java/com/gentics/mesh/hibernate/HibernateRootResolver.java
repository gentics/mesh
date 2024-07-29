package com.gentics.mesh.hibernate;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.RootResolver;
import com.gentics.mesh.hibernate.data.dao.HibDaoCollectionImpl;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entity root resolver implementation. Based on entity DAOs and permission roots.
 * 
 * @author plyhun
 *
 */
@Singleton
public class HibernateRootResolver implements RootResolver {
	private static final Logger log = LoggerFactory.getLogger(HibernateRootResolver.class);
	private final HibDaoCollectionImpl daoCollection;
	private final HibPermissionRoots roots;

	@Inject
	public HibernateRootResolver(ProjectDao projectDao, HibPermissionRoots roots, HibDaoCollectionImpl daoCollection) {
		this.roots = roots;
		this.daoCollection = daoCollection;
	}

	@Override
	public HibBaseElement resolvePathToElement(String pathToElement) {
		if (StringUtils.isEmpty(pathToElement)) {
			throw error(BAD_REQUEST, "Could not resolve path. The path must must not be empty or null.");
		}
		if (pathToElement.endsWith("/")) {
			throw error(BAD_REQUEST, "Could not resolve path. The path must not end with a slash.");
		}

		// Prepare the stack which we use for resolving
		String[] elements = pathToElement.split("\\/");
		List<String> list = Arrays.asList(elements);
		Collections.reverse(list);
		Stack<String> stack = new Stack<>();
		stack.addAll(list);

		if (log.isDebugEnabled()) {
			log.debug("Found " + stack.size() + " elements");
			for (String segment : list) {
				log.debug("Segment: " + segment);
			}
		}
		String rootNodeSegment = stack.pop();

		// Check whether the root segment is a project name
		HibProject project = daoCollection.projectDao().findByName(rootNodeSegment);
		if (project != null) {
			return project;
		} else {
			switch (rootNodeSegment) {
			case PermissionRoots.PROJECTS:
				return daoCollection.projectDao().resolveToElement(roots.project(), null, stack);
			case PermissionRoots.USERS:
				return daoCollection.userDao().resolveToElement(roots.user(), null, stack);
			case PermissionRoots.GROUPS:
				return daoCollection.groupDao().resolveToElement(roots.group(), null, stack);
			case PermissionRoots.ROLES:
				return daoCollection.roleDao().resolveToElement(roots.role(), null, stack);
			case PermissionRoots.MICROSCHEMAS:
				return daoCollection.microschemaDao().resolveToElement(roots.microschema(), null, stack);
			case PermissionRoots.SCHEMAS:
				return daoCollection.schemaDao().resolveToElement(roots.schema(), null, stack);
			default:
				// TOOO i18n
				throw error(NOT_FOUND, "Could not resolve given path. Unknown element {" + rootNodeSegment + "}");
			}
		}
	}
}
