package com.gentics.cailun.verticle.admin;

import static com.gentics.cailun.util.DeploymentUtils.deployAndWait;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.PUT;
import static io.vertx.core.http.HttpMethod.POST;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.graph.neo4j.Neo4VertxConfiguration;

import java.io.File;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.backup.OnlineBackup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCaiLunCoreApiVerticle;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.repository.ProjectRepository;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.gentics.cailun.git.GitPullChecker;

/**
 * The admin verticle provides core administration rest endpoints.
 * 
 * @author johannes2
 *
 */
@Component
@Scope("singleton")
@SpringVerticle
public class AdminVerticle extends AbstractCaiLunCoreApiVerticle {

	private static final Logger log = LoggerFactory.getLogger(AdminVerticle.class);
	public static final String GIT_PULL_CHECKER_INTERVAL_KEY = "gitPullCheckerInterval";
	public static final String GIT_PULL_CHECKER_KEY = "gitPullChecker";

	public static final boolean DEFAULT_GIT_CHECKER = false;
	public static final long DEFAULT_GIT_CHECKER_INTERVAL = 60 * 5 * 100; // 5 Min

	@Autowired
	private CaiLunSpringConfiguration caiLunConfig;

	@Autowired
	private ProjectRepository projectRepository;

	GitPullChecker gitChecker;

	public AdminVerticle() {
		super("admin");
	}

	@Override
	public void registerEndPoints() throws Exception {
		if (config().getBoolean(GIT_PULL_CHECKER_KEY, DEFAULT_GIT_CHECKER)) {
			gitChecker = new GitPullChecker(config().getLong(GIT_PULL_CHECKER_INTERVAL_KEY, DEFAULT_GIT_CHECKER_INTERVAL));
		}

		addBackupHandler();
		addNeo4VertxRestartHandler();
		addProjectHandlers();

		// addVerticleHandler();
		// addServiceHandler();

	}

	private void addProjectHandlers() {
		route("/projects/:name").method(GET).handler(ctx -> {
			String name = ctx.request().params().get("name");
			// TODO add check whether project was already registered/added
				Project project = projectRepository.findByName(name);
				if (project == null) {
					project = new Project(name);
					projectRepository.save(project);
				}
				try {
					springConfig.routerStorage().addProjectRouter(name);
					log.info("Registered project {" + name + "}");
					ctx.response().end("Registered project {" + name + "}");
				} catch (Exception e) {
					ctx.fail(409);
					ctx.fail(e);
				}
			});

		route("/projects/:name").method(DELETE).handler(ctx -> {
			String name = ctx.request().params().get("name");
			springConfig.routerStorage().removeProjectRouter(name);
			ctx.response().end("Deleted project {" + name + "}");
		});

		route("/projects/").method(POST).handler(ctx -> {
			// TODO also create a default object schema for the project. Move this into service class
			// ObjectSchema defaultContentSchema = objectSchemaService.findByName(, name)
			});

		route("/projects/:nameOrUuid").method(PUT).handler(ctx -> {

		});
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		if (gitChecker != null) {
			gitChecker.close();
		}
	}

	private void addNeo4VertxRestartHandler() {
		route("/neo4vertx/restart").method(GET).handler(ctx -> {
			try {
				caiLunConfig.neo4VertxVerticle().stop();
				caiLunConfig.neo4VertxVerticle().config().put(Neo4VertxConfiguration.PATH_KEY, "/tmp/backup");
				caiLunConfig.neo4VertxVerticle().start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

	}

	private void addBackupHandler() {
		route("/backup").method(GET).handler(ctx -> {
			log.info("Backup started");
			// TODO handle path by config setting
			// TODO check for admin role
				File backupPath = new File("/tmp/backup");
				OnlineBackup backup = OnlineBackup.from("127.0.0.1");
				backup.backup(backupPath.getAbsolutePath());
				log.info("Backup completed");
			});

	}

	private void addServiceHandler() {
		route("/deployService/:mavenCoordinates").method(GET).handler(rc -> {
			// TODO impl me
				rc.response().end("Deploy " + rc.request().params().get("mavenCoordinates"));
			});

		route("/undeployService/:mavenCoordinates").method(GET).handler(rc -> {
			// TODO impl me
				rc.response().end("Undeploy " + rc.request().params().get("mavenCoordinates"));
			});

	}

	private void addVerticleHandler() {
		route("/deployVerticle/:clazz").method(GET).handler(rc -> {
			String clazz = rc.request().params().get("clazz");
			try {
				// TODO create merged jsonconfig (see cailun init)
				JsonObject config = new JsonObject();
				String id = deployAndWait(vertx, config, clazz);
				rc.response().end("Deployed " + clazz + " id: " + id);
			} catch (Exception e) {
				rc.fail(e);
			}
		});

		route("/undeployVerticle/:clazz").method(GET).handler(rc -> {
			// TODO impl me
				rc.response().end("Undeploy " + rc.request().params().get("clazz"));
			});
	}

}
