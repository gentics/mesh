package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CHANGELOG_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_GROUP_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_JOB_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LANGUAGE_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PROJECT_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER_ROOT;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.changelog.ChangelogRoot;
import com.gentics.mesh.core.data.changelog.ChangelogRootImpl;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.job.JobRoot;
import com.gentics.mesh.core.data.job.impl.JobRootImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.MicroschemaRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.result.Result;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see MeshRoot
 */
public class MeshRootImpl extends MeshVertexImpl implements MeshRoot {

	private static Logger log = LoggerFactory.getLogger(MeshRootImpl.class);

	private UserRoot userRoot;
	private GroupRoot groupRoot;
	private RoleRoot roleRoot;

	private TagRoot tagRoot;
	private TagFamilyRoot tagFamilyRoot;

	private LanguageRoot languageRoot;
	private ProjectRoot projectRoot;

	private SchemaRoot schemaRoot;
	private MicroschemaRoot microschemaRoot;
	private JobRoot jobRoot;
	private ChangelogRoot changelogRoot;

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(MeshRootImpl.class, MeshVertexImpl.class);
	}

	@Override
	public String getMeshVersion() {
		return property(MESH_VERSION);
	}

	@Override
	public void setMeshVersion(String version) {
		property(MESH_VERSION, version);
	}

	@Override
	public String getDatabaseRevision() {
		return property(MESH_DB_REV);
	}

	@Override
	public void setDatabaseRevision(String rev) {
		property(MESH_DB_REV, rev);
	}

	@Override
	public ChangelogRoot getChangelogRoot() {
		if (changelogRoot == null) {
			db().tx(() -> {
				synchronized (MeshRootImpl.class) {
					if (changelogRoot == null) {
						ChangelogRoot foundChangelogRoot = out(HAS_CHANGELOG_ROOT).nextOrDefaultExplicit(ChangelogRootImpl.class, null);
						if (foundChangelogRoot == null) {
							changelogRoot = getGraph().addFramedVertex(ChangelogRootImpl.class);
							linkOut(changelogRoot, HAS_CHANGELOG_ROOT);
							if (log.isInfoEnabled()) {
								log.info("Created changelog root {" + changelogRoot.getUuid() + "}");
							}
						} else {
							changelogRoot = foundChangelogRoot;
						}
					}
				}
			});
		}
		return changelogRoot;
	}

	@Override
	public JobRoot getJobRoot() {
		if (jobRoot == null) {
			synchronized (MeshRootImpl.class) {
				JobRoot foundJobRoot = out(HAS_JOB_ROOT).nextOrDefaultExplicit(JobRootImpl.class, null);
				if (foundJobRoot == null) {
					jobRoot = getGraph().addFramedVertex(JobRootImpl.class);
					linkOut(jobRoot, HAS_JOB_ROOT);
					if (log.isDebugEnabled()) {
						log.debug("Created job root {" + jobRoot.getUuid() + "}");
					}
				} else {
					jobRoot = foundJobRoot;
				}
			}
		}
		return jobRoot;
	}

	@Override
	public UserRoot getUserRoot() {
		if (userRoot == null) {
			synchronized (MeshRootImpl.class) {
				UserRoot foundUserRoot = out(HAS_USER_ROOT).has(UserRootImpl.class).nextOrDefaultExplicit(UserRootImpl.class, null);
				if (foundUserRoot == null) {
					userRoot = getGraph().addFramedVertex(UserRootImpl.class);
					linkOut(userRoot, HAS_USER_ROOT);
					if (log.isDebugEnabled()) {
						log.debug("Created tag root {" + userRoot.getUuid() + "}");
					}
				} else {
					userRoot = foundUserRoot;
				}
			}
		}
		return userRoot;
	}

	@Override
	public RoleRoot getRoleRoot() {
		if (roleRoot == null) {
			synchronized (MeshRootImpl.class) {
				RoleRoot foundRoleRoot = out(HAS_ROLE_ROOT).has(RoleRootImpl.class).nextOrDefaultExplicit(RoleRootImpl.class, null);
				if (foundRoleRoot == null) {
					roleRoot = getGraph().addFramedVertex(RoleRootImpl.class);
					linkOut(roleRoot, HAS_ROLE_ROOT);
					if (log.isDebugEnabled()) {
						log.debug("Created role root {" + roleRoot.getUuid() + "}");
					}
				} else {
					roleRoot = foundRoleRoot;
				}
			}
		}
		return roleRoot;
	}

	@Override
	public GroupRoot getGroupRoot() {
		if (groupRoot == null) {
			synchronized (MeshRootImpl.class) {
				GroupRoot foundGroupRoot = out(HAS_GROUP_ROOT).nextOrDefaultExplicit(GroupRootImpl.class, null);
				if (foundGroupRoot == null) {
					groupRoot = getGraph().addFramedVertex(GroupRootImpl.class);
					linkOut(groupRoot, HAS_GROUP_ROOT);
					if (log.isDebugEnabled()) {
						log.debug("Created group root {" + groupRoot.getUuid() + "}");
					}
				} else {
					groupRoot = foundGroupRoot;
				}
			}
		}
		return groupRoot;
	}

	@Override
	public TagRoot getTagRoot() {
		if (tagRoot == null) {
			synchronized (MeshRootImpl.class) {
				TagRoot foundTagRoot = out(HAS_TAG_ROOT).nextOrDefaultExplicit(TagRootImpl.class, null);
				if (foundTagRoot == null) {
					tagRoot = getGraph().addFramedVertex(TagRootImpl.class);
					linkOut(tagRoot, HAS_TAG_ROOT);
					if (log.isDebugEnabled()) {
						log.debug("Created tag root {" + tagRoot.getUuid() + "}");
					}
				} else {
					tagRoot = foundTagRoot;
				}
			}
		}
		return tagRoot;
	}

	@Override
	public SchemaRoot getSchemaContainerRoot() {
		if (schemaRoot == null) {
			synchronized (MeshRootImpl.class) {
				SchemaRoot foundSchemaRoot = out(HAS_SCHEMA_ROOT).has(SchemaContainerRootImpl.class).nextOrDefaultExplicit(
					SchemaContainerRootImpl.class, null);
				if (foundSchemaRoot == null) {
					schemaRoot = getGraph().addFramedVertex(SchemaContainerRootImpl.class);
					linkOut(schemaRoot, HAS_SCHEMA_ROOT);
					if (log.isDebugEnabled()) {
						log.debug("Created schema container root {" + schemaRoot.getUuid() + "}");
					}
				} else {
					schemaRoot = foundSchemaRoot;
				}
			}
		}
		return schemaRoot;
	}

	@Override
	public LanguageRoot getLanguageRoot() {
		if (languageRoot == null) {
			synchronized (MeshRootImpl.class) {
				LanguageRoot foundLanguageRoot = out(HAS_LANGUAGE_ROOT).nextOrDefaultExplicit(LanguageRootImpl.class, null);
				if (foundLanguageRoot == null) {
					languageRoot = getGraph().addFramedVertex(LanguageRootImpl.class);
					linkOut(languageRoot, HAS_LANGUAGE_ROOT);
					if (log.isDebugEnabled()) {
						log.debug("Created language root {" + languageRoot.getUuid() + "}");
					}
				} else {
					languageRoot = foundLanguageRoot;
				}
			}
		}
		return languageRoot;
	}

	@Override
	public ProjectRoot getProjectRoot() {
		if (projectRoot == null) {
			synchronized (MeshRootImpl.class) {
				ProjectRoot foundProjectRoot = out(HAS_PROJECT_ROOT).has(ProjectRootImpl.class).nextOrDefaultExplicit(ProjectRootImpl.class, null);
				if (foundProjectRoot == null) {
					projectRoot = getGraph().addFramedVertex(ProjectRootImpl.class);
					linkOut(projectRoot, HAS_PROJECT_ROOT);
					if (log.isDebugEnabled()) {
						log.debug("Created project root {" + projectRoot.getUuid() + "}");
					}
				} else {
					projectRoot = foundProjectRoot;
				}
			}
		}
		return projectRoot;
	}

	@Override
	public TagFamilyRoot getTagFamilyRoot() {
		if (tagFamilyRoot == null) {
			synchronized (MeshRootImpl.class) {
				TagFamilyRoot foundTagFamilyRoot = out(HAS_TAGFAMILY_ROOT).has(TagFamilyRootImpl.class).nextOrDefaultExplicit(TagFamilyRootImpl.class,
					null);
				if (foundTagFamilyRoot == null) {
					tagFamilyRoot = getGraph().addFramedVertex(TagFamilyRootImpl.class);
					linkOut(tagFamilyRoot, HAS_TAGFAMILY_ROOT);
					if (log.isDebugEnabled()) {
						log.debug("Created tag family root {" + tagFamilyRoot.getUuid() + "}");
					}
				} else {
					tagFamilyRoot = foundTagFamilyRoot;
				}
			}
		}
		return tagFamilyRoot;
	}

	@Override
	public MicroschemaRoot getMicroschemaContainerRoot() {
		if (microschemaRoot == null) {
			synchronized (MeshRootImpl.class) {
				MicroschemaRoot foundMicroschemaRoot = out(HAS_MICROSCHEMA_ROOT).has(MicroschemaContainerRootImpl.class)
					.nextOrDefaultExplicit(MicroschemaContainerRootImpl.class, null);
				if (foundMicroschemaRoot == null) {
					microschemaRoot = getGraph().addFramedVertex(MicroschemaContainerRootImpl.class);
					linkOut(microschemaRoot, HAS_MICROSCHEMA_ROOT);
					if (log.isDebugEnabled()) {
						log.debug("Created microschema root {" + microschemaRoot.getUuid() + "}");
					}
				} else {
					microschemaRoot = foundMicroschemaRoot;
				}
			}
		}
		return microschemaRoot;
	}

	/**
	 * Clear static references to the aggregation vertices.
	 */
	public void clearReferences() {
		projectRoot = null;
		tagRoot = null;

		userRoot = null;
		groupRoot = null;
		roleRoot = null;

		schemaRoot = null;
		tagFamilyRoot = null;
		microschemaRoot = null;
		languageRoot = null;
		jobRoot = null;
		changelogRoot = null;
		
	}

	@Override
	public HibBaseElement resolvePathToElement(String pathToElement) {
		MeshRoot root = mesh().boot().meshRoot();
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
		Stack<String> stack = new Stack<String>();
		stack.addAll(list);

		if (log.isDebugEnabled()) {
			log.debug("Found " + stack.size() + " elements");
			for (String segment : list) {
				log.debug("Segment: " + segment);
			}
		}
		String rootNodeSegment = stack.pop();

		// Check whether the root segment is a project name
		Project project = root.getProjectRoot().findByName(rootNodeSegment);
		if (project != null) {
			return project;
		} else {
			switch (rootNodeSegment) {
			case PermissionRoots.PROJECTS:
				return root.getProjectRoot().resolveToElement(stack);
			case PermissionRoots.USERS:
				return root.getUserRoot().resolveToElement(stack);
			case PermissionRoots.GROUPS:
				return root.getGroupRoot().resolveToElement(stack);
			case PermissionRoots.ROLES:
				return root.getRoleRoot().resolveToElement(stack);
			case PermissionRoots.MICROSCHEMAS:
				return root.getMicroschemaContainerRoot().resolveToElement(stack);
			case PermissionRoots.SCHEMAS:
				return root.getSchemaContainerRoot().resolveToElement(stack);
			default:
				// TOOO i18n
				throw error(NOT_FOUND, "Could not resolve given path. Unknown element {" + rootNodeSegment + "}");
			}
		}
	}

	@Override
	public Node findNodeByUuid(String uuid) {
		return db().getVerticesTraversal(NodeImpl.class, "uuid", uuid).nextOrNull();
	}

	@Override
	public long nodeCount() {
		return db().count(NodeImpl.class);
	}

	@Override
	public Result<? extends Node> findAllNodes() {
		return db().getVerticesTraversal(NodeImpl.class, new String[0], new String[0]);
	}

}
