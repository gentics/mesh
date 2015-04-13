package com.gentics.cailun.demo;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.cailun.cli.BootstrapInitializer;
import com.gentics.cailun.cli.CaiLun;
import com.gentics.cailun.core.data.model.CaiLunRoot;
import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.PropertyType;
import com.gentics.cailun.core.data.model.PropertyTypeSchema;
import com.gentics.cailun.core.data.model.RootTag;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.AuthRelationships;
import com.gentics.cailun.core.data.model.auth.GraphPermission;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.CaiLunRootService;
import com.gentics.cailun.core.data.service.ContentService;
import com.gentics.cailun.core.data.service.GroupService;
import com.gentics.cailun.core.data.service.LanguageService;
import com.gentics.cailun.core.data.service.ObjectSchemaService;
import com.gentics.cailun.core.data.service.ProjectService;
import com.gentics.cailun.core.data.service.RoleService;
import com.gentics.cailun.core.data.service.TagService;
import com.gentics.cailun.core.data.service.UserService;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;

@Component
public class DemoDataProvider {

	private static final Logger log = LoggerFactory.getLogger(CaiLun.class);

	public static final String PROJECT_NAME = "dummy";
	public static final String TAG_CATEGORIES_SCHEMA_NAME = "tagCategories";
	public static final String TAG_DEFAULT_SCHEMA_NAME = "tag";
	private static SecureRandom random = new SecureRandom();

	private int totalTags = 0;
	private int totalContents = 0;

	private int totalUsers = 0;
	private int totalGroups = 0;
	private int totalRoles = 0;

	@Autowired
	private UserService userService;

	@Autowired
	private CaiLunRootService rootService;

	@Autowired
	private GroupService groupService;

	@Autowired
	private LanguageService languageService;

	@Autowired
	private ContentService contentService;

	@Autowired
	private TagService tagService;

	@Autowired
	private RoleService roleService;

	@Autowired
	private ProjectService projectService;

	@Autowired
	private ObjectSchemaService objectSchemaService;

	@Autowired
	private GraphDatabaseService graphDb;

	@Autowired
	private Neo4jTemplate neo4jTemplate;

	@Autowired
	protected CaiLunSpringConfiguration springConfig;

	@Autowired
	private BootstrapInitializer bootstrapInitializer;

	// References to dummy data

	private CaiLunRoot root;

	private RootTag rootTag;

	private Language english;

	private Language german;

	private Project project;

	private ObjectSchema contentSchema;

	private ObjectSchema tagSchema;

	private ObjectSchema categoriesSchema;

	private UserInfo userInfo;

	/**
	 * Path: /news
	 */
	private Tag news;
	/**
	 * Path: /news/news2015
	 */
	private Tag news2015;

	/**
	 * Path: /news/news2015/Special News_2014.de.html
	 */
	private Content news2015Content;
	private Content dealsSuperDeal;
	private Tag productsTag;
	private Tag deals;

	private DemoDataProvider() {
	}

	public UserInfo createUserInfo(String username, String firstname, String lastname) {

		String password = "test123";
		String email = firstname.toLowerCase().substring(0, 1) + "." + lastname.toLowerCase() + "@spam.gentics.com";

		User user = new User(username);
		userService.setPassword(user, password);
		log.info("Creating user with username: " + username + " and password: " + password);
		user.setFirstname(firstname);
		user.setLastname(lastname);
		user.setEmailAddress(email);
		userService.save(user);
		totalUsers++;

		Role role = new Role(username + "_role");
		roleService.save(role);
		totalRoles++;

		Group group = new Group(username + "_group");
		group.addUser(user);
		group.addRole(role);
		group = groupService.save(group);
		totalGroups++;

		UserInfo userInfo = new UserInfo(user, group, role, password);
		return userInfo;

	}

	public void setup(int multiplicator) throws JsonParseException, JsonMappingException, IOException {
		try (Transaction tx = graphDb.beginTx()) {
			bootstrapInitializer.initMandatoryData();
			tx.success();
		}

		addUserGroupRoleProject(multiplicator);
		addSchemas(multiplicator);
		addData(multiplicator);
		updatePermissions();
	}

