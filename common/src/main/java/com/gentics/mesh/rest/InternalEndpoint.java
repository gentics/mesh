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
 */
public interface InternalEndpoint {

	NodeExamples nodeExamples = new NodeExamples();
	TagExamples tagExamples = new TagExamples();
	TagFamilyExamples tagFamilyExamples = new TagFamilyExamples();
	GroupExamples groupExamples = new GroupExamples();
	RoleExamples roleExamples = new RoleExamples();
	MiscExamples miscExamples = new MiscExamples();
	VersioningExamples versioningExamples = new VersioningExamples();
	SchemaExamples schemaExamples = new SchemaExamples();
	ProjectExamples projectExamples = new ProjectExamples();
	UserExamples userExamples = new UserExamples();
	MicroschemaExamples microschemaExamples = new MicroschemaExamples();
	GraphQLExamples graphqlExamples = new GraphQLExamples();
	AdminExamples adminExamples = new AdminExamples();
	JobExamples jobExamples = new JobExamples();
	ReleaseExamples releaseExamples = new ReleaseExamples();
	UtilityExamples utilityExamples = new UtilityExamples();

	/**
	 * Create a new endpoint. Internally a new route will be wrapped.
	 * 
	 * @return Created endpoint
	 */
	InternalEndpointRoute createRoute();

}
