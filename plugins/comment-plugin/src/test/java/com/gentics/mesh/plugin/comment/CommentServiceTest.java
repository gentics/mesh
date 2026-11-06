package com.gentics.mesh.plugin.comment;

import com.gentics.mesh.plugin.comment.error.AnonymousNotAllowedError;
import com.gentics.mesh.plugin.comment.error.InvalidEmailException;
import com.gentics.mesh.plugin.comment.model.EffectiveCommentSection;
import com.gentics.mesh.plugin.comment.model.request.CreateCommentRequest;
import com.gentics.mesh.plugin.comment.model.request.UpdateCommentRequest;
import com.gentics.mesh.util.UUIDUtil;
import org.junit.Ignore;
import org.junit.Test;

import static com.gentics.mesh.plugin.comment.TestUtil.loadNodeResponse;
import static com.gentics.mesh.plugin.comment.TestUtil.loadUserResponse;

public class CommentServiceTest extends AbstractPluginTest {

	@Test
	public void testValidRequest() {
		EffectiveCommentSection section = new EffectiveCommentSection(loadNodeResponse("effectiveSection1"));
		CreateCommentRequest request = new CreateCommentRequest()
			.setAuthor("carl")
			.setEmail("carl@carlson.com")
			.setLanguage("en")
			.setNode("6a0ca4f03107442ea334987429d8d0d0");

		service.validateCommentRequest(request, section)
			.test()
			.assertComplete();
	}

	@Test
	public void testInvalidMail() {
		EffectiveCommentSection section = new EffectiveCommentSection(loadNodeResponse("effectiveSection1"));
		CreateCommentRequest request = new CreateCommentRequest()
			.setAuthor("carl")
			.setEmail("bogus")
			.setLanguage("en")
			.setNode("6a0ca4f03107442ea334987429d8d0d0");

		service.validateCommentRequest(request, section)
			.test()
			.assertError(InvalidEmailException.class);
	}

	@Test
	public void testValidNoMail() {
		EffectiveCommentSection section = new EffectiveCommentSection(loadNodeResponse("effectiveSection1"));
		CreateCommentRequest request = new CreateCommentRequest()
			.setAuthor("carl")
			.setLanguage("en")
			.setNode("6a0ca4f03107442ea334987429d8d0d0");

		service.validateCommentRequest(request, section)
			.test()
			.assertComplete();
	}

	@Test
	public void testDenyAnonymous() {
		EffectiveCommentSection section = new EffectiveCommentSection(loadNodeResponse("effectiveSection2"));
		CreateCommentRequest request = new CreateCommentRequest()
			.setAuthor("carl")
			.setLanguage("en")
			.setNode("6a0ca4f03107442ea334987429d8d0d0");

		service.validateCommentRequest(request, section)
			.test()
			.assertError(AnonymousNotAllowedError.class);
	}

	@Test
	public void allowLoggedIn() {
		EffectiveCommentSection section = new EffectiveCommentSection(loadNodeResponse("effectiveSection2"));
		CreateCommentRequest request = new CreateCommentRequest()
			.setAuthor("carl")
			.setLanguage("en")
			.setNode("6a0ca4f03107442ea334987429d8d0d0");

		userClient.setUser(loadUserResponse("admin"));

		service.validateCommentRequest(request, section)
			.test()
			.assertComplete();
	}

	@Ignore
	@Test
	public void testCreateComment() {
		CreateCommentRequest request = new CreateCommentRequest()
			.setAuthor("carl")
			.setLanguage("en")
			.setEmail("some@where.tld")
			.setNode("6a0ca4f03107442ea334987429d8d0d0");
		service.createComment(request)
			.test()
			.assertComplete();
	}

	@Ignore
	@Test
	public void testUpdateComment() {
		UpdateCommentRequest request = new UpdateCommentRequest();
		request.setAuthor("someothername");
		String uuid = UUIDUtil.randomUUID();
		service.updateComment(uuid, request)
			.test()
			.assertComplete();
	}

	@Ignore
	@Test
	public void testDeleteComment() {
		service.deleteComment(UUIDUtil.randomUUID())
			.test()
			.assertComplete();
	}

	@Test
	public void testPublishComment() {
		String uuid = UUIDUtil.randomUUID();
		service.publishComment(uuid)
			.test()
			.assertComplete();
	}

	@Test
	public void testUnpublishComment() {
		String uuid = UUIDUtil.randomUUID();
		service.unpublishComment(uuid)
			.test()
			.assertComplete();
	}

}
