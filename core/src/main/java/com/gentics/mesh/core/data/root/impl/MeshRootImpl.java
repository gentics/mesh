package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ASSET_BINARY_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_GROUP_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_JOB_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LANGUAGE_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE_ROOT;
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

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.asset.AssetBinaryRoot;
import com.gentics.mesh.core.data.asset.impl.AssetBinaryRootImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.job.JobRoot;
import com.gentics.mesh.core.data.job.impl.JobRootImpl;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see MeshRoot
 */
public class MeshRootImpl extends MeshVertexImpl implements MeshRoot {

	private static Logger log = LoggerFactory.getLogger(MeshRootImpl.class);

	private static UserRoot userRoot;
	private static GroupRoot groupRoot;
	private static RoleRoot roleRoot;

	private static NodeRoot nodeRoot;
	private static TagRoot tagRoot;
	private static TagFamilyRoot tagFamilyRoot;

	private static LanguageRoot languageRoot;
	private static ProjectRoot projectRoot;

	private static SchemaContainerRoot schemaContainerRoot;
	private static MicroschemaContainerRoot microschemaContainerRoot;
	private static JobRoot jobRoot;
	private static AssetBinaryRoot assetBinaryRoot;

	public static void init(Database database) {
		database.addVertexType(MeshRootImpl.class, MeshVertexImpl.class);
	}

	@Override
	public String getMeshVersion() {
		return getProperty(MESH_VERSION);
	}

	@Override
	public void setMeshVersion(String version) {
		setProperty(MESH_VERSION, version);
	}

	@Override
	public AssetBinaryRoot getAssetBinaryRoot() {
		if (assetBinaryRoot == null) {
			synchronized (MeshRootImpl.class) {
				AssetBinaryRoot foundAssetBinaryRoot = out(HAS_ASSET_BINARY_ROOT).nextOrDefaultExplicit(AssetBinaryRootImpl.class, null);
				if (foundAssetBinaryRoot == null) {
					assetBinaryRoot = getGraph().addFramedVertex(AssetBinaryRootImpl.class);
					linkOut(assetBinaryRoot, HAS_ASSET_BINARY_ROOT);
					if (log.isInfoEnabled()) {
						log.info("Created asset binary root {" + assetBinaryRoot.getUuid() + "}");
					}
				} else {
					assetBinaryRoot = foundAssetBinaryRoot;
				}
			}
		}
		return assetBinaryRoot;
	}

