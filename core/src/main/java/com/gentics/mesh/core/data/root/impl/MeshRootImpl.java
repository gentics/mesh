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

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
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
import com.gentics.mesh.core.data.root.RootVertex;
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
		if (userRoot == null) {
			synchronized (MeshRootImpl.class) {
				UserRoot foundUserRoot = out(HAS_USER_ROOT).has(UserRootImpl.class).nextOrDefault(UserRootImpl.class, null);
				if (foundUserRoot == null) {
					userRoot = getGraph().addFramedVertex(UserRootImpl.class);
					linkOut(userRoot.getImpl(), HAS_USER_ROOT);
					if (log.isInfoEnabled()) {
						log.info("Stored tag root {" + userRoot.getUuid() + "}");
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
				RoleRoot foundRoleRoot = out(HAS_ROLE_ROOT).has(RoleRootImpl.class).nextOrDefault(RoleRootImpl.class, null);
				if (foundRoleRoot == null) {
					roleRoot = getGraph().addFramedVertex(RoleRootImpl.class);
					linkOut(roleRoot.getImpl(), HAS_ROLE_ROOT);
					if (log.isInfoEnabled()) {
						log.info("Stored role root {" + roleRoot.getUuid() + "}");
					}
				} else {
					roleRoot = foundRoleRoot;
				}
			}
		}
		return roleRoot;
	}

	@Override
	public SearchQueue getSearchQueue() {
		if (searchQueueRoot == null) {
			synchronized (MeshRootImpl.class) {
				SearchQueue foundSearchQueueRoot = out(HAS_SEARCH_QUEUE_ROOT).has(SearchQueueImpl.class).nextOrDefault(SearchQueueImpl.class, null);
				if (foundSearchQueueRoot == null) {
					searchQueueRoot = getGraph().addFramedVertex(SearchQueueImpl.class);
					linkOut(searchQueueRoot.getImpl(), HAS_SEARCH_QUEUE_ROOT);
					if (log.isInfoEnabled()) {
						log.info("Stored search queue root {" + searchQueueRoot.getUuid() + "}");
					}
				} else {
					searchQueueRoot = foundSearchQueueRoot;
				}
			}
		}
		return searchQueueRoot;

	}

	@Override
	public GroupRoot getGroupRoot() {
		if (groupRoot == null) {
			synchronized (MeshRootImpl.class) {
				GroupRoot foundGroupRoot = out(HAS_GROUP_ROOT).has(GroupRootImpl.class).nextOrDefault(GroupRootImpl.class, null);
				if (foundGroupRoot == null) {
					groupRoot = getGraph().addFramedVertex(GroupRootImpl.class);
					linkOut(groupRoot.getImpl(), HAS_GROUP_ROOT);
					if (log.isInfoEnabled()) {
						log.info("Stored group root {" + groupRoot.getUuid() + "}");
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
				TagRoot foundTagRoot = out(HAS_TAG_ROOT).has(TagRootImpl.class).nextOrDefault(TagRootImpl.class, null);
				if (foundTagRoot == null) {
					tagRoot = getGraph().addFramedVertex(TagRootImpl.class);
					linkOut(tagRoot.getImpl(), HAS_TAG_ROOT);
					if (log.isInfoEnabled()) {
						log.info("Stored tag root {" + tagRoot.getUuid() + "}");
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
						.nextOrDefault(SchemaContainerRootImpl.class, null);
				if (foundSchemaContainerRoot == null) {
					schemaContainerRoot = getGraph().addFramedVertex(SchemaContainerRootImpl.class);
					linkOut(schemaContainerRoot.getImpl(), HAS_SCHEMA_ROOT);
					if (log.isInfoEnabled()) {
						log.info("Stored schema container root {" + schemaContainerRoot.getUuid() + "}");
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
				LanguageRoot foundLanguageRoot = out(HAS_LANGUAGE_ROOT).has(LanguageRootImpl.class).nextOrDefault(LanguageRootImpl.class, null);
				if (foundLanguageRoot == null) {
					languageRoot = getGraph().addFramedVertex(LanguageRootImpl.class);
					linkOut(languageRoot.getImpl(), HAS_LANGUAGE_ROOT);
					if (log.isInfoEnabled()) {
						log.info("Stored language root {" + languageRoot.getUuid() + "}");
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
				ProjectRoot foundProjectRoot = out(HAS_PROJECT_ROOT).has(ProjectRootImpl.class).nextOrDefault(ProjectRootImpl.class, null);
				if (foundProjectRoot == null) {
					projectRoot = getGraph().addFramedVertex(ProjectRootImpl.class);
					linkOut(projectRoot.getImpl(), HAS_PROJECT_ROOT);
					if (log.isInfoEnabled()) {
						log.info("Stored project root {" + projectRoot.getUuid() + "}");
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
				NodeRoot foundNodeRoot = out(HAS_NODE_ROOT).has(NodeRootImpl.class).nextOrDefault(NodeRootImpl.class, null);
				if (foundNodeRoot == null) {
					nodeRoot = getGraph().addFramedVertex(NodeRootImpl.class);
					linkOut(nodeRoot.getImpl(), HAS_NODE_ROOT);
					if (log.isInfoEnabled()) {
						log.info("Stored node root {" + nodeRoot.getUuid() + "}");
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
					linkOut(tagFamilyRoot.getImpl(), HAS_TAGFAMILY_ROOT);
					if (log.isInfoEnabled()) {
						log.info("Stored tag family root {" + tagFamilyRoot.getUuid() + "}");
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
					linkOut(microschemaContainerRoot.getImpl(), HAS_MICROSCHEMA_ROOT);
					if (log.isInfoEnabled()) {
						log.info("Stored microschema root {" + microschemaContainerRoot.getUuid() + "}");
					}
				} else {
					microschemaContainerRoot = foundMicroschemaContainerRoot;
				}
			}
		}
		return microschemaContainerRoot;
	}

	@Override
	public void clearReferences() {
		MeshRootImpl.projectRoot = null;
		MeshRootImpl.tagRoot = null;
		MeshRootImpl.roleRoot = null;
		MeshRootImpl.groupRoot = null;
		MeshRootImpl.userRoot = null;
		MeshRootImpl.nodeRoot = null;
		MeshRootImpl.schemaContainerRoot = null;
		MeshRootImpl.tagFamilyRoot = null;
		MeshRootImpl.languageRoot = null;
	}

	@Override
	public void resolvePathToElement(String pathToElement, Handler<AsyncResult<? extends MeshVertex>> resultHandler) {
		MeshRoot root = BootstrapInitializer.getBoot().meshRoot();
		if (pathToElement.endsWith("/")) {
			resultHandler.handle(Future.failedFuture("Could not resolve path. The path must not end with a slash."));
			return;
		}
		String[] elements = pathToElement.split("\\/");
		if (log.isDebugEnabled()) {
			log.debug("Found " + elements.length + " elements");
		}
		String rootNodeSegment = elements[0];
		RootVertex<? extends GenericVertex<?>> rootVertex = null;
		switch (rootNodeSegment) {
		case "projects":
			ProjectRoot projectRoot = root.getProjectRoot();
			if (elements.length > 4) {
				//TODO maybe this will change in the future. It would be better to check this individually within each segment handler
				resultHandler.handle(Future.failedFuture("Could not resolve path. You can't resolve more then 4 segments."));
			} else if (elements.length == 1) {
				resultHandler.handle(Future.succeededFuture(projectRoot));
			} else if (elements.length >= 2) {
				String uuidSegment = elements[1];
				projectRoot.findByUuid(uuidSegment, rh -> {
					if (rh.succeeded()) {
						Project project = rh.result();

						if (elements.length == 2) {
							resultHandler.handle(Future.succeededFuture(project));
						} else if (elements.length > 2) {

							String nestedRootNode = elements[2];
							switch (nestedRootNode) {
							case "tagFamilies":
								TagFamilyRoot tagFamilyRoot = project.getTagFamilyRoot();
								if (elements.length == 3) {
									resultHandler.handle(Future.succeededFuture(tagFamilyRoot));
								} else {
									tagFamilyRoot.findByUuid(elements[3], sh -> {
										if (sh.succeeded()) {
											resultHandler.handle(Future.succeededFuture(sh.result()));
										} else {
											resultHandler.handle(Future.failedFuture(sh.cause()));
										}
									});
								}
								break;
							case "schemas":
								SchemaContainerRoot schemaRoot = project.getSchemaContainerRoot();
								if (elements.length == 3) {
									resultHandler.handle(Future.succeededFuture(schemaRoot));
								} else {
									schemaRoot.findByUuid(elements[3], sh -> {
										if (sh.succeeded()) {
											resultHandler.handle(Future.succeededFuture(sh.result()));
										} else {
											resultHandler.handle(Future.failedFuture(sh.cause()));
										}
									});
								}
								break;
							case "microschemas":
								//project.getMicroschemaRoot();
								throw new NotImplementedException();
								//break;
							case "nodes":
								NodeRoot nodeRoot = project.getNodeRoot();
								if (elements.length == 3) {
									resultHandler.handle(Future.succeededFuture(nodeRoot));
								} else {
									nodeRoot.findByUuid(elements[3], sh -> {
										if (sh.succeeded()) {
											resultHandler.handle(Future.succeededFuture(sh.result()));
										} else {
											resultHandler.handle(Future.failedFuture(sh.cause()));
										}
									});
								}
								break;
							case "tags":
								TagRoot tagRoot = project.getTagRoot();
								if (elements.length == 3) {
									resultHandler.handle(Future.succeededFuture(tagRoot));
								} else {
									tagRoot.findByUuid(elements[3], sh -> {
										if (sh.succeeded()) {
											resultHandler.handle(Future.succeededFuture(sh.result()));
										} else {
											resultHandler.handle(Future.failedFuture(sh.cause()));
										}
									});
								}
								break;
							default:
								resultHandler.handle(Future.failedFuture("Unknown project element {" + nestedRootNode + "}"));
								return;
							}
						}
					} else {
						resultHandler.handle(Future.failedFuture(rh.cause()));
						return;
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture("Could not resolve given path. You specified more then three segments."));
				return;
			}

			return;
		case "users":
			rootVertex = root.getUserRoot();
			break;
		case "groups":
			rootVertex = root.getGroupRoot();
			break;
		case "roles":
			rootVertex = root.getRoleRoot();
			break;
		case "schemas":
			rootVertex = root.getSchemaContainerRoot();
			break;
		default:
			resultHandler.handle(Future.failedFuture("Could not resolve given path. Unknown element {" + rootNodeSegment + "}"));
			return;
		}

		if (rootVertex != null && elements.length == 1) {
			resultHandler.handle(Future.succeededFuture(rootVertex));
		} else if (rootVertex != null && elements.length == 2) {
			String uuidSegment = elements[1];
			rootVertex.findByUuid(uuidSegment, rh -> {
				if (rh.succeeded()) {
					resultHandler.handle(Future.succeededFuture(rh.result()));
				} else {
					resultHandler.handle(Future.failedFuture(rh.cause()));
				}
			});
		} else {
			resultHandler.handle(Future.failedFuture("path could not be resolved."));
		}
	}

	@Override
	public MeshRootImpl getImpl() {
		return this;
	}

}