	private void addUserGroupRoleProject(int multiplicator) {
		try (Transaction tx = springConfig.getGraphDatabaseService().beginTx()) {

			// User, Groups, Roles
			userInfo = createUserInfo("joe1", "Joe", "Doe");

			project = new Project(PROJECT_NAME);
			project.setCreator(userInfo.getUser());
			project = projectService.save(project);

			english = languageService.findByLanguageTag("en");
			german = languageService.findByLanguageTag("de");

			// Guest Group / Role
			Role guestRole = new Role("guest_role");
			roleService.save(guestRole);
			totalRoles++;

			Group guests = new Group("guests");
			guests.addRole(guestRole);
			guests = groupService.save(guests);
			totalGroups++;

			// Extra User
			for (int i = 0; i < 12 * multiplicator; i++) {
				User user = new User("guest_" + i);
				// userService.setPassword(user, "guestpw" + i);
				user.setFirstname("Guest Firstname");
				user.setLastname("Guest Lastname");
				user.setEmailAddress("guest_" + i + "@spam.gentics.com");
				userService.save(user);
				guests.addUser(user);
				guests = groupService.save(guests);
				totalUsers++;
			}
			// Extra Groups
			for (int i = 0; i < 12 * multiplicator; i++) {
				Group group = new Group("extra_group_" + i);
				group = groupService.save(group);
				totalGroups++;
			}

			// Extra Roles
			for (int i = 0; i < 12 * multiplicator; i++) {
				Role role = new Role("extra_role_" + i);
				roleService.save(role);
				totalRoles++;
			}
			tx.success();
		}

		userInfo.setGroup(groupService.reload(userInfo.getGroup()));
		userInfo.setUser(userService.reload(userInfo.getUser()));
		userInfo.setRole(roleService.reload(userInfo.getRole()));
		project = projectService.reload(project);
	}

	private void addSchemas(int multiplicator) {
		try (Transaction tx = springConfig.getGraphDatabaseService().beginTx()) {

			tagSchema = objectSchemaService.findByName("tag");
			contentSchema = objectSchemaService.findByName("content");

			categoriesSchema = new ObjectSchema(TAG_CATEGORIES_SCHEMA_NAME);
			categoriesSchema.addProject(project);
			categoriesSchema.setDisplayName("Category");
			categoriesSchema.setDescription("Custom schema for tag categories");
			categoriesSchema.setCreator(userInfo.getUser());
			PropertyTypeSchema nameProp = new PropertyTypeSchema(Content.NAME_KEYWORD, PropertyType.I18N_STRING);
			nameProp.setDisplayName("Name");
			nameProp.setDescription("The name of the category.");
			categoriesSchema.addPropertyTypeSchema(nameProp);

			PropertyTypeSchema filenameProp = new PropertyTypeSchema(Content.FILENAME_KEYWORD, PropertyType.I18N_STRING);
			filenameProp.setDisplayName("Filename");
			filenameProp.setDescription("The filename property of the category.");
			categoriesSchema.addPropertyTypeSchema(filenameProp);

			PropertyTypeSchema contentProp = new PropertyTypeSchema(Content.CONTENT_KEYWORD, PropertyType.I18N_STRING);
			contentProp.setDisplayName("Content");
			contentProp.setDescription("The main content html of the category.");
			categoriesSchema.addPropertyTypeSchema(contentProp);
			objectSchemaService.save(categoriesSchema);
			tx.success();
		}

	}

	private void updatePermissions() {
		// // Add Permissions
		// try (Transaction tx = graphDb.beginTx()) {
		// // Add admin permissions to all nodes
		// int i = 0;
		// for (GenericNode currentNode : genericNodeService.findAll()) {
		// currentNode = genericNodeService.reload(currentNode);
		// log.info("Adding BasicPermission to node {" + currentNode.getId() + "}");
		// if (adminRole.getId() == currentNode.getId()) {
		// log.info("Skipping role");
		// continue;
		// }
		// roleService.addPermission(adminRole, currentNode, CREATE, READ, UPDATE, DELETE);
		// adminRole = roleService.save(adminRole);
		// log.info("Added permissions to {" + i + "} objects.");
		// i++;
		// }
		// tx.success();
		// }

		// TODO determine why this is not working when using sdn
		// Add Permissions
		try (Transaction tx = graphDb.beginTx()) {
			Node roleNode = neo4jTemplate.getPersistentState(userInfo.getRole());
			int i = 0;
			for (Node node : GlobalGraphOperations.at(graphDb).getAllNodes()) {

				if (roleNode.getId() == node.getId()) {
					log.info("Skipping own role");
					continue;
				}
				Relationship rel = roleNode.createRelationshipTo(node, AuthRelationships.TYPES.HAS_PERMISSION);
				rel.setProperty("__type__", GraphPermission.class.getSimpleName());
				rel.setProperty("permissions-read", true);
				rel.setProperty("permissions-delete", true);
				rel.setProperty("permissions-create", true);
				rel.setProperty("permissions-update", true);
				// GenericNode sdnNode = neo4jTemplate.projectTo(node, GenericNode.class);
				// roleService.addPermission(adminRole, sdnNode, CREATE, READ, UPDATE, DELETE);
				// genericNodeService.save(node);
				log.info("Adding BasicPermission to node {" + node.getId() + "} " + i);
				i++;
			}
			tx.success();
		}

	}

