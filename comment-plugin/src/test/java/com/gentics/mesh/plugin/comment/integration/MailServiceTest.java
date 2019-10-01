package com.gentics.mesh.plugin.comment.integration;

import co.unruly.matchers.StreamMatchers;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.Address;
import javax.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.stream.Stream;

import static com.gentics.mesh.plugin.comment.TestUtil.loadResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MailServiceTest extends AbstractLocalIntegrationTest {
	@Rule
	public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);

	@BeforeClass
	public static void createProject() {
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
}
