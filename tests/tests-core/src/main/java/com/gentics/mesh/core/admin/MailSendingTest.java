package com.gentics.mesh.core.admin;

import static com.gentics.mesh.MeshVersion.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.perm.InternalPermission.*;
import static com.gentics.mesh.core.rest.MeshEvent.*;
import static com.gentics.mesh.core.rest.common.Permission.*;
import static com.gentics.mesh.core.rest.job.JobStatus.*;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.INITIAL_BRANCH_NAME;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldEndpointTest;
import com.gentics.mesh.core.rest.admin.mail.MailAttachmentsResponse;
import com.gentics.mesh.core.rest.admin.mail.MailSendingResponse;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchListResponse;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.core.rest.branch.info.BranchInfoMicroschemaList;
import com.gentics.mesh.core.rest.branch.info.BranchInfoSchemaList;
import com.gentics.mesh.core.rest.branch.info.BranchMicroschemaInfo;
import com.gentics.mesh.core.rest.branch.info.BranchSchemaInfo;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.event.branch.BranchMicroschemaAssignModel;
import com.gentics.mesh.core.rest.event.branch.BranchSchemaAssignEventModel;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.event.project.ProjectBranchEventModel;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.parameter.impl.SchemaUpdateParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.definition.BasicRestTestcases;
import com.gentics.mesh.test.util.TestUtils;
import com.gentics.mesh.util.UUIDUtil;
import io.reactivex.Observable;

@MeshTestSetting(testSize = FULL, startServer = true)
public class MailSendingTest extends AbstractMeshTest {
	public static final String PROJECT_NAME = "test";