	@SuppressWarnings("unchecked")
	private void addData(int multiplicator) {
		try (Transaction tx = graphDb.beginTx()) {

			// Contents, Tags, Projects
			root = rootService.findRoot();
			root.addUser(userInfo.getUser());
			rootService.save(root);

			// Root Tag
			rootTag = new RootTag();
			rootTag = (RootTag) tagService.save(rootTag);
			rootTag.setCreator(userInfo.getUser());
			totalTags++;
			project.setRootTag(rootTag);
			project = projectService.save(project);
			project = projectService.reload(project);

			// News - 2014
			news = addTag(rootTag, "News", "Neuigkeiten", tagSchema);
			totalTags++;

			Tag news2014 = addTag(news, "2014", null, tagSchema);
			totalTags++;

			for (int i = 0; i < 12 * multiplicator; i++) {
				addContent(news2014, "News_2014_" + i, "News " + i + "!", "Neuigkeiten " + i + "!", contentSchema);
				totalContents++;
			}

			// News - 2015
			news2015 = addTag(news2014, "2015", null, tagSchema);
			totalTags++;
			news2015Content = addContent(news2015, "Special News_2014", "News!", "Neuigkeiten!", contentSchema);
			totalContents++;
			for (int i = 0; i < 12 * multiplicator; i++) {
				addContent(news2015, "News_2015_" + i, "News" + i + "!", "Neuigkeiten " + i + "!", contentSchema);
				totalContents++;
			}

			// Tags for categories
			Tag categories = addTag(rootTag, "categories", null, categoriesSchema);
			totalTags++;

			Tag vehicle = addTag(rootTag, "Vehicle", "Fahrzeug", categoriesSchema);
			totalTags++;

			Tag car = addTag(vehicle, "Car", "Auto", categoriesSchema);
			totalTags++;

			Tag jeep = addTag(car, "Jeep", null, categoriesSchema);
			totalTags++;

			Tag bike = addTag(vehicle, "Bike", "Fahrrad", categoriesSchema);
			totalTags++;

			Tag motorcycle = addTag(vehicle, "Motorcycle", "Motorrad", categoriesSchema);
			totalTags++;

			Tag bus = addTag(vehicle, "Bus", "Bus", categoriesSchema);
			totalTags++;

			Tag plane = addTag(rootTag, "Plane", "Flugzeug", categoriesSchema);
			totalTags++;

			Tag jetFighter = addTag(plane, "JetFigther", "Düsenjäger", categoriesSchema);
			totalTags++;

			Tag twinjet = addTag(plane, "Twinjet", "Zweistrahliges Flugzeug", categoriesSchema);
			totalTags++;

			// productsTag
			Map<String, Content> products = new HashMap<>();
			productsTag = addTag(rootTag, "products", "Produkte", tagSchema);
			totalTags++;
			productsTag.addContent(news2015Content);
			productsTag = tagService.save(productsTag);

			Content porsche911 = addContent(
					productsTag,
					"Porsche 911",
					"997 is the internal designation for the Porsche 911 model manufactured and sold by German manufacturer Porsche between 2004 (as Model Year 2005) and 2012.",
					"Porsche 997 ist die interne Modellbezeichnung von Porsche für das von 2004 bis Ende 2012 produzierte 911-Modell.", contentSchema);
			porsche911.addTag(vehicle);
			porsche911.addTag(car);
			products.put("Porsche 911", porsche911);
			totalContents++;

			Content nissanGTR = addContent(
					productsTag,
					"Nissan GT-R",
					"The Nissan GT-R is a 2-door 2+2 sports coupé produced by Nissan and first released in Japan in 2007",
					"Der Nissan GT-R ist ein seit Dezember 2007 produziertes Sportcoupé des japanischen Automobilherstellers Nissan und der Nachfolger des Nissan Skyline GT-R R34.",
					contentSchema);
			nissanGTR.addTag(vehicle);
			nissanGTR.addTag(car);
			products.put("Nissan GTR", nissanGTR);
			totalContents++;

			Content bmwM3 = addContent(
					productsTag,
					"BMW M3",
					"The BMW M3 (first launched in 1986) is a high-performance version of the BMW 3-Series, developed by BMW's in-house motorsport division, BMW M.",
					"Der BMW M3 ist ein Sportmodell der 3er-Reihe von BMW, das seit Anfang 1986 hergestellt wird. Dabei handelt es sich um ein Fahrzeug, welches von der BMW-Tochterfirma BMW M GmbH entwickelt und anfangs (E30 und E36) auch produziert wurde.",
					contentSchema);
			bmwM3.addTag(vehicle);
			bmwM3.addTag(car);
			products.put("BMW M3", bmwM3);
			totalContents++;

			Content concorde = addContent(
					productsTag,
					"Concorde",
					"Aérospatiale-BAC Concorde is a turbojet-powered supersonic passenger jet airliner that was in service from 1976 to 2003.",
					"Die Aérospatiale-BAC Concorde 101/102, kurz Concorde (französisch und englisch für Eintracht, Einigkeit), ist ein Überschall-Passagierflugzeug, das von 1976 bis 2003 betrieben wurde.",
					contentSchema);
			concorde.addTag(plane);
			concorde.addTag(twinjet);
			products.put("Concorde", concorde);
			totalContents++;

			Content boeing737 = addContent(
					productsTag,
					"Boeing 737",
					"The Boeing 737 is a short- to medium-range twinjet narrow-body airliner. Originally developed as a shorter, lower-cost twin-engined airliner derived from Boeing's 707 and 727, the 737 has developed into a family of nine passenger models with a capacity of 85 to 215 passengers.",
					"Die Boeing 737 des US-amerikanischen Flugzeugherstellers Boeing ist die weltweit meistgebaute Familie strahlgetriebener Verkehrsflugzeuge.",
					contentSchema);
			boeing737.addTag(plane);
			boeing737.addTag(twinjet);
			products.put("Boeing 737", boeing737);
			totalContents++;

			Content a300 = addContent(
					productsTag,
					"Airbus A300",
					"The Airbus A300 is a short- to medium-range wide-body twin-engine jet airliner that was developed and manufactured by Airbus. Released in 1972 as the world's first twin-engined widebody, it was the first product of Airbus Industrie, a consortium of European aerospace manufacturers, now a subsidiary of Airbus Group.",
					"Der Airbus A300 ist das erste zweistrahlige Großraumflugzeug der Welt, produziert vom europäischen Flugzeughersteller Airbus.",
					contentSchema);
			a300.addTag(plane);
			a300.addTag(twinjet);
			products.put("Airbus A300", a300);
			totalContents++;

			Content wrangler = addContent(
					productsTag,
					"Jeep Wrangler",
					"The Jeep Wrangler is a compact and mid-size (Wrangler Unlimited models) four-wheel drive off-road and sport utility vehicle (SUV), manufactured by American automaker Chrysler, under its Jeep marque – and currently in its third generation.",
					"Der Jeep Wrangler ist ein Geländewagen des US-amerikanischen Herstellers Jeep innerhalb des Chrysler-Konzerns.", contentSchema);
			wrangler.addTag(vehicle);
			wrangler.addTag(jeep);
			products.put("Jeep Wrangler", wrangler);
			totalContents++;

			Content volvo = addContent(productsTag, "Volvo B10M",
					"The Volvo B10M was a mid-engined bus and coach chassis manufactured by Volvo between 1978 and 2003.", null, contentSchema);
			volvo.addTag(vehicle);
			volvo.addTag(bus);
			products.put("Volvo B10M", volvo);
			totalContents++;

			Content hondact90 = addContent(productsTag, "Honda CT90",
					"The Honda CT90 was a small step-through motorcycle manufactured by Honda from 1966 to 1979.", null, contentSchema);
			hondact90.addTag(vehicle);
			hondact90.addTag(motorcycle);
			products.put("Honda CT90", hondact90);
			totalContents++;

			Content hondaNR = addContent(
					productsTag,
					"Honda NR",
					"The Honda NR (New Racing) was a V-four motorcycle engine series started by Honda in 1979 with the 500cc NR500 Grand Prix racer that used oval pistons.",
					"Die NR750 ist ein Motorrad mit Ovalkolben-Motor des japanischen Motorradherstellers Honda, von dem in den Jahren 1991 und 1992 300 Exemplare gebaut wurden.",
					contentSchema);
			hondaNR.addTag(vehicle);
			hondaNR.addTag(motorcycle);
			products.put("Honda NR", hondaNR);
			totalContents++;

			// Deals
			deals = addTag(rootTag, "Deals", "Angebote", tagSchema);
			totalTags++;

			dealsSuperDeal = addContent(deals, "Super Special Deal 2015", "Buy two get nine!", "Kauf zwei und nimm neun mit!", contentSchema);
			totalContents++;
			for (int i = 0; i < 12 * multiplicator; i++) {
				addContent(deals, "Special Deal June 2015 - " + i, "Buy two get three! " + i, "Kauf zwei und nimm drei mit!" + i, contentSchema);
				totalContents++;
			}

			tx.success();
		}
		news = tagService.reload(news);
		news2015 = tagService.reload(news2015);
		news2015Content = contentService.reload(news2015Content);
		dealsSuperDeal = contentService.reload(dealsSuperDeal);

		productsTag = tagService.reload(productsTag);
		deals = tagService.reload(deals);

	}

