package com.gentics.mesh.linkrenderer;

import static com.gentics.mesh.MeshVersion.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.AWSTestMode.MINIO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.S3BinaryFieldSchemaImpl;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.UUIDUtil;

@MeshTestSetting(awsContainer = MINIO, testSize = FULL, startServer = false)
public class LinkRendererTest extends AbstractMeshTest {

	private WebRootLinkReplacer replacer;

	@Before
	public void setupDeps() {
		replacer = meshDagger().webRootLinkReplacer();
	}

	@Test
	public void testLinkReplacerTypeOff() {
		try (Tx tx = tx()) {
			HibNode newsNode = content("news overview");
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
			HibNode newsNode = content("news overview");
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
			HibNode newsNode = content("news overview");
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
			HibNode newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "{{mesh.link('" + uuid + "')}}";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", CURRENT_API_BASE_PATH + "/dummy/webroot/News/News%20Overview.en.html", replacedContent);
		}
	}

	@Test
	public void testLinkAtStart() {
		try (Tx tx = tx()) {
			HibNode newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "{{mesh.link('" + uuid + "')}} postfix";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", CURRENT_API_BASE_PATH + "/dummy/webroot/News/News%20Overview.en.html postfix", replacedContent);
		}
	}

	@Test
	public void testLinkAtEnd() {
		try (Tx tx = tx()) {
			HibNode newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "prefix {{mesh.link('" + uuid + "')}}";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", "prefix " + CURRENT_API_BASE_PATH + "/dummy/webroot/News/News%20Overview.en.html", replacedContent);
		}
	}

	@Test
	public void testLinkInMiddle() {
		try (Tx tx = tx()) {
			HibNode newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "prefix {{mesh.link('" + uuid + "')}} postfix";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", "prefix " + CURRENT_API_BASE_PATH + "/dummy/webroot/News/News%20Overview.en.html postfix", replacedContent);
		}
	}

