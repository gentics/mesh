package com.gentics.mesh.plugin.comment.integration.mail;

import static com.gentics.mesh.plugin.comment.TestUtil.loadResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Collections;

import javax.mail.internet.MimeMessage;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.plugin.comment.integration.AbstractLocalIntegrationTest;
import com.gentics.mesh.plugin.comment.integration.TestDataSetup;
import com.gentics.mesh.plugin.comment.model.request.CreateCommentRequest;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;

public class DateHelperTest extends AbstractLocalIntegrationTest {
	@Rule
	public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);

	@BeforeClass
	public static void createProject() throws InterruptedException {
		initMesh("datehelper");
	}

	@Test
	public void testDatehelper() throws Exception {
		createSchema();
		NodeResponse node = createNode();
		client.addComment(new CreateCommentRequest()
			.setNode(node.getUuid())
			.setLanguage("en")
			.setComment("Check out the new date formatter!")
		);

		boolean inTime = greenMail.waitForIncomingEmail(Duration.ofSeconds(10).toMillis(), 1);
		assertTrue("Mails were not received in time", inTime);
		MimeMessage message = greenMail.getReceivedMessages()[0];

		assertEquals(loadResource("/expectedMails/dateFormatter.html"), message.getContent().toString().replaceAll("\r\n", "\n"));
	}

	private void createSchema() {
		SchemaResponse schemaResponse = server.client().createSchema(new SchemaCreateRequest()
			.setName("dateSchema")
			.setFields(Collections.singletonList(new DateFieldSchemaImpl().setName("somedate")))
		).blockingGet();
		server.client().assignSchemaToProject(TestDataSetup.PROJECT_NAME, schemaResponse.getUuid()).blockingAwait();
	}

	private NodeResponse createNode() {
		return server.client().createNode(TestDataSetup.PROJECT_NAME, new NodeCreateRequest()
			.setSchemaName("dateSchema")
			.setLanguage("en")
			.setParentNodeUuid(playground.getUuid())
			.setFields(FieldMap.of("somedate", new DateFieldImpl().setDate("1990-03-21T13:37:00z")))
		).blockingGet();
	}
}
