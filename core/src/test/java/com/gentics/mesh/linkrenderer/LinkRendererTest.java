package com.gentics.mesh.linkrenderer;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class LinkRendererTest extends AbstractMeshTest {

	private WebRootLinkReplacer replacer;

	@Before
	public void setupDeps() {
		replacer = meshDagger().webRootLinkReplacer();
	}

	@Test
	public void testLinkReplacerTypeOff() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "{{mesh.link('" + uuid + "')}}";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.OFF, null,
					null);

			assertEquals("Check rendered content", content, replacedContent);
		}
	}

	@Test
	public void testLinkReplacerTypeShort() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "{{mesh.link('" + uuid + "')}}";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.SHORT, null,
					null);

			assertEquals("Check rendered content", "/News/News%20Overview.en.html", replacedContent);
		}
	}

	@Test
	public void testLinkReplacerTypeMedium() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "{{mesh.link('" + uuid + "')}}";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.MEDIUM, null,
					null);

			assertEquals("Check rendered content", "/dummy/News/News%20Overview.en.html", replacedContent);
		}
	}

	@Test
	public void testLinkReplacerTypeFull() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "{{mesh.link('" + uuid + "')}}";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", "/api/v1/dummy/webroot/News/News%20Overview.en.html", replacedContent);
		}
	}

	@Test
	public void testLinkAtStart() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "{{mesh.link('" + uuid + "')}} postfix";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", "/api/v1/dummy/webroot/News/News%20Overview.en.html postfix", replacedContent);
		}
	}

	@Test
	public void testLinkAtEnd() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "prefix {{mesh.link('" + uuid + "')}}";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", "prefix /api/v1/dummy/webroot/News/News%20Overview.en.html", replacedContent);
		}
	}

	@Test
	public void testLinkInMiddle() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "prefix {{mesh.link('" + uuid + "')}} postfix";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", "prefix /api/v1/dummy/webroot/News/News%20Overview.en.html postfix", replacedContent);
		}
	}

	@Test
	public void testAdjacentLinks() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "{{mesh.link('" + uuid + "')}}{{mesh.link('" + uuid + "')}}";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content",
					"/api/v1/dummy/webroot/News/News%20Overview.en.html/api/v1/dummy/webroot/News/News%20Overview.en.html", replacedContent);
		}
	}

	@Test
	public void testNonAdjacentLinks() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "{{mesh.link('" + uuid + "')}} in between {{mesh.link('" + uuid + "')}}";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content",
					"/api/v1/dummy/webroot/News/News%20Overview.en.html in between /api/v1/dummy/webroot/News/News%20Overview.en.html",
					replacedContent);
		}
	}

	@Test
	public void testInvalidLinks() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "{{mesh.link('" + uuid + "')}";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", content, replacedContent);
		}
	}

	@Test
	public void testNoQuote() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "'\"{{mesh.link(" + uuid + ")}}\"'";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", "'\"/api/v1/dummy/webroot/News/News%20Overview.en.html\"'", replacedContent);
		}
	}

	@Test
	public void testNoQuoteGerman() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "'\"{{mesh.link(" + uuid + ", de)}}\"'";

			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", "'\"/api/v1/dummy/webroot/Neuigkeiten/News%20Overview.de.html\"'", replacedContent);
		}
	}

	@Test
	public void testSingleQuote() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "'\"{{mesh.link('" + uuid + "')}}\"'";

			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", "'\"/api/v1/dummy/webroot/News/News%20Overview.en.html\"'", replacedContent);
		}
	}

	@Test
	public void testDoubleQuote() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "'\"{{mesh.link(\"" + uuid + "\")}}\"'";

			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", "'\"/api/v1/dummy/webroot/News/News%20Overview.en.html\"'", replacedContent);
		}
	}

	@Test
	public void testGerman() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "{{mesh.link(\"" + uuid + "\", \"de\")}}";

			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", "/api/v1/dummy/webroot/Neuigkeiten/News%20Overview.de.html", replacedContent);
		}
	}

	@Test
	public void testEnglish() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "{{mesh.link(\"" + uuid + "\", \"en\")}}";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", "/api/v1/dummy/webroot/News/News%20Overview.en.html", replacedContent);
		}
	}

	@Test
	public void testNodeReplace() throws IOException, InterruptedException, ExecutionException {
		try (Tx tx = tx()) {
			String german = german();
			String english = english();
			Node parentNode = folder("2015");

			SchemaContainerVersion schemaVersion = schemaContainer("content").getLatestVersion();
			// Create some dummy content
			Node content = parentNode.create(user(), schemaVersion, project());
			NodeGraphFieldContainer germanContainer = content.createGraphFieldContainer(german, content.getProject().getLatestBranch(), user());
			germanContainer.createString("displayName").setString("german name");
			germanContainer.createString("name").setString("german.html");

			Node content2 = parentNode.create(user(), schemaContainer("content").getLatestVersion(), project());
			NodeGraphFieldContainer englishContainer = content2.createGraphFieldContainer(english, content2.getProject().getLatestBranch(), user());
			englishContainer.createString("displayName").setString("content 2 english");
			englishContainer.createString("name").setString("english.html");

			InternalActionContext ac = mockActionContext();
			replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, "dgasd", null, null, null);
		}
	}

	@Test
	public void testBinaryFieldLinkResolving() {
		try (Tx tx = tx()) {
			Node node = content("news overview");
			String uuid = node.getUuid();

			// Transform the node into a node with a binary field.
			String fileName = "somefile.dat";
			SchemaModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(new BinaryFieldSchemaImpl().setName("binary").setLabel("Binary content"));
			schema.setSegmentField("binary");
			node.getSchemaContainer().getLatestVersion().setSchema(schema);
			Binary binary = MeshInternal.get().boot().binaryRoot().create("bogus", 1L);
			node.getLatestDraftFieldContainer(english()).createBinary("binary", binary).setFileName(fileName);

			// Render the link
			final String meshLink = "{{mesh.link(\"" + uuid + "\", \"en\")}}";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, meshLink, LinkType.FULL, null,
					null);
			assertEquals("Check rendered content", "/api/v1/dummy/webroot/News/somefile.dat", replacedContent);
		}
	}

	@Test
	public void testResolving() throws InterruptedException, ExecutionException {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "some bla START<a href=\"{{mesh.link('" + uuid + "','en')}}\">Test</a>   dasasdg <a href=\"{{mesh.link(\"" + uuid
					+ "\")}}\">Test</a>DEN";
			System.out.println("From: " + content);
			InternalActionContext ac = mockActionContext();
			String output = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, null, null, null);
			System.out.println("To:   " + output);
		}
	}

	@Test
	public void testRendering() throws IOException {
		try (Tx tx = tx()) {
			String uuid = UUIDUtil.randomUUID();
			final String content = "some bla START<a href=\"{{mesh.link(\"" + uuid + "\")}}\">Test</a>   dasasdg <a href=\"{{mesh.link(\"" + uuid
					+ "\")}}\">Test</a>DEN";
			System.out.println(content);
			int start = content.indexOf("{{mesh.link(");
			int stop = content.lastIndexOf(")}}") + 3;
			int len = stop - start;
			System.out.println("from " + start + " to " + stop + " len " + len);
			InputStream in = IOUtils.toInputStream(content);
			try {
				int e = 0;
				for (int c; (c = in.read()) != -1 /* EOF */;) {
					if (e >= start && e < stop) {
						// System.out.println("skipping");
						in.skip(len);
						System.out.print("Hugsdibugsdi");
						e = stop;
					} else {
						System.out.print((char) c);
					}
					e++;
				}
			} finally {
				in.close();
			}
		}
	}

}
