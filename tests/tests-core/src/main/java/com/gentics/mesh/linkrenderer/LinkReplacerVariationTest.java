package com.gentics.mesh.linkrenderer;

import static com.gentics.mesh.FieldUtil.createStringField;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Test cases for the {@link WebRootLinkReplacer}
 */
@MeshTestSetting(testSize = PROJECT, startServer = true)
@RunWith(Parameterized.class)
public class LinkReplacerVariationTest extends AbstractMeshTest {
	private static final String PROJECT_NAME = "Testproject";

	/**
	 * Get variation data
	 * @return variation data
	 */
	@Parameters(name = "{index}: folder in en: {0}, folder in de: {1}, content in en {2}, content in de {3}, language {4}, link language {5}, link type {6}")
	public static Collection<Object[]> paramData() {
		Collection<Object[]> data = new ArrayList<>();

		for (boolean folderEn : Arrays.asList(true, false)) {
			for (boolean folderDe : Arrays.asList(true, false)) {
				if (!folderEn && !folderDe) {
					// folder must exist in at least one language
					continue;
				}
				for (boolean contentEn : Arrays.asList(true, false)) {
					for (boolean contentDe : Arrays.asList(true, false)) {
						if (!contentEn && !contentDe) {
							// content must exist in at least one language
							continue;
						}
						for (String language : Arrays.asList("en", "de")) {
							for (String linkLanguage : Arrays.asList("en", "de", null)) {
								for (LinkType linkType : Arrays.asList(LinkType.SHORT, LinkType.MEDIUM, LinkType.FULL)) {
									data.add(new Object[] { folderEn, folderDe, contentEn, contentDe, language,
											linkLanguage, linkType });
								}
							}
						}
					}
				}
			}
		}

		return data;
	}

	/**
	 * Whether the folder exists in english
	 */
	@Parameter(0)
	public boolean folderEn;

	/**
	 * Whether the folder exists in german
	 */
	@Parameter(1)
	public boolean folderDe;

	/**
	 * Whether the content exists in english
	 */
	@Parameter(2)
	public boolean contentEn;

	/**
	 * Whether the content exists in german
	 */
	@Parameter(3)
	public boolean contentDe;

	/**
	 * Language, for which the link shall be rendered
	 */
	@Parameter(4)
	public String language;

	/**
	 * Language in the link (null to have a link without language tag)
	 */
	@Parameter(5)
	public String linkLanguage;

	/**
	 * Link type
	 */
	@Parameter(6)
	public LinkType linkType;

	private WebRootLinkReplacer replacer;

	private String projectUuid;

	private String branchUuid;

	private String folderUuid;

	private String contentUuid;

	private String otherFolderUuid;

	private String otherContentUuid;

	private String expected;

	private String expectedForEn;

	private String expectedForDe;

