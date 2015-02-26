package com.gentics.cailun.cli;

import static com.gentics.cailun.util.DeploymentUtils.deployAndWait;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.core.spi.cluster.VertxSPI;
import io.vertx.spi.cluster.impl.hazelcast.HazelcastClusterManager;

import java.util.HashMap;
import java.util.Map;

import javax.naming.InvalidNameException;

import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.data.model.CaiLunRoot;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.ObjectSchemaService;
import com.gentics.cailun.core.repository.CaiLunRootRepository;
import com.gentics.cailun.core.repository.GroupRepository;
import com.gentics.cailun.core.repository.LanguageRepository;
import com.gentics.cailun.core.repository.ProjectRepository;
import com.gentics.cailun.core.repository.RoleRepository;
import com.gentics.cailun.core.repository.UserRepository;
import com.gentics.cailun.core.verticle.ContentVerticle;
import com.gentics.cailun.core.verticle.GroupVerticle;
import com.gentics.cailun.core.verticle.ObjectSchemaVerticle;
import com.gentics.cailun.core.verticle.ProjectVerticle;
import com.gentics.cailun.core.verticle.RoleVerticle;
import com.gentics.cailun.core.verticle.TagVerticle;
import com.gentics.cailun.core.verticle.UserVerticle;
import com.gentics.cailun.etc.CaiLunCustomLoader;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.gentics.cailun.etc.CaiLunVerticleConfiguration;
import com.gentics.cailun.etc.config.CaiLunConfiguration;

public class CaiLunInitializer {

	private static Logger log = LoggerFactory.getLogger(CaiLunInitializer.class);

	private Map<String, Class<? extends AbstractVerticle>> mandatoryVerticles = new HashMap<>();

	private CaiLunConfiguration configuration;

	@Autowired
	CaiLunRootRepository rootRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	GroupRepository groupRepository;

	@Autowired
	RoleRepository roleRepository;

	@Autowired
	ProjectRepository projectRepository;

	@Autowired
	LanguageRepository languageRepository;

	@Autowired
	ObjectSchemaService objectSchemaService;

	@Autowired
	CaiLunSpringConfiguration springConfiguration;

	public CaiLunInitializer() {
		addMandatoryVerticle(TagVerticle.class);
		addMandatoryVerticle(ContentVerticle.class);
		addMandatoryVerticle(UserVerticle.class);

		addMandatoryVerticle(GroupVerticle.class);
		addMandatoryVerticle(RoleVerticle.class);
		addMandatoryVerticle(ProjectVerticle.class);
		addMandatoryVerticle(ObjectSchemaVerticle.class);
		// mandatoryVerticles.add(SearchVerticle.class);
		// mandatoryVerticles.add(AuthenticationVerticle.class);
		// mandatoryVerticles.add(AdminVerticle.class);

	}

	private void addMandatoryVerticle(Class<? extends AbstractVerticle> clazz) {
		mandatoryVerticles.put(clazz.getSimpleName(), clazz);
	}

	/**
	 * Load verticles that are configured within the cailun configuration.
	 * 
	 * @throws InterruptedException
	 */
	private void loadConfiguredVerticles() throws InterruptedException {
		JsonObject defaultConfig = new JsonObject();
		defaultConfig.put("port", configuration.getHttpPort());

		for (Class<? extends AbstractVerticle> clazz : getMandatoryVerticleClasses().values()) {
			try {
				log.info("Loading mandatory verticle {" + clazz.getName() + "}.");
				// TODO handle custom config? i assume we will not allow this
				deployAndWait(springConfiguration.vertx(), defaultConfig, clazz);
			} catch (InterruptedException e) {
				log.error("Could not load mandatory verticle {" + clazz.getSimpleName() + "}.", e);
			}
		}

		for (String verticleName : configuration.getVerticles().keySet()) {
			if (getMandatoryVerticleClasses().containsKey(verticleName)) {
				log.error("Can't configure mandatory verticles. Skipping configured verticle {" + verticleName + "}");
				continue;
			}
			CaiLunVerticleConfiguration verticleConf = configuration.getVerticles().get(verticleName);
			JsonObject mergedVerticleConfig = new JsonObject();
			if (verticleConf.getVerticleConfig() != null) {
				mergedVerticleConfig = verticleConf.getVerticleConfig().copy();
			}
			mergedVerticleConfig.put("port", configuration.getHttpPort());
			try {
				log.info("Loading configured verticle {" + verticleName + "}.");
				deployAndWait(springConfiguration.vertx(), mergedVerticleConfig, verticleName);
			} catch (InterruptedException e) {
				log.error("Could not load verticle {" + verticleName + "}.", e);
			}
		}
	}

