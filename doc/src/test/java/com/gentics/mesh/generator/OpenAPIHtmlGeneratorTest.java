package com.gentics.mesh.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;

/**
 * Tests for {@link OpenAPIHtmlGenerator}.
 */
public class OpenAPIHtmlGeneratorTest {

	private static final String SPEC = String.join("\n",
		"openapi: 3.0.1",
		"info:",
		"  title: Test",
		"  version: 1.0.0",
		"paths:",
		"  /api/v2/users/:",
		"    post:",
		"      description: Create a new user.",
		"      requestBody:",
		"        content:",
		"          application/json:",
		"            schema:",
		"              $ref: '#/components/schemas/UserCreateRequest'",
		"            example:",
		"              username: jdoe",
		"      responses:",
		"        default:",
		"          description: application/json",
		"        \"201\":",
		"          description: Created user.",
		"          content:",
		"            application/json:",
		"              example:",
		"                uuid: 589319933be24ec79319933be24ec7fe",
		"                groups:",
		"                - name: editors",
		"  /api/v2/users/{userUuid}:",
		"    get:",
		"      description: Read the user <b>with</b> the given uuid",
		"      parameters:",
		"      - name: userUuid",
		"        in: path",
		"        required: true",
		"        description: Uuid of the user.",
		"        schema:",
		"          type: string",
		"          example: 589319933be24ec79319933be24ec7fe",
		"      - name: perPage",
		"        in: query",
		"        description: Number of elements per page.",
		"        schema:",
		"          type: number",
		"          default: \"25\"",
		"          minimum: 1",
		"      responses:",
		"        \"200\":",
		"          description: User response.",
		"  /api/v2/{project}/webrootfield/{fieldName}/{path}:",
		"    get:",
		"      responses:",
		"        \"200\":",
		"          description: The field.",
		"  /api/v2/utilities/linkResolver:",
		"    post:",
		"      requestBody:",
		"        content:",
		"          text/plain:",
		"            example: '{{mesh.link(\"uuid\")}}'",
		"      responses:",
		"        \"200\":",
		"          description: Resolved.",
		"components:",
		"  schemas:",
		"    UserCreateRequest:",
		"      required:",
		"      - username",
		"      type: object",
		"      properties:",
		"        username:",
		"          type: string",
		"          description: Username of the user.",
		"        groupUuid:",
		"          type: string",
		"          description: Optional group uuid.");

	private static OpenAPI openApi;
	private static OpenAPIHtmlGenerator generator;

	@BeforeClass
	public static void setup() throws IOException {
		openApi = Yaml.mapper().readValue(SPEC, OpenAPI.class);
		generator = new OpenAPIHtmlGenerator(new File("target/openapi-html-test"));
	}

	private String render() throws IOException {
		Map<String, String> resources = new LinkedHashMap<>();
		resources.put("/users", "Provides endpoints which allow the manipulation of users.");
		resources.put("/{project}/webrootfield", "Provides the webrootfield endpoint.");
		return generator.render(openApi, resources);
	}

	@Test
	public void testUniqueIdFollowsRaml2HtmlScheme() {
		assertThat(OpenAPIHtmlGenerator.uniqueId("/users")).isEqualTo("users");
		assertThat(OpenAPIHtmlGenerator.uniqueId("/users/")).isEqualTo("users_");
		assertThat(OpenAPIHtmlGenerator.uniqueId("/users/{userUuid}/permissions/{path}")).isEqualTo("users__userUuid__permissions__path_");
		assertThat(OpenAPIHtmlGenerator.uniqueId("/{project}/webrootfield/{fieldName}/{path}")).isEqualTo("project__webrootfield__fieldName___path_");
	}