	@Test
	public void testAdjacentLinks() {
		try (Tx tx = tx()) {
			HibNode newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "{{mesh.link('" + uuid + "')}}{{mesh.link('" + uuid + "')}}";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content",
				CURRENT_API_BASE_PATH + "/dummy/webroot/News/News%20Overview.en.html" + CURRENT_API_BASE_PATH + "/dummy/webroot/News/News%20Overview.en.html", replacedContent);
		}
	}

	@Test
	public void testNonAdjacentLinks() {
		try (Tx tx = tx()) {
			HibNode newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "{{mesh.link('" + uuid + "')}} in between {{mesh.link('" + uuid + "')}}";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content",
				CURRENT_API_BASE_PATH + "/dummy/webroot/News/News%20Overview.en.html in between " + CURRENT_API_BASE_PATH + "/dummy/webroot/News/News%20Overview.en.html",
					replacedContent);
		}
	}

	@Test
	public void testInvalidLinks() {
		try (Tx tx = tx()) {
			HibNode newsNode = content("news overview");
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
			HibNode newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "'\"{{mesh.link(" + uuid + ")}}\"'";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", "'\"" + CURRENT_API_BASE_PATH + "/dummy/webroot/News/News%20Overview.en.html\"'", replacedContent);
		}
	}

	@Test
	public void testNoQuoteGerman() {
		try (Tx tx = tx()) {
			HibNode newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "'\"{{mesh.link(" + uuid + ", de)}}\"'";

			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", "'\"" + CURRENT_API_BASE_PATH + "/dummy/webroot/Neuigkeiten/News%20Overview.de.html\"'", replacedContent);
		}
	}

	@Test
	public void testSingleQuote() {
		try (Tx tx = tx()) {
			HibNode newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "'\"{{mesh.link('" + uuid + "')}}\"'";

			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", "'\"" + CURRENT_API_BASE_PATH + "/dummy/webroot/News/News%20Overview.en.html\"'", replacedContent);
		}
	}

	@Test
	public void testDoubleQuote() {
		try (Tx tx = tx()) {
			HibNode newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "'\"{{mesh.link(\"" + uuid + "\")}}\"'";

			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", "'\"" + CURRENT_API_BASE_PATH + "/dummy/webroot/News/News%20Overview.en.html\"'", replacedContent);
		}
	}

	@Test
	public void testGerman() {
		try (Tx tx = tx()) {
			HibNode newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "{{mesh.link(\"" + uuid + "\", \"de\")}}";

			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", CURRENT_API_BASE_PATH + "/dummy/webroot/Neuigkeiten/News%20Overview.de.html", replacedContent);
		}
	}

	@Test
	public void testEnglish() {
		try (Tx tx = tx()) {
			HibNode newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "{{mesh.link(\"" + uuid + "\", \"en\")}}";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, content, LinkType.FULL, null,
					null);

			assertEquals("Check rendered content", CURRENT_API_BASE_PATH + "/dummy/webroot/News/News%20Overview.en.html", replacedContent);
		}
	}

	@Test
	public void testNodeReplace() throws IOException, InterruptedException, ExecutionException {
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			String german = german();
			String english = english();
			HibNode parentNode = folder("2015");

			HibSchemaVersion schemaVersion = schemaContainer("content").getLatestVersion();
			schemaVersion.getSchema()
					.addField(new StringFieldSchemaImpl().setName("displayName"))
					.addField(new StringFieldSchemaImpl().setName("name"));
			actions().updateSchemaVersion(schemaVersion);
			// Create some dummy content
			HibNode content = nodeDao.create(parentNode, user(), schemaVersion, project());
			HibNodeFieldContainer germanContainer = tx.contentDao().createFieldContainer(content, german, content.getProject().getLatestBranch(), user());
			germanContainer.createString("displayName").setString("german name");
			germanContainer.createString("name").setString("german.html");

			HibNode content2 = nodeDao.create(parentNode, user(), schemaContainer("content").getLatestVersion(), project());
			HibNodeFieldContainer englishContainer = tx.contentDao().createFieldContainer(content2, english, content2.getProject().getLatestBranch(), user());
			englishContainer.createString("displayName").setString("content 2 english");
			englishContainer.createString("name").setString("english.html");

			InternalActionContext ac = mockActionContext();
			replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, "dgasd", null, null, null);
		}
	}

	@Test
	public void testBinaryFieldLinkResolving() {
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNode node = content("news overview");
			String uuid = node.getUuid();

			// Transform the node into a node with a binary field.
			String fileName = "somefile.dat";
			SchemaVersionModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(new BinaryFieldSchemaImpl().setName("binary").setLabel("Binary content"));
			schema.setSegmentField("binary");
			node.getSchemaContainer().getLatestVersion().setSchema(schema);
			actions().updateSchemaVersion(node.getSchemaContainer().getLatestVersion());
			HibBinary binary = tx.binaries().create("bogus", 1L).runInExistingTx(tx);
			contentDao.getLatestDraftFieldContainer(node, english()).createBinary("binary", binary).setFileName(fileName);

			// Render the link
			final String meshLink = "{{mesh.link(\"" + uuid + "\", \"en\")}}";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, meshLink, LinkType.FULL, null,
					null);
			assertEquals("Check rendered content", CURRENT_API_BASE_PATH + "/dummy/webroot/News/somefile.dat", replacedContent);
		}
	}

	@Test
	public void testS3BinaryFieldLinkResolving() {
		String fileName = "somefile.jpg";
		String s3Bucket = getTestContext().getOptions().getS3Options().getBucket();

		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNode node = content("news overview");
			String uuid = node.getUuid();

			// Transform the node into a node with a S3 binary field.
			SchemaVersionModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(new S3BinaryFieldSchemaImpl().setName("s3binary").setLabel("Binary content"));
			schema.setSegmentField("s3binary");
			node.getSchemaContainer().getLatestVersion().setSchema(schema);
			actions().updateSchemaVersion(node.getSchemaContainer().getLatestVersion());

			S3HibBinary binary = tx.s3binaries().create(UUIDUtil.randomUUID(), s3Bucket, fileName).runInExistingTx(tx);
			HibNodeFieldContainer original = contentDao.getLatestDraftFieldContainer(node, english());
			HibNodeFieldContainer newBinary = contentDao.createFieldContainer(node, english(), project().getLatestBranch(), user(), original, true);
			newBinary.createS3Binary("s3binary", binary).setFileName(fileName);

			// uploading
			File tempFile = createTempFile();
			s3BinaryStorage().createBucket(s3Bucket)
					.flatMap(unused -> s3BinaryStorage().uploadFile(s3Bucket, uuid + "/s3binary/" + english(), tempFile, false))
					.blockingGet();

			// Render the link
			final String meshLink = "{{mesh.link(\"" + uuid + "\", \"en\")}}";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, meshLink, LinkType.FULL, null,
					null);
			assertTrue(replacedContent.matches("https?:\\/\\/[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)\\/" + s3Bucket + "/" + s3Bucket + "\\?X-Amz-Algorithm=[-A-Z0-9]{1,256}&X-Amz-Date=[0-9]{8}T[0-9]{6}Z&X-Amz-SignedHeaders=host&X-Amz-Expires=[0-9]{1,256}&X-Amz-Credential=accessKey%2F[0-9]{8}%2Feu-central-1%2Fs3%2Faws4_request&X-Amz-Signature=[a-z0-9]{1,256}"));
		}
	}

	@Test
	public void testResolving() throws InterruptedException, ExecutionException {
		try (Tx tx = tx()) {
			HibNode newsNode = content("news overview");
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