	@Test
	public void testEnqueueMailSendingJob() {
		MailSendingResponse par = new MailSendingResponse();
		ArrayList to = new ArrayList();
			to.add("dajena@test.com");
		par.setTo(to);
		par.setSubject("Test Subject");
		par.setFrom("dajena@test-domain.com");
		par.setText("Hello, This is a test");
		String jobUuid = tx(() -> {
			HibJob job = boot().jobDao().enqueueMailSending(user(), new Timestamp(System.currentTimeMillis()).getTime(), String.valueOf(par.toJson()));
			return job.getUuid();
		});

		JobListResponse status = adminCall(() -> client().findJobs());
		assertThat(status).listsAll(QUEUED).hasInfos(1).containsJobs(jobUuid);
	}
	@Test
	public void testTryToSendEmailWithoutAttachments() {
		MailSendingResponse par = new MailSendingResponse();
		ArrayList to = new ArrayList();
		to.add("person1@test.com");
		to.add("person2@test.com");

		ArrayList cc = new ArrayList();
		cc.add("cc1@test.com");
		cc.add("cc2@test.com");
		cc.add("cc3@test.com");

		ArrayList bcc = new ArrayList();
		bcc.add("bcc1@test.com");
		bcc.add("bcc2@test.com");
		bcc.add("bcc3@test.com");

		par.setBounceAddress("test");
		par.setTo(to);
		par.setCc(cc);
		par.setBcc(bcc);

		par.setSubject("Test Subject");
		par.setFrom("dajena@test-domain.com");
		par.setText("Hello, This is a test");
		par.setHtml("<!DOCTYPE html><html><head><style id=\\\"content\\\" type=\\\"text/css\\\">body{ /* Font-Size in body must be in percent. Otherwise IE has a cascading bug. */ font-size: 80%; font-family: Arial, Helvetica, sans-serif;}h1{ font-size: 200%;}h2{ font-size: 150%;}h3{ font-size: 125%;}h4{ font-size: 100%;}h5{ font-size: 80%;}h6{ font-size: 60%;}p{ margin-top: 0px; margin-bottom: 0px;}.link { text-decoration: underline !important;}</style></head><body><p>Guten Tag</p><p>&nbsp;</p><p>This is a HTMl Mail Message.</p><p><br />This Mail Message was created from Mesh tests<br />&nbsp;<br />Best Regards<br />Mesh Team</p></body></html>");
		String jobUuid = tx(() -> {
			HibJob job = boot().jobDao().enqueueMailSending(user(), new Timestamp(System.currentTimeMillis()).getTime(), String.valueOf(par.toJson()));
			return job.getUuid();
		});
		waitForJob(() -> {
			adminCall(() -> client().processJob(jobUuid));
		}, jobUuid, COMPLETED);

		JobListResponse status = adminCall(() -> client().findJobs());
		assertThat(status).listsAll(COMPLETED).hasInfos(1).containsJobs(jobUuid);
	}
	@Test
	public void testTryToSendEmailWithoutTo() {
		MailSendingResponse par = new MailSendingResponse();
		ArrayList to = new ArrayList();
		to.add("dajena@test.com");
		//par.setTo(to);
		par.setSubject("Test Subject");
		par.setFrom("dajena@test-domain.com");
		par.setText("Hello, This is a test");
		String jobUuid = tx(() -> {
			HibJob job = boot().jobDao().enqueueMailSending(user(), new Timestamp(System.currentTimeMillis()).getTime(), String.valueOf(par.toJson()));
			return job.getUuid();
		});
		waitForJob(() -> {
			adminCall(() -> client().processJob(jobUuid));
		}, jobUuid, FAILED);

		JobListResponse status = adminCall(() -> client().findJobs());
		assertThat(status).listsAll(FAILED).hasInfos(1).containsJobs(jobUuid);
	}
	@Test
	public void testTryToSendEmailWithoutFrom() {
		MailSendingResponse par = new MailSendingResponse();
		ArrayList to = new ArrayList();
		to.add("dajena@test.com");
		par.setTo(to);
		par.setSubject("Test Subject");
		//par.setFrom("dajena@test-domain.com");
		par.setText("Hello, This is a test");
		String jobUuid = tx(() -> {
			HibJob job = boot().jobDao().enqueueMailSending(user(), new Timestamp(System.currentTimeMillis()).getTime(), String.valueOf(par.toJson()));
			return job.getUuid();
		});
		waitForJob(() -> {
			adminCall(() -> client().processJob(jobUuid));
		}, jobUuid, FAILED);

		JobListResponse status = adminCall(() -> client().findJobs());
		assertThat(status).listsAll(FAILED).hasInfos(1).containsJobs(jobUuid);
	}
	@Test
	public void testTryToSendEmailWithAttachmentsWithoutProjects() {
		MailSendingResponse par = new MailSendingResponse();
		ArrayList to = new ArrayList();
		to.add("dajena@test.com");
		par.setTo(to);
		par.setSubject("Test Subject");
		par.setFrom("dajena@test-domain.com");
		par.setText("Hello, This is a test");
		MailAttachmentsResponse attachment1 = new MailAttachmentsResponse();
		attachment1.setProject("test");
		attachment1.setField("binary");
		attachment1.setUuid("c898e5259d004fb5bf7a220ca0bec903");
		attachment1.setLanguage("de");
		ArrayList<MailAttachmentsResponse> attachments = new ArrayList<>();
		attachments.add(attachment1);
		par.setAttachments(attachments);
		String jobUuid = tx(() -> {
			HibJob job = boot().jobDao().enqueueMailSending(user(), new Timestamp(System.currentTimeMillis()).getTime(), String.valueOf(par.toJson()));
			return job.getUuid();
		});
		waitForJob(() -> {
			adminCall(() -> client().processJob(jobUuid));
		}, jobUuid, FAILED);

		JobListResponse status = adminCall(() -> client().findJobs());
		assertThat(status).listsAll(FAILED).hasInfos(1).containsJobs(jobUuid);
	}
	@Test
	public void testTryToSendEmailWithAttachments() throws IOException {
		MailSendingResponse par = new MailSendingResponse();
		ArrayList to = new ArrayList();
		to.add("dajena@test.com");
		par.setTo(to);
		par.setSubject("Test Subject");
		par.setFrom("dajena@test-domain.com");
		par.setText("Hello, This is a test");
		NodeResponse image = uploadImage();
		MailAttachmentsResponse attachment1 = new MailAttachmentsResponse();
		attachment1.setProject(image.getProject().getName());
		attachment1.setField("binary");
		attachment1.setUuid(image.getUuid());
		attachment1.setLanguage(image.getLanguage());

		NodeResponse pdf = uploadPdf();
		MailAttachmentsResponse attachment2 = new MailAttachmentsResponse();
		attachment2.setProject(pdf.getProject().getName());
		attachment2.setField("binary");
		attachment2.setUuid(pdf.getUuid());
		attachment2.setLanguage(pdf.getLanguage());


		NodeResponse word = uploadWordDoc();
		MailAttachmentsResponse attachment3 = new MailAttachmentsResponse();
		attachment3.setProject(word.getProject().getName());
		attachment3.setField("binary");
		attachment3.setUuid(word.getUuid());
		attachment3.setLanguage(word.getLanguage());

		ArrayList<MailAttachmentsResponse> attachments = new ArrayList<>();
		attachments.add(attachment1);
		attachments.add(attachment2);
		attachments.add(attachment3);
		par.setAttachments(attachments);
		String jobUuid = tx(() -> {
			HibJob job = boot().jobDao().enqueueMailSending(user(), new Timestamp(System.currentTimeMillis()).getTime(), String.valueOf(par.toJson()));
			return job.getUuid();
		});
		waitForJob(() -> {
			adminCall(() -> client().processJob(jobUuid));
		}, jobUuid, COMPLETED);

		JobListResponse status = adminCall(() -> client().findJobs());
		assertThat(status).listsAll(COMPLETED).hasInfos(1).containsJobs(jobUuid);
	}

