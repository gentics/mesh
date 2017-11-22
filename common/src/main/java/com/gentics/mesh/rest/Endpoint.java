package com.gentics.mesh.rest;

import com.gentics.mesh.example.AdminExamples;
import com.gentics.mesh.example.GraphQLExamples;
import com.gentics.mesh.example.GroupExamples;
import com.gentics.mesh.example.JobExamples;
import com.gentics.mesh.example.MicroschemaExamples;
import com.gentics.mesh.example.MiscExamples;
import com.gentics.mesh.example.NodeExamples;
import com.gentics.mesh.example.ProjectExamples;
import com.gentics.mesh.example.ReleaseExamples;
import com.gentics.mesh.example.RoleExamples;
import com.gentics.mesh.example.SchemaExamples;
import com.gentics.mesh.example.TagExamples;
import com.gentics.mesh.example.TagFamilyExamples;
import com.gentics.mesh.example.UserExamples;
import com.gentics.mesh.example.UtilityExamples;
import com.gentics.mesh.example.VersioningExamples;

/**
 * An endpoint represents a specific path in the REST API which exposes various endpoint routes.
 * 
 * @author johannes2
 *
 */
public interface Endpoint {

	/**
	 * Create a new endpoint. Internally a new route will be wrapped.
	 * 
	 * @return Created endpoint
	 */
	EndpointRoute createEndpoint();

	static NodeExamples nodeExamples = new NodeExamples();
	static TagExamples tagExamples = new TagExamples();
	static TagFamilyExamples tagFamilyExamples = new TagFamilyExamples();
	static GroupExamples groupExamples = new GroupExamples();
	static RoleExamples roleExamples = new RoleExamples();
	static MiscExamples miscExamples = new MiscExamples();
	static VersioningExamples versioningExamples = new VersioningExamples();
	static SchemaExamples schemaExamples = new SchemaExamples();
	static ProjectExamples projectExamples = new ProjectExamples();
	static UserExamples userExamples = new UserExamples();
	static MicroschemaExamples microschemaExamples = new MicroschemaExamples();
	static GraphQLExamples graphqlExamples = new GraphQLExamples();
	static AdminExamples adminExamples = new AdminExamples();
	static JobExamples jobExamples = new JobExamples();
	static ReleaseExamples releaseExamples = new ReleaseExamples();
	static UtilityExamples utilityExamples = new UtilityExamples();

}
