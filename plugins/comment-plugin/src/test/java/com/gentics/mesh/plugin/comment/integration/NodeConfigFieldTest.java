package com.gentics.mesh.plugin.comment.integration;

import com.gentics.mesh.plugin.comment.model.Comment;
import com.gentics.mesh.plugin.comment.model.CommentStatus;
import com.gentics.mesh.plugin.comment.model.NestedCommentSection;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.gentics.mesh.plugin.comment.TestCommentClient.User.EDITOR;
import static com.gentics.mesh.plugin.comment.TestUtil.loadResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NodeConfigFieldTest extends AbstractLocalIntegrationTest {
	@BeforeClass
	public static void createProject() throws Exception {
		// There are separate tests for sending mails, so this configuration contains no moderator email addresses.
		initMesh("nodeConfigField", loadResource("/nodeConfig/nodeConfigNoModerators.json"));
	}

	@Test
	public void testApprovalRequired() {
		Comment comment = client.addComment("comment1");
		client.setUser(EDITOR);

		comment = client.getComment(comment);
		assertEquals(CommentStatus.PENDING, comment.getStatus());
	}

	@Test
	public void testCommentsOfNode() {
		client.addComment("comment1");
		client.setUser(EDITOR);

		NestedCommentSection comments = commentsOfNodeResponse(client.getCommentsOfNode());
		assertTrue("Section should require Approval", comments.getFields().getRequiresApproval());
	}
}