	@Test
	public void testResourcePanels() throws IOException {
		String html = render();
		// Top level resources in the given order, with description and sidebar entry
		assertThat(html).contains("<h3 id=\"users\" class=\"panel-title\">/users</h3>");
		assertThat(html).contains("<p>Provides endpoints which allow the manipulation of users.</p>");
		assertThat(html).contains("<li><a href=\"#users\">/users</a></li>");
		assertThat(html.indexOf("id=\"users\"")).isLessThan(html.indexOf("id=\"project__webrootfield\""));
		// Paths without a known resource are grouped by their first segment
		assertThat(html).contains("<h3 id=\"utilities\" class=\"panel-title\">/utilities</h3>");
		// Child resources and their method modals
		assertThat(html).contains("<span class=\"parent\">/users</span>/{userUuid}");
		assertThat(html).contains("<span class=\"parent\">/{project}/webrootfield</span>/{fieldName}/{path}");
		assertThat(html).contains("id=\"panel_users__userUuid_\"");
		assertThat(html).contains("<div class=\"modal fade\" tabindex=\"0\" id=\"users__userUuid__get\">");
		assertThat(html).contains("<span class=\"badge badge_post\">post</span>");
		assertThat(html).contains("id=\"project__webrootfield__fieldName___path__get\"");
	}

	@Test
	public void testMethodDetails() throws IOException {
		String html = render();
		// Descriptions are escaped
		assertThat(html).contains("<p>Read the user &lt;b&gt;with&lt;/b&gt; the given uuid</p>");
		// Parameters
		assertThat(html).contains("<h3>URI Parameters</h3>");
		assertThat(html).containsPattern("<strong>userUuid</strong>:\\s*<em>required \\(string\\)</em>\\s*<p>Uuid of the user.</p>");
		assertThat(html).contains("<pre>589319933be24ec79319933be24ec7fe</pre>");
		assertThat(html).containsPattern("<strong>perPage</strong>:\\s*<em>\\(number - default: 25 - minimum: 1\\)</em>");
		// Request body with schema table and example
		assertThat(html).contains("<tr><td>username</td><td>true</td><td>string</td><td>Username of the user.</td></tr>");
		assertThat(html).contains("<tr><td>groupUuid</td><td>false</td><td>string</td><td>Optional group uuid.</td></tr>");
		assertThat(html.indexOf("<td>groupUuid</td>")).isLessThan(html.indexOf("<td>username</td>"));
		assertThat(html).contains("&quot;username&quot; : &quot;jdoe&quot;");
		// Responses: numeric codes only, the "default" response is not rendered
		assertThat(html).contains("<a href=\"http://httpstatus.es/201\" target=\"_blank\">201</a>");
		assertThat(html).doesNotContain("httpstatus.es/default");
	}

	@Test
	public void testScriptsLoadWhenOpenedFromFileSystem() throws IOException {
		String html = render();
		// Protocol relative URLs resolve to file:// when the page is opened locally, so highlight.js would not be loaded
		assertThat(html).contains("<script src=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.2.0/highlight.min.js\"></script>");
		assertThat(html).doesNotContainPattern("src=\"//");
		assertThat(html).contains("'https://www.google-analytics.com/analytics.js'");
		assertThat(html).doesNotContain("'//www.google-analytics.com");
	}

	@Test
	public void testMissingValuesAreNotInheritedFromParentContext() throws IOException {
		String html = render();
		int start = html.indexOf("id=\"project__webrootfield__fieldName___path__get\"");
		int end = html.indexOf("<!-- Tab panes -->", start);
		// The method has no description, so the description of the resource must not show up in its modal
		assertThat(html.substring(start, end)).doesNotContain("alert-info");
	}

	@Test
	public void testExampleFiles() throws IOException {
		Map<String, String> files = generator.exampleFiles(openApi);
		assertThat(files).containsOnlyKeys(
			"response/api/v2/users//201/example.json",
			"request/api/v2/users//request-body.json",
			"request/api/v2/utilities/linkResolver/request-body.txt");
		assertThat(files.get("response/api/v2/users//201/example.json")).isEqualTo(String.join("\n",
			"{",
			"  \"uuid\" : \"589319933be24ec79319933be24ec7fe\",",
			"  \"groups\" : [ {",
			"    \"name\" : \"editors\"",
			"  } ]",
			"}"));
		assertThat(files.get("request/api/v2/utilities/linkResolver/request-body.txt")).isEqualTo("{{mesh.link(\"uuid\")}}");
	}
}
