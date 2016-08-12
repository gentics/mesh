package com.gentics.mesh.linkrenderer;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.verticle.utility.UtilityVerticle;
import com.gentics.mesh.parameter.impl.LinkType;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.json.JsonObject;

/**
 * Test cases for link rendering using the Utility Verticle
 */
public class LinkRendererVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private UtilityVerticle utilityVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(utilityVerticle);
		return list;
	}

	/**
	 * Test rendering valid link with link type "OFF" (expects no link rendering)
	 */
	@Test
	public void testLinkReplacerTypeOff() {
		Node newsNode = content("news overview");
		testSimpleLink(newsNode, LinkType.OFF, "{{mesh.link('" + newsNode.getUuid() + "')}}");
	}

	/**
	 * Test rendering valid link with link type "SHORT" (no webroot prefix, no project prefix)
	 */
	@Test
	public void testLinkReplacerTypeShort() {
		Node newsNode = content("news overview");
		testSimpleLink(newsNode, LinkType.SHORT, "/News/News+Overview.en.html");
	}

	/**
	 * Test rendering valid link with link type "MEDIUM" (project prefix, but no webroot prefix)
	 */
	@Test
	public void testLinkReplacerTypeMedium() {
		Node newsNode = content("news overview");
		testSimpleLink(newsNode, LinkType.MEDIUM, "/dummy/News/News+Overview.en.html");
	}

	/**
	 * Test rendering valid link with link type "FULL" (webroot and project prefix)
	 */
	@Test
	public void testLinkReplacerTypeFull() {
		Node newsNode = content("news overview");
		testSimpleLink(newsNode, LinkType.FULL, "/api/v1/dummy/webroot/News/News+Overview.en.html");
	}

	/**
	 * Test rendering JSON object with links in attribute values. Links use single quotes, double quotes and no quotes
	 */
	@Test
	public void testLinkInJson() {
		Node newsNode = content("news overview");

		JsonObject jsonObject = new JsonObject().put("quotes", "prefix {{mesh.link('" + newsNode.getUuid() + "')}} postfix")
				.put("doublequotes", "prefix {{mesh.link(\"" + newsNode.getUuid() + "\")}} postfix")
				.put("noquotes", "prefix {{mesh.link(" + newsNode.getUuid() + ")}} postfix");

		JsonObject expected = new JsonObject().put("quotes", "prefix /api/v1/dummy/webroot/News/News+Overview.en.html postfix")
				.put("doublequotes", "prefix /api/v1/dummy/webroot/News/News+Overview.en.html postfix")
				.put("noquotes", "prefix /api/v1/dummy/webroot/News/News+Overview.en.html postfix");

		JsonObject resultObject = new JsonObject(renderContent(jsonObject.encode(), LinkType.FULL));

		for (String attr : Arrays.asList("quotes", "doublequotes", "noquotes")) {
			assertEquals("Check attribute '" + attr + "'", expected.getString(attr), resultObject.getString(attr));
		}
	}

	/**
	 * Test rendering invalid link (node does not exist). Expects link to be rendered as '#'
	 */
	@Test
	public void testInvalidLink() {
		testRenderContent("{{mesh.link('" + UUIDUtil.randomUUID() + "')}}", LinkType.FULL, "/api/v1/project/webroot/error/404");
	}

	/**
	 * Test rendering a simple link to the given node with given link type
	 * 
	 * @param node
	 *            node to link to
	 * @param linkType
	 *            link type
	 * @param expectedResult
	 *            expected result
	 */
	private void testSimpleLink(Node node, LinkType linkType, String expectedResult) {
		testRenderContent("{{mesh.link('" + node.getUuid() + "')}}", linkType, expectedResult);
	}

	/**
	 * Test rendering the given content
	 * 
	 * @param content
	 *            content to render
	 * @param linkType
	 *            link type
	 * @param expectedResult
	 *            expected result
	 */
	private void testRenderContent(String content, LinkType linkType, String expectedResult) {
		assertEquals("Check rendered content", expectedResult, renderContent(content, linkType));
	}

	/**
	 * Render the given content, assert success and return the result
	 * 
	 * @param content
	 *            content to render
	 * @param linkType
	 *            link type
	 * @return rendered result
	 */
	private String renderContent(String content, LinkType linkType) {
		MeshResponse<String> future = getClient().resolveLinks(content, new NodeParameters().setResolveLinks(linkType)).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}
}