	private Tag addTag(Tag rootTag, String englishName, String germanName, ObjectSchema schema) {
		Tag tag = new Tag();
		if (englishName != null) {
			tagService.setName(tag, english, englishName);
		}
		if (germanName != null) {
			tagService.setName(tag, german, germanName);
		}
		tag.addProject(project);
		tag.setSchema(schema);
		tag.setCreator(userInfo.getUser());
		tag = tagService.save(tag);
		rootTag.addTag(tag);
		tagService.save(rootTag);
		return tag;
	}

	private Content addContent(Tag tag, String name, String englishContent, String germanContent, ObjectSchema schema) {
		Content content = new Content();
		contentService.setName(content, english, name + " english");
		contentService.setFilename(content, english, name + ".en.html");
		contentService.setContent(content, english, englishContent);

		if (germanContent != null) {
			contentService.setName(content, german, name + " german");
			contentService.setFilename(content, german, name + ".de.html");
			contentService.setContent(content, german, germanContent);
		}
		// TODO maybe set project should be done inside the save?
		content.addProject(project);
		content.setCreator(userInfo.getUser());
		content.setSchema(schema);
		content = contentService.save(content);

		// Add the content to the given tag
		tag.addContent(content);
		tag = tagService.save(tag);

		return content;
	}

	public CaiLunRoot getCaiLunRoot() {
		return root;
	}