	@Test
	public void testTryToSendEmailWithAttachmentsWrongProject() throws IOException {
		MailSendingResponse par = new MailSendingResponse();
		ArrayList to = new ArrayList();
		to.add("dajena@test.com");
		par.setTo(to);
		par.setSubject("Test Subject");
		par.setFrom("dajena@test-domain.com");
		par.setText("Hello, This is a test");
		NodeResponse image = uploadImage();
		MailAttachmentsResponse attachment1 = new MailAttachmentsResponse();
		attachment1.setProject("notexistingproject");
		attachment1.setField("binary");
		attachment1.setUuid(image.getUuid());
		attachment1.setLanguage(image.getLanguage());


		ArrayList<MailAttachmentsResponse> attachments = new ArrayList<>();
		attachments.add(attachment1);
		par.setAttachments(attachments);
		String jobUuid = tx(() -> {
			HibJob job = boot().jobDao().enqueueMailSending(user(), new Timestamp(System.currentTimeMillis()).getTime(), String.valueOf(par.toJson()));
			return job.getUuid();
		});
		waitForJob(() -> {
			adminCall(() -> client().processJob(jobUuid));
		}, jobUuid, FAILED);

		JobListResponse status = adminCall(() -> client().findJobs());
		assertThat(status).listsAll(FAILED).hasInfos(1).containsJobs(jobUuid);
	}

	public NodeResponse uploadImage() throws IOException {
		ProjectResponse project = adminCall(() -> client().findProjectByName(PROJECT_NAME));

		SchemaResponse schemaResponse = adminCall(() -> client().findSchemas()).getData().stream()
			.filter(schema -> schema.getName().equalsIgnoreCase("binary_content"))
			.findFirst().get();
		String binaryContentSchemaUuid = schemaResponse.getUuid();

		adminCall(() -> client().assignSchemaToProject(PROJECT_NAME, binaryContentSchemaUuid));
		String path = new File(getClass().getResource("/pictures/android-gps.jpg").getPath())
			.getAbsolutePath();
		File image = new File(path);
				// 5. Create node
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setSchemaName("binary_content");
		nodeCreateRequest.setParentNodeUuid(project.getRootNode().getUuid());
		NodeResponse binaryNode = adminCall(()-> client().createNode(PROJECT_NAME, nodeCreateRequest));
		FileInputStream ins = new FileInputStream(image);
		long len = image.length();
		//update the binary field with the image
		adminCall(()-> client().updateNodeBinaryField(PROJECT_NAME, binaryNode.getUuid(), "en", "draft", "binary", ins, len, image.getName(), "application/jpeg"));
		return binaryNode;

	}
	public NodeResponse uploadPdf() throws IOException {
		ProjectResponse project = adminCall(() -> client().findProjectByName(PROJECT_NAME));

		SchemaResponse schemaResponse = adminCall(() -> client().findSchemas()).getData().stream()
			.filter(schema -> schema.getName().equalsIgnoreCase("binary_content"))
			.findFirst().get();
		String binaryContentSchemaUuid = schemaResponse.getUuid();

		adminCall(() -> client().assignSchemaToProject(PROJECT_NAME, binaryContentSchemaUuid));
		String path = new File(getClass().getResource("/testfiles/test.pdf").getPath())
			.getAbsolutePath();
		File image = new File(path);
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setSchemaName("binary_content");
		nodeCreateRequest.setParentNodeUuid(project.getRootNode().getUuid());
		NodeResponse binaryNode = adminCall(()-> client().createNode(PROJECT_NAME, nodeCreateRequest));
		FileInputStream ins = new FileInputStream(image);
		long len = image.length();
		//update the binary field with the image
		adminCall(()-> client().updateNodeBinaryField(PROJECT_NAME, binaryNode.getUuid(), "en", "draft", "binary", ins, len, image.getName(), "application/pdf"));
		return binaryNode;

	}
	@Before
	public void getProjectResponse() {
		ProjectCreateRequest projectRequest = new ProjectCreateRequest();
		projectRequest.setName(PROJECT_NAME);
		projectRequest.setSchemaRef("folder");
		adminCall(() -> client().createProject(projectRequest));
	}

