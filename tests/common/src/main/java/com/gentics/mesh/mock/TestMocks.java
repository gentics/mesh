package com.gentics.mesh.mock;

import static com.gentics.mesh.example.AbstractExamples.TIMESTAMP_NEW;
import static com.gentics.mesh.example.AbstractExamples.TIMESTAMP_OLD;
import static com.gentics.mesh.example.ExampleUuids.GROUP_CLIENT_UUID;
import static com.gentics.mesh.example.ExampleUuids.LANGUAGE_UUID;
import static com.gentics.mesh.example.ExampleUuids.MICROSCHEMA_UUID;
import static com.gentics.mesh.example.ExampleUuids.NODE_DELOREAN_UUID;
import static com.gentics.mesh.example.ExampleUuids.PROJECT_DEMO2_UUID;
import static com.gentics.mesh.example.ExampleUuids.ROLE_CLIENT_UUID;
import static com.gentics.mesh.example.ExampleUuids.SCHEMA_VEHICLE_UUID;
import static com.gentics.mesh.example.ExampleUuids.TAGFAMILY_FUELS_UUID;
import static com.gentics.mesh.example.ExampleUuids.TAG_BLUE_UUID;
import static com.gentics.mesh.example.ExampleUuids.USER_EDITOR_UUID;
import static com.gentics.mesh.example.ExampleUuids.UUID_1;
import static com.gentics.mesh.example.ExampleUuids.UUID_2;
import static com.gentics.mesh.example.ExampleUuids.UUID_3;
import static com.gentics.mesh.example.ExampleUuids.UUID_4;
import static com.gentics.mesh.example.ExampleUuids.UUID_5;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.mockito.Mockito;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.group.Group;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BooleanField;
import com.gentics.mesh.core.data.node.field.DateField;
import com.gentics.mesh.core.data.node.field.HtmlField;
import com.gentics.mesh.core.data.node.field.NumberField;
import com.gentics.mesh.core.data.node.field.StringField;
import com.gentics.mesh.core.data.node.field.list.BooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.DateFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberFieldList;
import com.gentics.mesh.core.data.node.field.list.StringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.NodeField;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.tag.Tag;
import com.gentics.mesh.core.data.tagfamily.TagFamily;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.result.TraversalResult;

public final class TestMocks {

	private TestMocks() {

	}