	public Tag getRootTag() {
		return rootTag;
	}

	/**
	 * Tag for Path: /News - /Neuigkeiten
	 * 
	 * @return
	 */
	public Tag getNews() {
		return news;
	}

	/**
	 * Tag for Path: /News/2015 - /Neuigkeiten/2015
	 * 
	 * @return
	 */
	public Tag getNews2015() {
		return news2015;
	}

	/**
	 * Returns the path to the tag for the given language.
	 * 
	 * @param language
	 * @return
	 */
	public String getPathForNews2015Tag(Language language) {
		return getNews().getName(language) + "/" + getNews2015().getName(language);
	}

	/**
	 * Content in path: /News/2015 - /Neuigkeiten/2015
	 * 
	 * @return
	 */
	public Content getNews2015Content() {
		return news2015Content;
	}

	public Content getDealsSuperDeal() {
		return dealsSuperDeal;
	}

	public Tag getDeals() {
		return deals;
	}

	public Tag getproductsTag() {
		return productsTag;
	}

	public Language getEnglish() {
		return english;
	}

	public Language getGerman() {
		return german;
	}

	public Project getProject() {
		return project;
	}

	public ObjectSchema getContentSchema() {
		return contentSchema;
	}

	public UserInfo getUserInfo() {
		return userInfo;
	}

	public int getTotalContents() {
		return totalContents;
	}

	public int getTotalTags() {
		return totalTags;
	}

	public int getTotalGroups() {
		return totalGroups;
	}

	public int getTotalRoles() {
		return totalRoles;
	}

	public int getTotalUsers() {
		return totalUsers;
	}

}