	public NodeResponse uploadWordDoc() throws IOException {
		ProjectResponse project = adminCall(() -> client().findProjectByName(PROJECT_NAME));
		SchemaResponse schemaResponse = adminCall(() -> client().findSchemas()).getData().stream()
			.filter(schema -> schema.getName().equalsIgnoreCase("binary_content"))
			.findFirst().get();
		String binaryContentSchemaUuid = schemaResponse.getUuid();

		adminCall(() -> client().assignSchemaToProject(PROJECT_NAME, binaryContentSchemaUuid));
		String path = new File(getClass().getResource("/testfiles/test.docx").getPath())
			.getAbsolutePath();
		File image = new File(path);
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setSchemaName("binary_content");
		nodeCreateRequest.setParentNodeUuid(project.getRootNode().getUuid());
		NodeResponse binaryNode = adminCall(()-> client().createNode(PROJECT_NAME, nodeCreateRequest));
		FileInputStream ins = new FileInputStream(image);
		long len = image.length();
		//update the binary field with the image
		adminCall(()-> client().updateNodeBinaryField(PROJECT_NAME, binaryNode.getUuid(), "en", "draft", "binary", ins, len, image.getName(), "application/docx"));
		return binaryNode;

	}
	@Test
	public void testTheRestClient() {
		MailSendingResponse par = new MailSendingResponse();
		ArrayList to = new ArrayList();
		to.add("person1@test.com");
		to.add("person2@test.com");

		ArrayList cc = new ArrayList();
		cc.add("cc1@test.com");
		cc.add("cc2@test.com");
		cc.add("cc3@test.com");

		ArrayList bcc = new ArrayList();
		bcc.add("bcc1@test.com");
		bcc.add("bcc2@test.com");
		bcc.add("bcc3@test.com");

		par.setBounceAddress("test");
		par.setTo(to);
		par.setCc(cc);
		par.setBcc(bcc);

		par.setSubject("Test Subject");
		par.setFrom("dajena@test-domain.com");
		par.setText("Hello, This is a test from testTheRestClient");
		par.setHtml("<!DOCTYPE html><html><head><style id=\\\"content\\\" type=\\\"text/css\\\">body{ /* Font-Size in body must be in percent. Otherwise IE has a cascading bug. */ font-size: 80%; font-family: Arial, Helvetica, sans-serif;}h1{ font-size: 200%;}h2{ font-size: 150%;}h3{ font-size: 125%;}h4{ font-size: 100%;}h5{ font-size: 80%;}h6{ font-size: 60%;}p{ margin-top: 0px; margin-bottom: 0px;}.link { text-decoration: underline !important;}</style></head><body><p>Guten Tag</p><p>&nbsp;</p><p>This is a HTMl Mail Message.</p><p><br />This Mail Message was created from Mesh tests<br />&nbsp;<br />Best Regards<br />Mesh Team</p></body></html>");

		GenericMessageResponse message = adminCall(() -> client().sendEmail(par));
		assertEquals(message.getMessage(), "The mail has been sent!");

	}

}