	public static Project mockProject(User user) {
		Project project = mock(Project.class);
		when(project.getUuid()).thenReturn(PROJECT_DEMO2_UUID);
		when(project.getName()).thenReturn("dummyProject");
		when(project.getCreator()).thenReturn(user);
		when(project.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(project.getEditor()).thenReturn(user);
		when(project.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		when(project.getElementVersion()).thenReturn(UUID_1);
		return project;
	}

	public static Language mockLanguage(String code) {
		Language language = mock(Language.class);
		when(language.getUuid()).thenReturn(LANGUAGE_UUID);
		when(language.getLanguageTag()).thenReturn("de");
		when(language.getElementVersion()).thenReturn(UUID_2);
		return language;
	}

	public static Node mockNodeBasic(String schemaType, User user) {
		Node node = mock(Node.class);
		when(node.getUuid()).thenReturn(NODE_DELOREAN_UUID);
		Schema schemaContainer = mockSchemaContainer(schemaType, user);
		when(node.getSchemaContainer()).thenReturn(schemaContainer);
		return node;
	}

	public static Micronode mockMicronode(String microschemaName, User user) {
		Micronode micronode = mock(Micronode.class);
		when(micronode.getUuid()).thenReturn(UUID_1);
		Microschema microschemaContainer = mockMicroschemaContainer(microschemaName, user);
		MicroschemaVersion latestVersion = microschemaContainer.getLatestVersion();
		// TODO find a away to convert this correctly
		when(micronode.getSchemaContainerVersion()).thenReturn(latestVersion);
		MicroschemaVersionModel microschema = microschemaContainer.getLatestVersion().getSchema();
		when(micronode.getSchemaContainerVersion().getSchema()).thenReturn(microschema);

		// longitude field
		NumberField longitudeField = mock(NumberField.class);
		when(longitudeField.getNumber()).thenReturn(16.373063840833);
		when(micronode.getNumber("longitude")).thenReturn(longitudeField);

		// latitude field
		NumberField latitudeField = mock(NumberField.class);
		when(latitudeField.getNumber()).thenReturn(16.373063840833);
		when(micronode.getNumber("latitude")).thenReturn(latitudeField);
		when(micronode.getElementVersion()).thenReturn(UUID_3);
		return micronode;
	}

	public static Role mockRole(String roleName, User creator) {
		Role role = mock(Role.class);
		when(role.getCreator()).thenReturn(creator);
		when(role.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(role.getEditor()).thenReturn(creator);
		when(role.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		when(role.getName()).thenReturn(roleName);
		when(role.getUuid()).thenReturn(ROLE_CLIENT_UUID);
		when(role.getElementVersion()).thenReturn(UUID_4);
		return role;
	}

	public static Group mockGroup(String groupName, User creator) {
		Group group = mock(Group.class);
		when(group.getCreator()).thenReturn(creator);
		when(group.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(group.getEditor()).thenReturn(creator);
		when(group.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		when(group.getName()).thenReturn(groupName);
		when(group.getUuid()).thenReturn(GROUP_CLIENT_UUID);
		//when(group.getRolesWithPerm(InternalPermission.READ_PERM)).thenReturn(createEmptyTraversal());
		when(group.getElementVersion()).thenReturn(UUID_5);
		return group;
	}

	public static User mockUser(String username, String firstname, String lastname) {
		return mockUser(username, firstname, lastname, null);
	}

	public static User mockUser(String username, String firstname, String lastname, User creator) {
		User user = mock(User.class);
		when(user.getUsername()).thenReturn(username);
		when(user.getFirstname()).thenReturn(firstname);
		when(user.getLastname()).thenReturn(lastname);
		when(user.getEmailAddress()).thenReturn(username + "@nowhere.tld");
		when(user.getUuid()).thenReturn(USER_EDITOR_UUID);
		when(user.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(user.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		if (creator != null) {
			when(user.getCreator()).thenReturn(creator);
			when(user.getEditor()).thenReturn(creator);
		}
		when(user.getElementVersion()).thenReturn(UUID_1);
		return user;
	}

	public static TagFamily mockTagFamily(String name, User user, Project project) {
		TagFamily tagFamily = mock(TagFamily.class);
		when(tagFamily.getCreator()).thenReturn(user);
		when(tagFamily.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(tagFamily.getEditor()).thenReturn(user);
		when(tagFamily.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		when(tagFamily.getName()).thenReturn(name);
		when(tagFamily.getUuid()).thenReturn(TAGFAMILY_FUELS_UUID);
		when(tagFamily.getProject()).thenReturn(project);
		when(tagFamily.getElementVersion()).thenReturn(UUID_2);
		return tagFamily;
	}

	public static Tag mockTag(String name, User user, TagFamily tagFamily, Project project) {
		Tag tag = mock(Tag.class);
		when(tag.getCreator()).thenReturn(user);
		when(tag.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(tag.getEditor()).thenReturn(user);
		when(tag.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		when(tag.getName()).thenReturn(name);
		when(tag.getUuid()).thenReturn(TAG_BLUE_UUID);
		when(tag.getTagFamily()).thenReturn(tagFamily);
		when(tag.getProject()).thenReturn(project);
		when(tag.getElementVersion()).thenReturn(UUID_3);
		return tag;
	}

	private static <T> TraversalResult<T> createEmptyTraversal() {
		return new TraversalResult<>(Collections.emptyList());
	}

	public static Schema mockSchemaContainer(String name, User user) {
		Schema container = mock(Schema.class);
		when(container.getName()).thenReturn(name);
		when(container.getUuid()).thenReturn(SCHEMA_VEHICLE_UUID);
		SchemaVersion latestVersion = mock(SchemaVersion.class);
		when(latestVersion.getSchemaContainer()).thenReturn(container);
		when(latestVersion.getSchema()).thenReturn(mockContentSchema());
		when(latestVersion.getName()).thenReturn(name);
		when(container.getLatestVersion()).thenReturn(latestVersion);
		when(container.getCreator()).thenReturn(user);
		when(container.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(container.getEditor()).thenReturn(user);
		when(container.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		//when(container.getRolesWithPerm(InternalPermission.READ_PERM)).thenReturn(createEmptyTraversal());
		return container;
	}

	public static Microschema mockMicroschemaContainer(String name, User user) {
		Microschema schema = mock(Microschema.class);
		when(schema.getName()).thenReturn(name);
		when(schema.getUuid()).thenReturn(MICROSCHEMA_UUID);
		MicroschemaVersion latestVersion = mock(MicroschemaVersion.class);
		when(latestVersion.getSchema()).thenReturn(mockGeolocationMicroschema());

		when(schema.getLatestVersion()).thenReturn(latestVersion);
		when(schema.getCreator()).thenReturn(user);
		when(schema.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(schema.getEditor()).thenReturn(user);
		when(schema.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		//when(schema.getRolesWithPerm(InternalPermission.READ_PERM)).thenReturn(createEmptyTraversal());
		when(schema.getElementVersion()).thenReturn(UUID_5);
		return schema;
	}

	public static SchemaVersionModel mockContentSchema() {
		SchemaVersionModel schema = new SchemaModelImpl();
		schema.setName("content");
		schema.setDescription("Content schema");
		schema.setDisplayField("string");
		// basic types
		schema.addField(new StringFieldSchemaImpl().setName("string").setRequired(true));
		schema.addField(new NumberFieldSchemaImpl().setName("number").setRequired(true));
		schema.addField(new BooleanFieldSchemaImpl().setName("boolean").setRequired(true));
		schema.addField(new DateFieldSchemaImpl().setName("date").setRequired(true));
		schema.addField(new HtmlFieldSchemaImpl().setName("html").setRequired(true));
		schema.addField(new NodeFieldSchemaImpl().setName("node").setRequired(true));
		schema.addField(new MicronodeFieldSchemaImpl().setName("micronode").setRequired(true));

		// lists types
		schema.addField(new ListFieldSchemaImpl().setListType("string").setName("stringList").setRequired(true));
		schema.addField(new ListFieldSchemaImpl().setListType("number").setName("numberList").setRequired(true));
		schema.addField(new ListFieldSchemaImpl().setListType("boolean").setName("booleanList").setRequired(true));
		schema.addField(new ListFieldSchemaImpl().setListType("date").setName("dateList").setRequired(true));
		schema.addField(new ListFieldSchemaImpl().setListType("html").setName("htmlList").setRequired(true));
		schema.addField(new ListFieldSchemaImpl().setListType("node").setName("nodeList").setRequired(true));
		schema.addField(new ListFieldSchemaImpl().setListType("micronode").setName("micronodeList").setRequired(true));

		return schema;
	}

	public static MicroschemaVersionModel mockGeolocationMicroschema() {
		MicroschemaVersionModel microschema = new MicroschemaModelImpl();
		microschema.setName("geolocation");
		microschema.setDescription("Microschema for Geolocations");

		microschema.addField(new NumberFieldSchemaImpl().setName("longitude").setLabel("Longitude").setRequired(true));
		microschema.addField(new NumberFieldSchemaImpl().setName("latitude").setLabel("Latitude").setRequired(true));

		return microschema;
	}

	public static Node mockNode(NodeDao nodeDao, ContentDao contentDao, TagDao tagDao, Node parent, Project project, User user, String languageTag, Tag tagA, Tag tagB) {
		Node node = mock(Node.class);

		when(node.getProject()).thenReturn(project);
		when(nodeDao.getParentNode(same(node), Mockito.any())).thenReturn(parent);

		TraversalResult<Tag> tagResult = new TraversalResult<>(Arrays.asList(tagA, tagB));
		when(tagDao.getTags(same(node), Mockito.any())).thenReturn(tagResult);

		Schema schemaContainer = mockSchemaContainer("content", user);
		SchemaVersion latestVersion = schemaContainer.getLatestVersion();
		when(latestVersion.getUuid()).thenReturn(UUID_2);
		when(node.getSchemaContainer()).thenReturn(schemaContainer);
		when(node.getCreator()).thenReturn(user);
		when(node.getUuid()).thenReturn(NODE_DELOREAN_UUID);
		
		NodeFieldContainer container = mockContainer(languageTag, user);
		when(contentDao.getSchemaContainerVersion(container)).thenReturn(latestVersion);
		when(contentDao.getNode(container)).thenReturn(node);
		when(container.getNode()).thenReturn(node);
		when(container.getElementVersion()).thenReturn(UUID_5);

		when(contentDao.getLatestDraftFieldContainer(node, languageTag)).thenReturn(container);

		when(node.getElementVersion()).thenReturn(UUID_4);
		return node;
	}

	public static NodeFieldContainer mockContainer(String languageTag, User user) {
		NodeFieldContainer container = mock(NodeFieldContainer.class);
		when(container.getLanguageTag()).thenReturn(languageTag);

		when(container.getEditor()).thenReturn(user);

		// String field
		StringField stringField = mock(StringField.class);
		when(stringField.getString()).thenReturn("The name value");
		when(container.getString("string")).thenReturn(stringField);

		// Number field
		NumberField numberField = mock(NumberField.class);
		when(numberField.getNumber()).thenReturn(0.146f);
		when(container.getNumber("number")).thenReturn(numberField);

		// Date field
		DateField dateField = mock(DateField.class);
		when(dateField.getDate()).thenReturn(TIMESTAMP_NEW / 1000);
		when(container.getDate("date")).thenReturn(dateField);

		// Boolean field
		BooleanField booleanField = mock(BooleanField.class);
		when(booleanField.getBoolean()).thenReturn(true);
		when(container.getBoolean("boolean")).thenReturn(booleanField);

		// Node field
		NodeField nodeField = mock(NodeField.class);
		Node nodeRef = mockNodeBasic("folder", user);
		when(nodeField.getNode()).thenReturn(nodeRef);
		when(container.getNode("node")).thenReturn(nodeField);

		// Html field
		HtmlField htmlField = mock(HtmlField.class);
		when(htmlField.getHTML()).thenReturn("some<b>html");
		when(container.getHtml("html")).thenReturn(htmlField);

		// micronode field
		MicronodeField micronodeField = mock(MicronodeField.class);
		Micronode micronode = mockMicronode("geolocation", user);
		when(micronodeField.getMicronode()).thenReturn(micronode);
		when(container.getMicronode("micronode")).thenReturn(micronodeField);

		// Node List Field
		NodeFieldList nodeListField = mock(NodeFieldList.class);
		Mockito.<List<? extends NodeField>> when(nodeListField.getList()).thenReturn(Arrays.asList(nodeField, nodeField, nodeField));
		when(container.getNodeList("nodeList")).thenReturn(nodeListField);

		// String List Field
		StringFieldList stringListField = mock(StringFieldList.class);
		Mockito.<List<? extends StringField>> when(stringListField.getList()).thenReturn(Arrays.asList(stringField, stringField, stringField));
		when(container.getStringList("stringList")).thenReturn(stringListField);

		// Boolean List Field
		BooleanFieldList booleanListField = mock(BooleanFieldList.class);
		Mockito.<List<? extends BooleanField>> when(booleanListField.getList())
				.thenReturn(Arrays.asList(booleanField, booleanField, booleanField));
		when(container.getBooleanList("booleanList")).thenReturn(booleanListField);

		// Date List Field
		DateFieldList dateListField = mock(DateFieldList.class);
		Mockito.<List<? extends DateField>> when(dateListField.getList()).thenReturn(Arrays.asList(dateField, dateField, dateField));
		when(container.getDateList("dateList")).thenReturn(dateListField);

		// Number List Field
		NumberFieldList numberListField = mock(NumberFieldList.class);
		Mockito.<List<? extends NumberField>> when(numberListField.getList()).thenReturn(Arrays.asList(numberField, numberField, numberField));
		when(container.getNumberList("numberList")).thenReturn(numberListField);

		// Html List Field
		HtmlFieldList htmlListField = mock(HtmlFieldList.class);
		Mockito.<List<? extends HtmlField>> when(htmlListField.getList()).thenReturn(Arrays.asList(htmlField, htmlField, htmlField));
		when(container.getHTMLList("htmlList")).thenReturn(htmlListField);

		// TODO currently, this mock is only used for the search document example, where we want to omit
		// fields of type "list of micronodes". We should better add an argument to the method to specify,
		// which types of fields should be added
		// // Micronode List Field
		// HibMicronodeFieldList micronodeListField = mock(HibMicronodeFieldListImpl.class);
		// Mockito.<List<? extends HibMicronodeField>> when(micronodeListField.getList()).thenReturn(Arrays.asList(micronodeField, micronodeField,
		// micronodeField));
		// when(container.getMicronodeList("micronodeList")).thenReturn(micronodeListField);

		// TODO add select fields

		return container;
	}

	
}
