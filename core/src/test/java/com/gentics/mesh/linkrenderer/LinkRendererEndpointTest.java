package com.gentics.mesh.linkrenderer;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.tx.Tx;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.Arrays;

import static com.gentics.mesh.handler.VersionHandler.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

/**
 * Test cases for link rendering using the Utility Verticle
 */
@MeshTestSetting(testSize = FULL, startServer = true)
public class LinkRendererEndpointTest extends AbstractMeshTest {

	/**
	 * Test rendering valid link with link type "OFF" (expects no link rendering)
	 */
	@Test
	public void testLinkReplacerTypeOff() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			testSimpleLink(newsNode, LinkType.OFF, "{{mesh.link('" + newsNode.getUuid() + "')}}");
		}
	}

	/**
	 * Test rendering valid link with link type "SHORT" (no webroot prefix, no project prefix)
	 */
	@Test
	public void testLinkReplacerTypeShort() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			testSimpleLink(newsNode, LinkType.SHORT, "/News/News%20Overview.en.html");
		}
	}

	/**
	 * Test rendering valid link with link type "MEDIUM" (project prefix, but no webroot prefix)
	 */
	@Test
	public void testLinkReplacerTypeMedium() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			testSimpleLink(newsNode, LinkType.MEDIUM, "/dummy/News/News%20Overview.en.html");
		}
	}

	/**
	 * Test rendering valid link with link type "FULL" (webroot and project prefix)
	 */
	@Test
	public void testLinkReplacerTypeFull() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			testSimpleLink(newsNode, LinkType.FULL, CURRENT_API_BASE_PATH + "/dummy/webroot/News/News%20Overview.en.html");
		}
	}

	/**
	 * Test rendering JSON object with links in attribute values. Links use single quotes, double quotes and no quotes
	 */
	@Test
	public void testLinkInJson() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");

			JsonObject jsonObject = new JsonObject().put("quotes", "prefix {{mesh.link('" + newsNode.getUuid() + "')}} postfix")
					.put("doublequotes", "prefix {{mesh.link(\"" + newsNode.getUuid() + "\")}} postfix")
					.put("noquotes", "prefix {{mesh.link(" + newsNode.getUuid() + ")}} postfix");

			JsonObject expected = new JsonObject().put("quotes", "prefix " + CURRENT_API_BASE_PATH + "/dummy/webroot/News/News%20Overview.en.html postfix")
					.put("doublequotes", "prefix " + CURRENT_API_BASE_PATH + "/dummy/webroot/News/News%20Overview.en.html postfix")
					.put("noquotes", "prefix " + CURRENT_API_BASE_PATH + "/dummy/webroot/News/News%20Overview.en.html postfix");

			JsonObject resultObject = new JsonObject(renderContent(jsonObject.encode(), LinkType.FULL));

			for (String attr : Arrays.asList("quotes", "doublequotes", "noquotes")) {
				assertEquals("Check attribute '" + attr + "'", expected.getString(attr), resultObject.getString(attr));
			}
		}
	}

	/**
	 * Test rendering invalid link (node does not exist). Expects link to be rendered as '#'
	 */
	@Test
	public void testInvalidLink() {
		try (Tx tx = tx()) {
			testRenderContent("{{mesh.link('" + UUIDUtil.randomUUID() + "')}}", LinkType.FULL, CURRENT_API_BASE_PATH + "/project/webroot/error/404");
		}
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
		try (Tx tx = tx()) {
			testRenderContent("{{mesh.link('" + node.getUuid() + "')}}", linkType, expectedResult);
		}
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
		return call(() -> client().resolveLinks(content, new NodeParametersImpl().setResolveLinks(linkType)));
	}
}
