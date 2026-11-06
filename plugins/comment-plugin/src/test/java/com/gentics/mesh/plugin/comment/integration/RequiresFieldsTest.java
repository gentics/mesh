package com.gentics.mesh.plugin.comment.integration;

import com.gentics.mesh.plugin.comment.model.NestedComment;
import com.gentics.mesh.plugin.comment.model.NestedCommentSection;
import com.gentics.mesh.plugin.comment.model.request.CreateCommentRequest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RequiresFieldsTest extends AbstractLocalIntegrationTest {
	@BeforeClass
	public static void createProject() throws Exception {
		initMesh("requiresFields");
	}

	@Test
	public void testNoFields() {
		assertStatusCode(400, client.addCommentResponse(new CreateCommentRequest()
			.setComment("comment")
		));
		NestedCommentSection comments = commentsOfNodeResponse(client.getCommentsOfNode());
		assertEquals(0, comments.getChildren().size());
	}

	@Test
	public void testSomeRequiredFields() {
		assertStatusCode(400, client.addCommentResponse(new CreateCommentRequest()
			.setComment("comment")
			.setAuthor("some author")
		));
		NestedCommentSection comments = commentsOfNodeResponse(client.getCommentsOfNode());
		assertEquals(0, comments.getChildren().size());
	}

	@Test
	public void testAllRequiredFields() {
		assertStatusCode(200, client.addCommentResponse(new CreateCommentRequest()
			.setComment("comment")
			.setAuthor("some author")
			.setSubject("some subject")
		));
		NestedCommentSection comments = commentsOfNodeResponse(client.getCommentsOfNode());
		NestedComment comment = comments.getChildren().get(0);

		assertEquals("comment", comment.getFields().getComment());
		assertEquals("some author", comment.getFields().getAuthor());
		assertEquals("some subject", comment.getFields().getSubject());
	}
}
