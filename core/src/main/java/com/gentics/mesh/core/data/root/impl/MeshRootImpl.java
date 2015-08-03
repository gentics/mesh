package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_GROUP_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_LANGUAGE_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_MICROSCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_NODE_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_PROJECT_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_ROLE_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_SEARCH_QUEUE_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAG_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_USER_ROOT;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
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
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.impl.SearchQueueImpl;

public class MeshRootImpl extends MeshVertexImpl implements MeshRoot {

	private static Logger log = LoggerFactory.getLogger(MeshRootImpl.class);

	private static MeshRoot instance;

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
	private static SearchQueue searchQueueRoot;

	public static MeshRoot getInstance() {
		return instance;
	}

	public static void setInstance(MeshRoot meshRoot) {
		instance = meshRoot;
	}

	@Override
	public UserRoot getUserRoot() {
		if (userRoot != null) {
			return userRoot;
		} else {
			userRoot = out(HAS_USER_ROOT).has(UserRootImpl.class).nextOrDefault(UserRootImpl.class, null);
			if (userRoot == null) {
				userRoot = getGraph().addFramedVertex(UserRootImpl.class);
				linkOut(userRoot.getImpl(), HAS_USER_ROOT);
				log.info("Stored tag root {" + userRoot.getUuid() + "}");
			}
			return userRoot;
		}
	}

	@Override
	public RoleRoot getRoleRoot() {
		if (roleRoot != null) {
			return roleRoot;
		} else {
			roleRoot = out(HAS_ROLE_ROOT).has(RoleRootImpl.class).nextOrDefault(RoleRootImpl.class, null);
			if (roleRoot == null) {
				roleRoot = getGraph().addFramedVertex(RoleRootImpl.class);
				linkOut(roleRoot.getImpl(), HAS_ROLE_ROOT);
				log.info("Stored role root {" + roleRoot.getUuid() + "}");
			}
			return roleRoot;
		}
	}

	@Override
	public SearchQueue getSearchQueue() {
		if (searchQueueRoot != null) {
			return searchQueueRoot;
		} else {
			searchQueueRoot = out(HAS_SEARCH_QUEUE_ROOT).has(SearchQueueImpl.class).nextOrDefault(SearchQueueImpl.class, null);
			if (searchQueueRoot == null) {
				searchQueueRoot = getGraph().addFramedVertex(SearchQueueImpl.class);
				linkOut(searchQueueRoot.getImpl(), HAS_SEARCH_QUEUE_ROOT);
				log.info("Stored search queue root {" + searchQueueRoot.getUuid() + "}");
			}
		}
		return searchQueueRoot;

	}

	@Override
	public GroupRoot getGroupRoot() {
		if (groupRoot != null) {
			return groupRoot;
		} else {
			groupRoot = out(HAS_GROUP_ROOT).has(GroupRootImpl.class).nextOrDefault(GroupRootImpl.class, null);
			if (groupRoot == null) {
				groupRoot = getGraph().addFramedVertex(GroupRootImpl.class);
				linkOut(groupRoot.getImpl(), HAS_GROUP_ROOT);
				log.info("Stored group root {" + groupRoot.getUuid() + "}");
			}
			return groupRoot;
		}
	}

	@Override
	public TagRoot getTagRoot() {
		if (tagRoot != null) {
			return tagRoot;
		} else {
			tagRoot = out(HAS_TAG_ROOT).has(TagRootImpl.class).nextOrDefault(TagRootImpl.class, null);
			if (tagRoot == null) {
				tagRoot = getGraph().addFramedVertex(TagRootImpl.class);
				linkOut(tagRoot.getImpl(), HAS_TAG_ROOT);
				log.info("Stored tag root {" + tagRoot.getUuid() + "}");
			}
			return tagRoot;
		}
	}

