package com.gentics.mesh.cli;

import java.io.IOException;
import java.util.Collection;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.changelog.ChangelogRoot;
import com.gentics.mesh.core.data.job.JobRoot;
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
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.Vertx;

/**
 * The bootstrap initialiser takes care of creating all mandatory graph elements for mesh. This includes the creation of MeshRoot, ProjectRoot, NodeRoot,
 * GroupRoot, UserRoot and various element such as the Admin User, Admin Group, Admin Role.
 */
public interface BootstrapInitializer {

	/**
	 * Return the project root element.
	 * 
	 * @return
	 */
	ProjectRoot projectRoot();

	/**
	 * Return the language root element.
	 * 
	 * @return
	 */
	LanguageRoot languageRoot();

	/**
	 * Return the group root element.
	 * 
	 * @return
	 */
	GroupRoot groupRoot();

	/**
	 * Return the user root element.
	 * 
	 * @return
	 */
	UserRoot userRoot();

	/**
	 * Return the job root element.
	 * 
	 * @return
	 */
	JobRoot jobRoot();

	/**
	 * Return the global node root element. Note that projects have their own node root element.
	 * 
	 * @return
	 */
	NodeRoot nodeRoot();

	/**
	 * Return the changelog root element.
	 * 
	 * @return
	 */
	ChangelogRoot changelogRoot();

	/**
	 * Return the global tagfamily root element. Note that each project has their own tag family root element.
	 * 
	 * @return
	 */
	TagFamilyRoot tagFamilyRoot();

	/**
	 * Return the global tag root element. Note that each project has their own tag root element.
	 * 
	 * @return
	 */
	TagRoot tagRoot();

	/**
	 * Return the role root element.
	 * 
	 * @return
	 */
	RoleRoot roleRoot();

	/**
	 * Return the global microschema root element.
	 * 
	 * @return
	 */
	MicroschemaContainerRoot microschemaContainerRoot();

	/**
	 * Return the global schema container root element.
	 * 
	 * @return
	 */
	SchemaContainerRoot schemaContainerRoot();

	/**
	 * Return the mesh root element. All other mesh graph elements are connected to this element. It represents the main root of the whole mesh graph.
	 * 
	 * @return
	 */
	MeshRoot meshRoot();

	/**
	 * Return the anonymous role (if-present).
	 * 
	 * @return
	 */
	Role anonymousRole();

	/**
	 * Initialise the search index mappings.
	 */
	void createSearchIndicesAndMappings();

	/***
	 * Marking all changes as applied since this is an initial mesh setup
	 */
	void markChangelogApplied();

	/**
	 * Setup various mandatory data. This includes mandatory root nodes and the admin user, group.
	 * 
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 * @throws MeshSchemaException
	 */
	void initMandatoryData() throws JsonParseException, JsonMappingException, IOException, MeshSchemaException;

	/**
	 * Clear all caches.
	 */
	void globalCacheClear();

	/**
	 * Setup the optional data. Optional data will only be setup during the first setup. Mesh will not try to recreate those elements on each setup. The
	 * {@link #initMandatoryData()} method on the other hand will setup elements which must exist and thus will enforce creation of those elements.
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
	 * Initialize the languages by loading the JSON file and creating the language graph elements.
	 * 
	 * @param root
	 *            Aggregation node to which the languages will be assigned
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	void initLanguages(LanguageRoot root) throws JsonParseException, JsonMappingException, IOException;

	/**
	 * Initialize optional languages, if an additional language file was configured
	 * 
	 * @param configuration
	 *            configuration
	 */
	void initOptionalLanguages(MeshOptions configuration);

	/**
	 * Grant CRUD to all objects within the graph to the Admin Role.
	 */
	void initPermissions();

	/**
	 * Invoke the changelog system to execute database changes.
	 */
	void invokeChangelog();

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

}