	@Override
	public JobRoot getJobRoot() {
		if (jobRoot == null) {
			synchronized (MeshRootImpl.class) {
				JobRoot foundJobRoot = out(HAS_JOB_ROOT).nextOrDefaultExplicit(JobRootImpl.class, null);
				if (foundJobRoot == null) {
					jobRoot = getGraph().addFramedVertex(JobRootImpl.class);
					linkOut(jobRoot, HAS_JOB_ROOT);
					if (log.isInfoEnabled()) {
						log.info("Created job root {" + jobRoot.getUuid() + "}");
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
					if (log.isInfoEnabled()) {
						log.info("Created tag root {" + userRoot.getUuid() + "}");
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
					if (log.isInfoEnabled()) {
						log.info("Created role root {" + roleRoot.getUuid() + "}");
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
					if (log.isInfoEnabled()) {
						log.info("Created group root {" + groupRoot.getUuid() + "}");
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
					if (log.isInfoEnabled()) {
						log.info("Created tag root {" + tagRoot.getUuid() + "}");
					}
				} else {
					tagRoot = foundTagRoot;
				}
			}
		}
		return tagRoot;
	}

	@Override
	public SchemaContainerRoot getSchemaContainerRoot() {
		if (schemaContainerRoot == null) {
			synchronized (MeshRootImpl.class) {
				SchemaContainerRoot foundSchemaContainerRoot = out(HAS_SCHEMA_ROOT).has(SchemaContainerRootImpl.class)
						.nextOrDefaultExplicit(SchemaContainerRootImpl.class, null);
				if (foundSchemaContainerRoot == null) {
					schemaContainerRoot = getGraph().addFramedVertex(SchemaContainerRootImpl.class);
					linkOut(schemaContainerRoot, HAS_SCHEMA_ROOT);
					if (log.isInfoEnabled()) {
						log.info("Created schema container root {" + schemaContainerRoot.getUuid() + "}");
					}
				} else {
					schemaContainerRoot = foundSchemaContainerRoot;
				}
			}
		}
		return schemaContainerRoot;
	}

	@Override
	public LanguageRoot getLanguageRoot() {
		if (languageRoot == null) {
			synchronized (MeshRootImpl.class) {
				LanguageRoot foundLanguageRoot = out(HAS_LANGUAGE_ROOT).nextOrDefaultExplicit(LanguageRootImpl.class, null);
				if (foundLanguageRoot == null) {
					languageRoot = getGraph().addFramedVertex(LanguageRootImpl.class);
					linkOut(languageRoot, HAS_LANGUAGE_ROOT);
					if (log.isInfoEnabled()) {
						log.info("Created language root {" + languageRoot.getUuid() + "}");
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
					if (log.isInfoEnabled()) {
						log.info("Created project root {" + projectRoot.getUuid() + "}");
					}
				} else {
					projectRoot = foundProjectRoot;
				}
			}
		}
		return projectRoot;
	}

	@Override
	public NodeRoot getNodeRoot() {
		if (nodeRoot == null) {
			synchronized (MeshRootImpl.class) {
				NodeRoot foundNodeRoot = out(HAS_NODE_ROOT).nextOrDefaultExplicit(NodeRootImpl.class, null);
				if (foundNodeRoot == null) {
					nodeRoot = getGraph().addFramedVertex(NodeRootImpl.class);
					linkOut(nodeRoot, HAS_NODE_ROOT);
					if (log.isInfoEnabled()) {
						log.info("Created node root {" + nodeRoot.getUuid() + "}");
					}
				} else {
					nodeRoot = foundNodeRoot;
				}
			}
		}
		return nodeRoot;
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
					if (log.isInfoEnabled()) {
						log.info("Created tag family root {" + tagFamilyRoot.getUuid() + "}");
					}
				} else {
					tagFamilyRoot = foundTagFamilyRoot;
				}
			}
		}
		return tagFamilyRoot;
	}

	@Override
	public MicroschemaContainerRoot getMicroschemaContainerRoot() {
		if (microschemaContainerRoot == null) {
			synchronized (MeshRootImpl.class) {
				MicroschemaContainerRoot foundMicroschemaContainerRoot = out(HAS_MICROSCHEMA_ROOT).has(MicroschemaContainerRootImpl.class)
						.nextOrDefaultExplicit(MicroschemaContainerRootImpl.class, null);
				if (foundMicroschemaContainerRoot == null) {
					microschemaContainerRoot = getGraph().addFramedVertex(MicroschemaContainerRootImpl.class);
					linkOut(microschemaContainerRoot, HAS_MICROSCHEMA_ROOT);
					if (log.isInfoEnabled()) {
						log.info("Created microschema root {" + microschemaContainerRoot.getUuid() + "}");
					}
				} else {
					microschemaContainerRoot = foundMicroschemaContainerRoot;
				}
			}
		}
		return microschemaContainerRoot;
	}

	/**
	 * Clear static references to the aggregation vertices.
	 */
	public static void clearReferences() {
		MeshRootImpl.projectRoot = null;
		MeshRootImpl.nodeRoot = null;
		MeshRootImpl.tagRoot = null;

		MeshRootImpl.userRoot = null;
		MeshRootImpl.groupRoot = null;
		MeshRootImpl.roleRoot = null;

		MeshRootImpl.schemaContainerRoot = null;
		MeshRootImpl.tagFamilyRoot = null;
		MeshRootImpl.microschemaContainerRoot = null;
		MeshRootImpl.languageRoot = null;
		MeshRootImpl.jobRoot = null;
	}

	@Override
	public MeshVertex resolvePathToElement(String pathToElement) {
		MeshRoot root = MeshInternal.get().boot().meshRoot();
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
			case ProjectRoot.TYPE:
				return root.getProjectRoot().resolveToElement(stack);
			case UserRoot.TYPE:
				return root.getUserRoot().resolveToElement(stack);
			case GroupRoot.TYPE:
				return root.getGroupRoot().resolveToElement(stack);
			case RoleRoot.TYPE:
				return root.getRoleRoot().resolveToElement(stack);
			case MicroschemaContainerRoot.TYPE:
				return root.getMicroschemaContainerRoot().resolveToElement(stack);
			case SchemaContainerRoot.TYPE:
				return root.getSchemaContainerRoot().resolveToElement(stack);
			default:
				// TOOO i18n
				throw error(NOT_FOUND, "Could not resolve given path. Unknown element {" + rootNodeSegment + "}");
			}
		}
	}

}
