package com.gentics.mesh.cli;

import static com.gentics.mesh.util.DeploymentUtil.deployAndWait;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.verticle.admin.AdminVerticle;
import com.gentics.mesh.core.verticle.admin.RestInfoVerticle;
import com.gentics.mesh.core.verticle.auth.AuthenticationVerticle;
import com.gentics.mesh.core.verticle.eventbus.EventbusVerticle;
import com.gentics.mesh.core.verticle.group.GroupVerticle;
import com.gentics.mesh.core.verticle.microschema.MicroschemaVerticle;
import com.gentics.mesh.core.verticle.navroot.NavRootVerticle;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.core.verticle.project.ProjectVerticle;
import com.gentics.mesh.core.verticle.release.ReleaseVerticle;
import com.gentics.mesh.core.verticle.role.RoleVerticle;
import com.gentics.mesh.core.verticle.schema.ProjectSchemaVerticle;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyVerticle;
import com.gentics.mesh.core.verticle.user.UserVerticle;
import com.gentics.mesh.core.verticle.utility.UtilityVerticle;
import com.gentics.mesh.core.verticle.webroot.WebRootVerticle;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.GraphQLVerticle;
import com.gentics.mesh.search.ProjectSearchVerticle;
import com.gentics.mesh.search.SearchVerticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class CoreVerticleLoader {

	private static Logger log = LoggerFactory.getLogger(CoreVerticleLoader.class);

	@Inject
	public RestInfoVerticle restInfoVerticle;

	@Inject
	public UserVerticle userVerticle;

	@Inject
	public GroupVerticle groupVerticle;

	@Inject
	public RoleVerticle roleVerticle;

	@Inject
	public SchemaVerticle schemaVerticle;

	@Inject
	public ProjectVerticle projectVerticle;

	@Inject
	public UtilityVerticle utilityVerticle;

	@Inject
	public EventbusVerticle eventbusVerticle;

	@Inject
	public NodeVerticle nodeVerticle;

	@Inject
	public TagFamilyVerticle tagFamilyVerticle;

	@Inject
	public WebRootVerticle webrootVerticle;

	@Inject
	public NavRootVerticle navrootVerticle;

	@Inject
	public MicroschemaVerticle microschemaVerticle;

	@Inject
	public NodeMigrationVerticle nodeMigrationVerticle;

	@Inject
	public ProjectSchemaVerticle projectSchemaVerticle;

	@Inject
	public ReleaseVerticle releaseVerticle;

	@Inject
	public SearchVerticle searchVerticle;

	@Inject
	public ProjectSearchVerticle projectSearchVerticle;

	@Inject
	public AuthenticationVerticle authenticationVerticle;

	@Inject
	public AdminVerticle adminVerticle;

	@Inject
	public GraphQLVerticle graphQLVerticle;

	@Inject
	public CoreVerticleLoader() {

	}

	/**
	 * Load verticles that are configured within the mesh configuration.
	 * 
	 * @param configuration
	 * @throws InterruptedException
	 */
	public void loadVerticles(MeshOptions configuration) throws InterruptedException {
		JsonObject defaultConfig = new JsonObject();
		defaultConfig.put("port", configuration.getHttpServerOptions().getPort());

		for (AbstractVerticle verticle : getMandatoryVerticleClasses()) {
			try {
				if (log.isInfoEnabled()) {
					log.info("Loading mandatory verticle {" + verticle.getClass().getName() + "}.");
				}
				// TODO handle custom config? i assume we will not allow this
				deployAndWait(Mesh.vertx(), defaultConfig, verticle, false);
			} catch (InterruptedException e) {
				log.error("Could not load mandatory verticle {" + verticle.getClass().getSimpleName() + "}.", e);
			}
		}

		for (AbstractVerticle verticle : getMandatoryWorkerVerticleClasses()) {
			try {
				if (log.isInfoEnabled()) {
					log.info("Loading mandatory verticle {" + verticle.getClass().getName() + "}.");
				}
				// TODO handle custom config? i assume we will not allow this
				deployAndWait(Mesh.vertx(), defaultConfig, verticle, true);
			} catch (InterruptedException e) {
				log.error("Could not load mandatory verticle {" + verticle.getClass().getSimpleName() + "}.", e);
			}
		}

		//		for (String verticleName : configuration.getVerticles().keySet()) {
		//			if (getMandatoryVerticleClasses().containsKey(verticleName)) {
		//				log.error("Can't configure mandatory verticles. Skipping configured verticle {" + verticleName + "}");
		//				continue;
		//			}
		//			MeshVerticleConfiguration verticleConf = configuration.getVerticles().get(verticleName);
		//			JsonObject mergedVerticleConfig = new JsonObject();
		//			if (verticleConf.getVerticleConfig() != null) {
		//				mergedVerticleConfig = verticleConf.getVerticleConfig().copy();
		//			}
		//			mergedVerticleConfig.put("port", configuration.getHttpServerOptions().getPort());
		//			try {
		//				if (log.isInfoEnabled()) {
		//					log.info("Loading configured verticle {" + verticleName + "}.");
		//				}
		//				deployAndWait(Mesh.vertx(), mergedVerticleConfig, verticleName, false);
		//			} catch (InterruptedException e) {
		//				log.error("Could not load verticle {" + verticleName + "}.", e);
		//			}
		//		}

	}

	/**
	 * Return a Map of mandatory verticles.
	 * 
	 * @return
	 */
	private List<AbstractVerticle> getMandatoryVerticleClasses() {
		List<AbstractVerticle> verticles = new ArrayList<>();
		verticles.add(restInfoVerticle);
		//		verticles.add(projectInfoVerticle());

		// User Group Role verticles
		verticles.add(userVerticle);
		verticles.add(groupVerticle);
		verticles.add(roleVerticle);

		// Project specific verticles
		verticles.add(nodeVerticle);
		verticles.add(tagFamilyVerticle);
		verticles.add(projectSchemaVerticle);
		verticles.add(releaseVerticle);

		// Global verticles
		verticles.add(webrootVerticle);
		verticles.add(navrootVerticle);
		verticles.add(projectVerticle);
		verticles.add(schemaVerticle);
		verticles.add(microschemaVerticle);
		verticles.add(searchVerticle);
		verticles.add(projectSearchVerticle);
		verticles.add(authenticationVerticle);
		verticles.add(adminVerticle);
		verticles.add(eventbusVerticle);
		verticles.add(utilityVerticle);
		verticles.add(graphQLVerticle);
		return verticles;
	}

	/**
	 * Get the map of mandatory worker verticle classes
	 * 
	 * @return
	 */
	private List<AbstractVerticle> getMandatoryWorkerVerticleClasses() {
		List<AbstractVerticle> verticles = new ArrayList<>();
		verticles.add(nodeMigrationVerticle);
		return verticles;
	}

}
