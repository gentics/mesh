package com.gentics.mesh.plugin.comment.integration.mail;

import static com.gentics.mesh.plugin.comment.TestCommentClient.User.ANONYMOUS;
import static com.gentics.mesh.plugin.comment.TestUtil.loadResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.time.Duration;
import java.util.stream.Stream;

import javax.mail.Address;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.parameter.impl.DeleteParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.plugin.comment.TestClient;
import com.gentics.mesh.plugin.comment.TestCommentClient;
import com.gentics.mesh.plugin.comment.integration.TestDataSetup;
import com.gentics.mesh.plugin.comment.model.request.CreateCommentRequest;
import com.gentics.mesh.plugin.test.common.AbstractPluginContainerIntegrationTest;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;

import co.unruly.matchers.StreamMatchers;
import io.reactivex.Observable;

public class MailServiceIT extends AbstractPluginContainerIntegrationTest {

	public static final String API_NAME = "comment";
	private static TestClient testClient;
	private static TestCommentClient client;
	private static TestDataSetup.TestData testData;

	@Rule
	public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);

	private NodeResponse playground;

	@BeforeClass
	public static void beforeClass() throws Exception {
		testClient = new TestClient(meshServer.client());
		testData = new TestDataSetup(meshServer.client()).setup();
		client = new TestCommentClient(meshServer.getHost(), meshServer.getPort(), testData);
		copyPlugin("comment", pluginDir -> {

			// 1. Create Plugin Config
			File favConfSrc = new File(MailServiceIT.class.getResource("/mailConfigDocker.yml").toURI());
			File favConfDest = new File(pluginDir, "config.yml");
			FileUtils.copyFile(favConfSrc, favConfDest);

			// 2. Create templates
			File templatesDir = new File(pluginDir, "storage/templates");
			templatesDir.mkdirs();
			FileUtils.copyInputStreamToFile(
				MailServiceIT.class.getResourceAsStream("/storage/templates/list.hbs"),
				new File(templatesDir, "list.hbs")
			);

			File mailTemplatesDir = new File(pluginDir, "storage/plugins/comment/mailTemplates");
			mailTemplatesDir.mkdirs();
			FileUtils.copyInputStreamToFile(
				MailServiceIT.class.getResourceAsStream("/storage/plugins/comment/mailTemplates/commentCreated.hbs"),
				new File(mailTemplatesDir, "commentCreated.hbs")
			);
		});
	}

	@Before
	public void setUp() {
		client.setUser(ANONYMOUS);
		playground = testClient.createFolder(testData.project, "playground");
	}

	@After
	public void cleanUp() {
		// Delete all comments and sections
		meshServer.client().findNodeChildren(testData.project.getName(), testData.commentFolder.getUuid(),
			new PagingParametersImpl().setPerPage(999L)).toSingle()
			.flatMapObservable(nodes -> Observable.fromIterable(nodes.getData()))
			.flatMapCompletable(node -> meshServer.client().deleteNode(
				testData.project.getName(), node.getUuid(),
				new DeleteParametersImpl().setRecursive(true)).toCompletable())
			.blockingAwait();

		meshServer.client().deleteNode(
			testData.project.getName(),
			playground.getUuid(),
			new DeleteParametersImpl().setRecursive(true)).blockingAwait();
	}


	@Test
	public void testNotifyCommentCreated() throws Exception {
		client.addComment("This is a test comment!");
		client.addComment("comment2");

		boolean inTime = greenMail.waitForIncomingEmail(Duration.ofSeconds(10).toMillis(), 4);
		assertTrue("Mails were not received in time", inTime);

		MimeMessage[] messages = greenMail.getReceivedMessages();
		assertEquals(4, messages.length);
		MimeMessage message = messages[0];
		Stream<String> recipients = Stream.of(message.getAllRecipients())
			.map(Address::toString);

		assertEquals("A test comment has been created", message.getSubject());
		assertEquals("Testmailer <test@mailer.com>", message.getFrom()[0].toString());
		assertThat(recipients, StreamMatchers.contains("moderator@example.com", "admin@example.com"));
		// Something converts LF to CRLF when receiving mails. We convert them back here
		assertEquals(loadResource("/expectedMails/commentCreated1.html"), message.getContent().toString().replaceAll("\r\n", "\n"));
	}

	@Test
	public void testLinkRendering() throws Exception {
		NodeResponse folder = testClient.createFolder(playground, String.format("{{mesh.link(\"%s\", \"en\")}}", playground.getUuid()));
		client.addComment(new CreateCommentRequest()
			.setComment("This is a link rendering test!")
			.setNode(folder.getUuid())
			.setLanguage("en")
		);

		boolean inTime = greenMail.waitForIncomingEmail(Duration.ofSeconds(10).toMillis(), 2);
		assertTrue("Mails were not received in time", inTime);

		MimeMessage[] messages = greenMail.getReceivedMessages();
		assertEquals(2, messages.length);
		MimeMessage message = messages[0];
		Stream<String> recipients = Stream.of(message.getAllRecipients())
			.map(Address::toString);

		assertEquals("A test comment has been created", message.getSubject());
		assertEquals("Testmailer <test@mailer.com>", message.getFrom()[0].toString());
		assertThat(recipients, StreamMatchers.contains("moderator@example.com", "admin@example.com"));
		// Something converts LF to CRLF when receiving mails. We convert them back here
		assertEquals(loadResource("/expectedMails/linkRendering.html"), message.getContent().toString().replaceAll("\r\n", "\n"));
	}

	protected MeshRestClient meshClient() {
		return meshServer.client();
	}

	protected String serverURL() {
		return "http://" + meshServer.getHost() + ":" + meshServer.getPort();
	}
}