	@Override
	public SchemaContainerRoot getSchemaContainerRoot() {
		if (schemaContainerRoot != null) {
			return schemaContainerRoot;
		} else {
			schemaContainerRoot = out(HAS_SCHEMA_ROOT).has(SchemaContainerRootImpl.class).nextOrDefault(SchemaContainerRootImpl.class, null);
			if (schemaContainerRoot == null) {
				schemaContainerRoot = getGraph().addFramedVertex(SchemaContainerRootImpl.class);
				linkOut(schemaContainerRoot.getImpl(), HAS_SCHEMA_ROOT);
				log.info("Stored schema container root {" + schemaContainerRoot.getUuid() + "}");
			}
			return schemaContainerRoot;
		}
	}

	@Override
	public LanguageRoot getLanguageRoot() {
		if (languageRoot != null) {
			return languageRoot;
		} else {
			languageRoot = out(HAS_LANGUAGE_ROOT).has(LanguageRootImpl.class).nextOrDefault(LanguageRootImpl.class, null);
			if (languageRoot == null) {
				languageRoot = getGraph().addFramedVertex(LanguageRootImpl.class);
				linkOut(languageRoot.getImpl(), HAS_LANGUAGE_ROOT);
				log.info("Stored language root {" + languageRoot.getUuid() + "}");
			}
			return languageRoot;
		}
	}

	@Override
	public ProjectRoot getProjectRoot() {
		if (projectRoot != null) {
			return projectRoot;
		} else {
			projectRoot = out(HAS_PROJECT_ROOT).has(ProjectRootImpl.class).nextOrDefault(ProjectRootImpl.class, null);
			if (projectRoot == null) {
				projectRoot = getGraph().addFramedVertex(ProjectRootImpl.class);
				linkOut(projectRoot.getImpl(), HAS_PROJECT_ROOT);
				log.info("Stored project root {" + projectRoot.getUuid() + "}");
			}
			return projectRoot;
		}
	}

	@Override
	public NodeRoot getNodeRoot() {
		if (nodeRoot != null) {
			return nodeRoot;
		} else {
			nodeRoot = out(HAS_NODE_ROOT).has(NodeRootImpl.class).nextOrDefault(NodeRootImpl.class, null);
			if (nodeRoot == null) {
				nodeRoot = getGraph().addFramedVertex(NodeRootImpl.class);
				linkOut(nodeRoot.getImpl(), HAS_NODE_ROOT);
				log.info("Stored node root {" + nodeRoot.getUuid() + "}");
			}
			return nodeRoot;
		}
	}

	@Override
	public TagFamilyRoot getTagFamilyRoot() {
		if (tagFamilyRoot != null) {
			return tagFamilyRoot;
		} else {
			tagFamilyRoot = out(HAS_TAGFAMILY_ROOT).has(TagFamilyRootImpl.class).nextOrDefaultExplicit(TagFamilyRootImpl.class, null);
			if (tagFamilyRoot == null) {
				tagFamilyRoot = getGraph().addFramedVertex(TagFamilyRootImpl.class);
				linkOut(tagFamilyRoot.getImpl(), HAS_TAGFAMILY_ROOT);
				log.info("Stored tag family root {" + tagFamilyRoot.getUuid() + "}");
			}
			return tagFamilyRoot;
		}
	}

	@Override
	public MicroschemaContainerRoot getMicroschemaContainerRoot() {
		if (microschemaContainerRoot != null) {
			return microschemaContainerRoot;
		} else {
			microschemaContainerRoot = out(HAS_MICROSCHEMA_ROOT).has(MicroschemaContainerRootImpl.class).nextOrDefaultExplicit(
					MicroschemaContainerRootImpl.class, null);
			if (microschemaContainerRoot == null) {
				microschemaContainerRoot = getGraph().addFramedVertex(MicroschemaContainerRootImpl.class);
				linkOut(microschemaContainerRoot.getImpl(), HAS_MICROSCHEMA_ROOT);
				log.info("Stored microschema root {" + microschemaContainerRoot.getUuid() + "}");
			}
			return microschemaContainerRoot;
		}
	}

	@Override
	public void clearReferences() {
		projectRoot = null;
		tagRoot = null;
		roleRoot = null;
		groupRoot = null;
		userRoot = null;
		nodeRoot = null;
		schemaContainerRoot = null;
		languageRoot = null;
	}

	@Override
	public MeshRootImpl getImpl() {
		return this;
	}

}
