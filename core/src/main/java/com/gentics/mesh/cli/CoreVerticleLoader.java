package com.gentics.mesh.cli;

import static com.gentics.mesh.util.DeploymentUtil.deployAndWait;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.verticle.admin.AdminEndpoint;
import com.gentics.mesh.core.verticle.admin.RestInfoEndpoint;
import com.gentics.mesh.core.verticle.auth.AuthenticationEndpoint;
import com.gentics.mesh.core.verticle.eventbus.EventbusEndpoint;
import com.gentics.mesh.core.verticle.group.GroupEndpoint;
import com.gentics.mesh.core.verticle.microschema.MicroschemaEndpoint;
import com.gentics.mesh.core.verticle.navroot.NavRootEndpoint;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.core.verticle.node.NodeEndpoint;
import com.gentics.mesh.core.verticle.project.ProjectEndpoint;
import com.gentics.mesh.core.verticle.release.ReleaseEndpoint;
import com.gentics.mesh.core.verticle.role.RoleEndpoint;
import com.gentics.mesh.core.verticle.schema.ProjectSchemaEndpoint;
import com.gentics.mesh.core.verticle.schema.SchemaEndpoint;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyEndpoint;
import com.gentics.mesh.core.verticle.user.UserEndpoint;
import com.gentics.mesh.core.verticle.utility.UtilityEndpoint;
import com.gentics.mesh.core.verticle.webroot.WebRootEndpoint;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.ProjectSearchEndpoint;
import com.gentics.mesh.search.SearchEndpoint;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class CoreVerticleLoader {

	private static Logger log = LoggerFactory.getLogger(CoreVerticleLoader.class);

	@Inject
	public RestInfoEndpoint restInfoEndpoint;

	@Inject
	public UserEndpoint userEndpoint;

	@Inject
	public GroupEndpoint groupVerticle;

	@Inject
	public RoleEndpoint roleVerticle;

	@Inject
	public SchemaEndpoint schemaVerticle;

	@Inject
	public ProjectEndpoint projectVerticle;

	@Inject
	public UtilityEndpoint utilityVerticle;

	@Inject
	public EventbusEndpoint eventbusVerticle;

	@Inject
	public NodeEndpoint nodeVerticle;

	@Inject
	public TagFamilyEndpoint tagFamilyVerticle;

	@Inject
	public WebRootEndpoint webrootVerticle;

	@Inject
	public NavRootEndpoint navrootVerticle;

	@Inject
	public MicroschemaEndpoint microschemaVerticle;

	@Inject
	public NodeMigrationVerticle nodeMigrationVerticle;

	@Inject
	public ProjectSchemaEndpoint projectSchemaVerticle;

	@Inject
	public ReleaseEndpoint releaseVerticle;

	@Inject
	public SearchEndpoint searchVerticle;

	@Inject
	public ProjectSearchEndpoint projectSearchVerticle;

	@Inject
	public AuthenticationEndpoint authenticationVerticle;

	@Inject
	public AdminEndpoint adminVerticle;

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