	/**
	 * Setup the data for the test case
	 */
	@Before
	public void setup() {
		replacer = meshDagger().webRootLinkReplacer();

		// Create test project
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(PROJECT_NAME);
		request.setHostname("dummy.io");
		request.setSsl(false);
		request.setSchema(new SchemaReferenceImpl().setName("folder"));
		ProjectResponse projectResponse = call(() -> client().createProject(request));
		projectUuid = projectResponse.getUuid();
		branchUuid = call(() -> client().findBranches(PROJECT_NAME)).getData().stream().filter(BranchResponse::getLatest).findFirst().orElseThrow().getUuid();

		// Assign the content schema
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schemaUuid));

		// create the link target
		if (folderEn) {
			folderUuid = createFolder(projectResponse.getRootNode().getUuid(), folderUuid, "targetfolder", "en");
		}
		if (folderDe) {
			folderUuid = createFolder(projectResponse.getRootNode().getUuid(), folderUuid, "zielordner", "de");
		}
		if (contentEn) {
			contentUuid = createContent(folderUuid, contentUuid, "targetcontent", "en");
		}
		if (contentDe) {
			contentUuid = createContent(folderUuid, contentUuid, "zielinhalt", "de");
		}

		// create another link target (in english and german)
		otherFolderUuid = createFolder(projectResponse.getRootNode().getUuid(), otherFolderUuid, "otherfolder", "en");
		otherFolderUuid = createFolder(projectResponse.getRootNode().getUuid(), otherFolderUuid, "andererordner", "de");
		otherContentUuid = createContent(otherFolderUuid, otherContentUuid, "othercontent", "en");
		otherContentUuid = createContent(otherFolderUuid, otherContentUuid, "andererinhalt", "de");

		String expectedLanguage = linkLanguage;
		if (StringUtils.isBlank(expectedLanguage)) {
			expectedLanguage = language;
		}
		expected = fixForLinkType(getExpectedForLanguage(expectedLanguage));
		expectedForEn = fixForLinkType(getExpectedForLanguage("en"));
		expectedForDe = fixForLinkType(getExpectedForLanguage("de"));
	}

	/**
	 * Test {@link WebRootLinkReplacer#replace(InternalActionContext, String, ContainerType, String, LinkType, String, java.util.List)}
	 */
	@Test
	public void testReplace() {
		try (Tx tx = tx()) {
			final String content = String.format("{{mesh.link('%s'%s)}}", contentUuid,
					linkLanguage != null ? ", '" + linkLanguage + "'" : "");
			HibProject project = tx.projectDao().findByUuid(projectUuid);
			InternalActionContext ac = getMockedInternalActionContext("", user(), project);
			ac.setParameter(VersioningParameters.BRANCH_QUERY_PARAM_KEY, PROJECT_NAME);
			String replacedContent = replacer.replace(ac, branchUuid, ContainerType.DRAFT, content, linkType, PROJECT_NAME, Arrays.asList(language));
			assertThat(replacedContent).as("Rendered link").isEqualTo(expected);
			tx.success();
		}
	}

	/**
	 * Test {@link WebRootLinkReplacer#replaceMany(InternalActionContext, String, ContainerType, java.util.Set, LinkType, String, String...)}
	 */
	@Test
	public void testReplaceMany() {
		try (Tx tx = tx()) {
			// test the link to the target page
			String link = String.format("{{mesh.link('%s'%s)}}", contentUuid,
					linkLanguage != null ? ", '" + linkLanguage + "'" : "");

			// test the link to the target page in german
			String linkDe = String.format("{{mesh.link('%s', 'de')}}", contentUuid);

			// test the link to the target page in english
			String linkEn = String.format("{{mesh.link('%s', 'en')}}", contentUuid);

			// test a link to an inexistent content
			String notFoundLink = String.format("{{mesh.link('%s')}}", UUIDUtil.randomUUID());
			String expectedNotFound = fixForLinkType("/error/404");

			// test a composed content (containing all the links from above)
			String composed = String.format("link1: %s, link2: %s, link3: %s, link4: %s", link, notFoundLink, linkDe, linkEn);
			String expectedForComposed = String.format("link1: %s, link2: %s, link3: %s, link4: %s", expected, expectedNotFound, expectedForDe, expectedForEn);

			HibProject project = tx.projectDao().findByUuid(projectUuid);
			InternalActionContext ac = getMockedInternalActionContext("", user(), project);
			ac.setParameter(VersioningParameters.BRANCH_QUERY_PARAM_KEY, PROJECT_NAME);

			Map<String, String> replacedContents = replacer.replaceMany(ac, branchUuid, ContainerType.DRAFT,
					SetUtils.hashSet(link, notFoundLink, linkDe, linkEn, composed), linkType, PROJECT_NAME, language);
			assertThat(replacedContents).as("Rendered link").containsOnly(
					entry(link, expected),
					entry(notFoundLink, expectedNotFound),
					entry(linkDe, expectedForDe),
					entry(linkEn, expectedForEn),
					entry(composed, expectedForComposed));
			tx.success();
		}
	}

	/**
	 * Create a folder
	 * @param parentUuid parent UUID
	 * @param uuid optional folder UUID
	 * @param slug slug
	 * @param language language
	 * @return folder UUID
	 */
	protected String createFolder(String parentUuid, String uuid, String slug, String language) {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setParentNodeUuid(parentUuid);
		nodeCreateRequest.setLanguage(language);
		nodeCreateRequest.setSchemaName("folder");
		nodeCreateRequest.getFields().put("slug", createStringField(slug));
		if (uuid != null) {
			return call(() -> client().createNode(uuid, PROJECT_NAME, nodeCreateRequest)).getUuid();
		} else {
			return call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest)).getUuid();
		}
	}

	/**
	 * Create a content
	 * @param parentUuid parent UUID
	 * @param uuid optional content UUID
	 * @param slug slug
	 * @param language language
	 * @return content UUID
	 */
	protected String createContent(String parentUuid, String uuid, String slug, String language) {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setParentNodeUuid(parentUuid);
		nodeCreateRequest.setLanguage(language);
		nodeCreateRequest.setSchemaName("content");
		nodeCreateRequest.getFields().put("slug", createStringField(slug));
		nodeCreateRequest.getFields().put("teaser", createStringField("teaser"));
		nodeCreateRequest.getFields().put("content", createStringField("content"));
		if (uuid != null) {
			return call(() -> client().createNode(uuid, PROJECT_NAME, nodeCreateRequest)).getUuid();
		} else {
			return call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest)).getUuid();
		}
	}

	/**
	 * Get the expected link for the given language
	 * @param language language (must be either "en" or "de")
	 * @return expected link
	 */
	protected String getExpectedForLanguage(String language) {
		switch (language) {
		case "en":
			if (contentEn) {
				if (folderEn) {
					return "/targetfolder/targetcontent";
				} else {
					return "/zielordner/targetcontent";
				}
			} else {
				return "/error/404";
			}
		case "de":
			if (contentDe) {
				if (folderDe) {
					return "/zielordner/zielinhalt";
				} else {
					return "/targetfolder/zielinhalt";
				}
			} else {
				if (folderDe) {
					return "/zielordner/targetcontent";
				} else {
					return "/targetfolder/targetcontent";
				}
			}
		default:
			fail("Unexpected language " + language);
			return null;
		}
	}

	/**
	 * Fix the given link for the link type
	 * @param link original link
	 * @return fixed link
	 */
	protected String fixForLinkType(String link) {
		switch (linkType) {
		case MEDIUM:
			return String.format("/%s%s", PROJECT_NAME, link);
		case FULL:
			return String.format("/api/v2/%s/webroot%s", PROJECT_NAME, link);
		default:
			return link;
		}
	}
}
