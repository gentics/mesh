package com.gentics.cailun.verticle.admin;

import static com.gentics.cailun.util.DeploymentUtils.deployAndWait;
import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.ext.graph.neo4j.Configuration;

import java.io.File;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.backup.OnlineBackup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.auth.Neo4jSpringConfiguration;
import com.gentics.cailun.core.AbstractCailunRestVerticle;

@Component
@Scope("singleton")
@SpringVerticle
public class AdminVerticle extends AbstractCailunRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(AdminVerticle.class);

	@Autowired
	Neo4jSpringConfiguration neo4jConfig;

	public AdminVerticle() {
		super("admin");
	}

	@Override
	public void start() throws Exception {
		super.start();

		addBackupHandler();
		addNeo4VertxRestartHandler();

		// addVerticleHandler();
		// addServiceHandler();

	}

	private void addNeo4VertxRestartHandler() {
		route("/neo4vertx/restart").method(GET).handler(ctx -> {
			try {
				neo4jConfig.neo4VertxVerticle().stop();
				neo4jConfig.neo4VertxVerticle().config().put(Configuration.PATH_KEY, "/tmp/backup");
				neo4jConfig.neo4VertxVerticle().start();
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
				String id = deployAndWait(vertx, clazz);
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
