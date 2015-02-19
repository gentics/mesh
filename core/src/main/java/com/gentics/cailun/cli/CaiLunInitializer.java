package com.gentics.cailun.cli;

import static com.gentics.cailun.util.DeploymentUtils.deployAndWait;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.core.spi.cluster.VertxSPI;
import io.vertx.spi.cluster.impl.hazelcast.HazelcastClusterManager;

import javax.naming.InvalidNameException;

import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.repository.CaiLunRootRepository;
import com.gentics.cailun.core.repository.GroupRepository;
import com.gentics.cailun.core.repository.LanguageRepository;
import com.gentics.cailun.core.repository.ProjectRepository;
import com.gentics.cailun.core.repository.RoleRepository;
import com.gentics.cailun.core.repository.UserRepository;
import com.gentics.cailun.core.rest.model.CaiLunRoot;
import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.Project;
import com.gentics.cailun.core.rest.model.auth.Group;
import com.gentics.cailun.core.rest.model.auth.Role;
import com.gentics.cailun.core.rest.model.auth.User;
import com.gentics.cailun.etc.CaiLunCustomLoader;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.gentics.cailun.etc.CaiLunVerticleConfiguration;
import com.gentics.cailun.etc.config.CaiLunConfiguration;

public class CaiLunInitializer {

	private static Logger log = LoggerFactory.getLogger(CaiLunInitializer.class);

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
	CaiLunSpringConfiguration springConfiguration;

	/**
	 * Load verticles that are configured within the cailun configuration.
	 */
	private void loadConfiguredVerticles() {
		for (String verticleName : configuration.getVerticles().keySet()) {
			CaiLunVerticleConfiguration verticleConf = configuration.getVerticles().get(verticleName);
			try {
				log.info("Loading configured verticle {" + verticleName + "}.");
				deployAndWait(springConfiguration.vertx(), verticleConf.getVerticleConfig(), verticleName);
			} catch (InterruptedException e) {
				log.error("Could not load verticle {" + verticleName + "}.", e);
			}
		}

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
