package com.gentics.mesh.cli;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Predicate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.core.data.changelog.HighLevelChange;
import com.gentics.mesh.core.data.dao.*;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.RootResolver;
import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.Vertx;

/**
 * The bootstrap initializer takes care of creating all mandatory dao elements for mesh. This includes the creation of ProjectDao, NodeDao,
 * GroupDao, UserDao and various element such as the Admin User, Admin Group, Admin Role.
 */
public interface BootstrapInitializer {

	@Getter
	ProjectDao projectDao();

	@Getter
	BranchDao branchDao();

	@Getter
	LanguageDao languageDao();

	@Getter
	GroupDao groupDao();

	@Getter
	UserDao userDao();

	@Getter
	JobDao jobDao();

	@Getter
	TagFamilyDao tagFamilyDao();

	@Getter
	TagDao tagDao();

	@Getter
	RoleDao roleDao();

	@Getter
	MicroschemaDao microschemaDao();

	@Getter
	SchemaDao schemaDao();

	@Getter
	NodeDao nodeDao();

	@Getter
	ContentDao contentDao();

	@Getter
	BinaryDao binaryDao();

	@Getter
	S3BinaryDao s3binaryDao();

	@Getter
	RootResolver rootResolver();

	/**
	 * Return the anonymous role (if-present).
	 * 
	 * @return
	 */
	HibRole anonymousRole();

	/**
	 * Initialise the search index mappings.
	 */
	void createSearchIndicesAndMappings();

	/***
	 * Marking all changes as applied since this is an initial mesh setup
	 */
	void markChangelogApplied();

	/**
	 * Setup data, that is required for Mesh to start, e.g. mandatory DB structure.
	 * 
	 * @param config
	 * @throws Exception
	 */
	void initMandatoryData(MeshOptions config) throws Exception;

	/**
	 * Setup data, that is required for Mesh to operate on a basic level, e.g. admin user, group.
	 *
	 * @param meshOptions
	 */
	void initBasicData(MeshOptions meshOptions);

	/**
	 * Clear all caches.
	 */
	void globalCacheClear();

	/**
	 * Setup the optional data. Optional data will only be setup during the first setup. Mesh will not try to recreate those elements on each setup. The
	 * {@link #initMandatoryData(MeshOptions)} method on the other hand will setup elements which must exist and thus will enforce creation of those elements.
	 * 
	 * @param isEmptyInstallation
	 */
	void initOptionalData(boolean isEmptyInstallation);

	/**
	 * Initialise mesh using the given configuration.
	 * 
	 * This method will startup mesh and take care of tasks which need to be executed before the REST endpoints can be accessed.
	 * 
	 * The following steps / checks will be performed:
	 * <ul>
	 * <li>Join the mesh cluster
	 * <li>Invoke the changelog check &amp; execution
	 * <li>Initialize the graph database and update vertex types and indices
	 * <li>Create initial mandatory structure data
	 * <li>Initialize search indices
	 * <li>Load verticles and setup routes / endpoints
	 * </ul>
	 * 
	 * @param mesh
	 * @param hasOldLock
	 * @param configuration
	 * @param verticleLoader
	 * @throws Exception
	 */
	void init(Mesh mesh, boolean hasOldLock, MeshOptions configuration, MeshCustomLoader<Vertx> verticleLoader) throws Exception;

	/**
	 * Init the database layout.
	 */
	void initDatabaseTypes();

	/**
	 * Initialize the languages by loading the JSON file and creating the language elements.
	 *
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	void initLanguages() throws JsonParseException, JsonMappingException, IOException;

	/**
	 * Initialize optional languages, if an additional language file was configured
	 * 
	 * @param configuration
	 *            configuration
	 */
	void initOptionalLanguages(MeshOptions configuration);

	/**
	 * Grant CRUD to all objects to the Admin Role.
	 */
	void initPermissions();

	/**
	 * Invoke the changelog system to execute database changes.
	 * 
	 * @param flags
	 *            Flags which will be used to control the post process actions
	 */
	void invokeChangelog(PostProcessFlags flags);

	/**
	 * Invoke the changelog system in a cluster. This will only apply changelog entries, which are allowed to be executed in a cluster.
	 *
	 * If any other changelog entries are found, which are not allowed to be executed in a cluster, this method will throw a RuntimeException
	 * @param flags Flags which will be used to control the post process actions
	 * @param configuration mesh options
	 */
	void invokeChangelogInCluster(PostProcessFlags flags, MeshOptions configuration);

	/**
	 * Return the list of all language tags.
	 * 
	 * @return
	 */
	Collection<? extends String> getAllLanguageTags();

	/**
	 * Compare the current version of the mesh graph with the version which is currently being executed.
	 */
	void handleMeshVersion();

	/**
	 * Check whether there are any vertices in the graph.
	 * 
	 * @return
	 */
	boolean isEmptyInstallation();

	/**
	 * Clear all indices and reindex all elements. This is a blocking action and could potentially take a lot of time.
	 */
	void syncIndex();

	/**
	 * Register the eventbus event handlers.
	 */
	void registerEventHandlers();

	/**
	 * Return the Vert.x instance.
	 * 
	 * @return
	 */
	Vertx vertx();

	/**
	 * Return the Mesh API
	 * 
	 * @return
	 */
	Mesh mesh();

	/**
	 * Flag that indicates whether this is the initial setup run for the graph.
	 * 
	 * @return
	 */
	boolean isInitialSetup();

	/**
	 * Clear stored references to graph elements.
	 */
	void clearReferences();

	/**
	 * Check whether the Vert.x instance is ready.
	 * 
	 * @return
	 */
	boolean isVertxReady();

	/**
	 * Check whether the execution of the changelog is required.
	 * 
	 * @param filter optional filter for high level changes to check (may be null to check all high level changes)
	 * @return
	 */
	boolean requiresChangelog(Predicate<? super HighLevelChange> filter);
}
