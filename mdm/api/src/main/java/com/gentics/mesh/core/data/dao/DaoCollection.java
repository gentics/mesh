package com.gentics.mesh.core.data.dao;

import java.util.Optional;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.action.BranchDAOActions;
import com.gentics.mesh.core.action.GroupDAOActions;
import com.gentics.mesh.core.action.MicroschemaDAOActions;
import com.gentics.mesh.core.action.ProjectDAOActions;
import com.gentics.mesh.core.action.RoleDAOActions;
import com.gentics.mesh.core.action.SchemaDAOActions;
import com.gentics.mesh.core.action.TagDAOActions;
import com.gentics.mesh.core.action.TagFamilyDAOActions;
import com.gentics.mesh.core.action.UserDAOActions;

/*
 * Aggregated collection of DAOs
 */
public interface DaoCollection {

	ImageVariantDao imageVariantDao();

	UserDao userDao();

	UserDAOActions userActions();

	GroupDao groupDao();

	GroupDAOActions groupActions();

	RoleDao roleDao();

	RoleDAOActions roleActions();

	ProjectDao projectDao();

	ProjectDAOActions projectActions();

	LanguageDao languageDao();

	JobDao jobDao();

	TagFamilyDao tagFamilyDao();

	TagFamilyDAOActions tagFamilyActions();

	TagDao tagDao();

	TagDAOActions tagActions();

	BranchDao branchDao();

	BranchDAOActions branchActions();

	MicroschemaDao microschemaDao();

	MicroschemaDAOActions microschemaActions();

	SchemaDao schemaDao();

	SchemaDAOActions schemaActions();

	BinaryDao binaryDao();

	S3BinaryDao s3binaryDao();

	NodeDao nodeDao();

	ContentDao contentDao();

	/**
	 * Try looking for a DAO that corresponds to the {@link ElementType}. Not every type is supported though.
	 * 
	 * @param etype
	 * @return
	 */
	default Optional<Dao<?>> maybeFindDao(ElementType etype) {
		Dao<?> dao = null;
		switch (etype) {
		case BRANCH:
			dao = branchDao();
			break;
		case GROUP:
			dao = groupDao();
			break;
		case JOB:
			dao = jobDao();
			break;
		case LANGUAGE:
			dao = languageDao();
			break;
		case MICROSCHEMA:
			dao = microschemaDao();
			break;
		case MICROSCHEMAVERSION:
			// currently unsupported
			break;
		case NODE:
			dao = nodeDao();
			break;
		case PROJECT:
			dao = projectDao();
			break;
		case ROLE:
			dao = roleDao();
			break;
		case SCHEMA:
			dao = schemaDao();
			break;
		case SCHEMAVERSION:
			// currently unsupported
			break;
		case TAG:
			dao = tagDao();
			break;
		case TAGFAMILY:
			dao = tagFamilyDao();
			break;
		case USER:
			dao = userDao();
			break;
		default:
			break;
		}
		return Optional.ofNullable(dao);
	}
}