	private Map<String, Class<? extends AbstractVerticle>> getMandatoryVerticleClasses() {
		return mandatoryVerticles;
	}

	/**
	 * Initialize cailun.
	 * 
	 * @param configuration
	 * @param verticleLoader
	 * @throws Exception
	 */
	public void init(CaiLunConfiguration configuration, CaiLunCustomLoader<Vertx> verticleLoader) throws Exception {
		this.configuration = configuration;
		if (configuration.isClusterMode()) {
			joinCluster();
		}
		try (Transaction tx = springConfiguration.getGraphDatabaseService().beginTx()) {
			initMandatoryData();
			tx.success();
		}
		loadConfiguredVerticles();
		if (verticleLoader != null) {
			verticleLoader.apply(springConfiguration.vertx());
		}
		initProjects();

	}

	/**
	 * The projects share various subrouters. This method will add the subrouters for all registered projects.
	 * 
	 * @throws InvalidNameException
	 */
	private void initProjects() throws InvalidNameException {
		try (Transaction tx = springConfiguration.getGraphDatabaseService().beginTx()) {
			for (Project project : projectRepository.findAll()) {
				springConfiguration.routerStorage().addProjectRouter(project.getName());
				log.info("Initalized project {" + project.getName() + "}");
			}
			tx.success();
		}
	}

	/**
	 * Use the hazelcast cluster manager to join the cluster of cailun instances.
	 */
	private void joinCluster() {
		HazelcastClusterManager manager = new HazelcastClusterManager();
		manager.setVertx((VertxSPI) springConfiguration.vertx());
		manager.join(rh -> {
			if (!rh.succeeded()) {
				log.error("Error while joining cailun cluster.", rh.cause());
			}
		});
	}

	/**
	 * Setup various mandatory data. This includes mandatory root nodes and the admin user, group.
	 */
	public void initMandatoryData() {

		// Verify that the root node is existing
		CaiLunRoot rootNode = rootRepository.findRoot();
		if (rootNode == null) {
			rootNode = new CaiLunRoot();
			rootRepository.save(rootNode);
			log.info("Stored root node");
		}
		// Reload the node to get one with a valid uuid
		// TODO check whether this really works. I assume i would have to commit first.
		rootNode = rootRepository.findRoot();

		Language german = languageRepository.findByName("german");
		if (german == null) {
			german = new Language("german", "de_DE");
			rootNode.addLanguage(languageRepository.save(german));
			rootRepository.save(rootNode);
		}

		Language english = languageRepository.findByName("english");
		if (english == null) {
			english = new Language("english", "en_US");
			rootNode.addLanguage(languageRepository.save(english));
			rootRepository.save(rootNode);
		}

		// Verify that an admin user exists
		User adminUser = userRepository.findByUsername("admin");
		if (adminUser == null) {
			adminUser = new User("admin");
			System.out.println("Enter admin password:");
			// Scanner scanIn = new Scanner(System.in);
			// String pw = scanIn.nextLine();
			// TODO remove later on
			String pw = "finger";
			// scanIn.close();
			adminUser.setPasswordHash(springConfiguration.passwordEncoder().encode(pw));
			userRepository.save(adminUser);
			log.info("Stored admin user");
		}
		rootNode.getUsers().add(adminUser);
		rootRepository.save(rootNode);

		Group adminGroup = groupRepository.findByName("admin");
		if (adminGroup == null) {
			adminGroup = new Group("admin");
			adminGroup.getMembers().add(adminUser);
			groupRepository.save(adminGroup);
			log.info("Stored admin group");
		}
		rootNode.setRootGroup(adminGroup);
		rootRepository.save(rootNode);

		Role adminRole = roleRepository.findByName("admin");
		if (adminRole == null) {
			adminRole = new Role("admin");
			adminGroup.addRole(adminRole);
		}

	}
}
