package com.gentics.mesh.plugin.comment.integration.mail;

import static com.gentics.mesh.plugin.comment.TestUtil.loadResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.stream.Stream;

import javax.mail.Address;
import javax.mail.internet.MimeMessage;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.comment.integration.AbstractLocalIntegrationTest;
import com.gentics.mesh.plugin.comment.model.request.CreateCommentRequest;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;

import co.unruly.matchers.StreamMatchers;

public class MailServiceTest extends AbstractLocalIntegrationTest {
	@Rule
	public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);

	@BeforeClass
	public static void createProject() throws InterruptedException {
		initMesh("mailConfig");
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
}
