package com.gentics.mesh.core.data.dao;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.action.BranchDAOActions;
import com.gentics.mesh.core.action.GroupDAOActions;
import com.gentics.mesh.core.action.MicroschemaDAOActions;
import com.gentics.mesh.core.action.ProjectDAOActions;
import com.gentics.mesh.core.action.RoleDAOActions;
import com.gentics.mesh.core.action.SchemaDAOActions;
import com.gentics.mesh.core.action.TagDAOActions;
import com.gentics.mesh.core.action.TagFamilyDAOActions;
import com.gentics.mesh.core.action.UserDAOActions;
import com.gentics.mesh.core.context.ContextDataRegistry;

/**
 * Container provider for various DAO's.
 * 
 * TODO MDM - Refactor to only use Dao's and not DaoActions.
 */
@Singleton
public class OrientDBDaoCollection implements DaoCollection {

	private final UserDaoWrapper userDao;
	private final UserDAOActions userActions;

	private final GroupDaoWrapper groupDao;
	private final GroupDAOActions groupActions;

	private final RoleDaoWrapper roleDao;
	private final RoleDAOActions roleActions;

	private final TagDaoWrapper tagDao;
	private final TagDAOActions tagActions;

	private final TagFamilyDaoWrapper tagFamilyDao;
	private final TagFamilyDAOActions tagFamilyActions;

	private final ProjectDaoWrapper projectDao;
	private final ProjectDAOActions projectActions;

	private final BranchDaoWrapper branchDao;
	private final BranchDAOActions branchActions;

	private final SchemaDaoWrapper schemaDao;
	private final SchemaDAOActions schemaActions;

	private final MicroschemaDaoWrapper microschemaDao;
	private final MicroschemaDAOActions microschemaActions;

	private final NodeDaoWrapper nodeDao;
	private final ContentDaoWrapper contentDao;

	private final LanguageDaoWrapper languageDao;
	private final BinaryDaoWrapper binaryDao;
	private final S3BinaryDaoWrapper s3binaryDao;
	private final JobDaoWrapper jobDao;
	private final ImageVariantDaoWrapper imageVariantDao;

	@Inject
	public OrientDBDaoCollection(
		UserDaoWrapper userDao,
		UserDAOActions userActions,

		GroupDaoWrapper groupDao,
		GroupDAOActions groupActions,

		RoleDaoWrapper roleDao,
		RoleDAOActions roleActions,

		ProjectDaoWrapper projectDao,
		ProjectDAOActions projectActions,

		TagFamilyDaoWrapper tagFamilyDao,
		TagFamilyDAOActions tagFamilyActions,

		TagDaoWrapper tagDao,
		TagDAOActions tagActions,

		BranchDaoWrapper branchDao,
		BranchDAOActions branchActions,

		SchemaDaoWrapper schemaDao,
		SchemaDAOActions schemaActions,

		MicroschemaDaoWrapper microschemaDao,
		MicroschemaDAOActions microschemaActions,

		NodeDaoWrapper nodeDao,
		ContentDaoWrapper contentDao,

		LanguageDaoWrapper languageDao,
		BinaryDaoWrapper binaryDao,
		S3BinaryDaoWrapper s3binaryDao,
		JobDaoWrapper jobDao,
		ImageVariantDaoWrapper imageVariantDao,

		ContextDataRegistry contextDataRegistry) {
		this.userDao = userDao;
		this.userActions = userActions;

		this.groupDao = groupDao;
		this.groupActions = groupActions;

		this.roleDao = roleDao;
		this.roleActions = roleActions;

		this.tagDao = tagDao;
		this.tagActions = tagActions;

		this.tagFamilyDao = tagFamilyDao;
		this.tagFamilyActions = tagFamilyActions;

		this.branchDao = branchDao;
		this.branchActions = branchActions;

		this.schemaDao = schemaDao;
		this.schemaActions = schemaActions;

		this.microschemaDao = microschemaDao;
		this.microschemaActions = microschemaActions;

		this.nodeDao = nodeDao;
		this.contentDao = contentDao;

		this.projectDao = projectDao;
		this.projectActions = projectActions;

		this.languageDao = languageDao;
		this.binaryDao = binaryDao;
		this.s3binaryDao=s3binaryDao;
		this.jobDao = jobDao;
		this.imageVariantDao = imageVariantDao;
	}

	@Override
	public UserDaoWrapper userDao() {
		return userDao;
	}

	@Override
	public UserDAOActions userActions() {
		return userActions;
	}

	@Override
	public GroupDaoWrapper groupDao() {
		return groupDao;
	}

	@Override
	public GroupDAOActions groupActions() {
		return groupActions;
	}

	@Override
	public RoleDaoWrapper roleDao() {
		return roleDao;
	}

	@Override
	public RoleDAOActions roleActions() {
		return roleActions;
	}

	@Override
	public ProjectDaoWrapper projectDao() {
		return projectDao;
	}

	@Override
	public ProjectDAOActions projectActions() {
		return projectActions;
	}

	@Override
	public LanguageDaoWrapper languageDao() {
		return languageDao;
	}

	@Override
	public JobDaoWrapper jobDao() {
		return jobDao;
	}

	@Override
	public TagFamilyDaoWrapper tagFamilyDao() {
		return tagFamilyDao;
	}

	@Override
	public TagFamilyDAOActions tagFamilyActions() {
		return tagFamilyActions;
	}

	@Override
	public TagDaoWrapper tagDao() {
		return tagDao;
	}

	@Override
	public TagDAOActions tagActions() {
		return tagActions;
	}

	@Override
	public BranchDaoWrapper branchDao() {
		return branchDao;
	}

	@Override
	public BranchDAOActions branchActions() {
		return branchActions;
	}

	@Override
	public MicroschemaDaoWrapper microschemaDao() {
		return microschemaDao;
	}

	@Override
	public MicroschemaDAOActions microschemaActions() {
		return microschemaActions;
	}

	@Override
	public SchemaDaoWrapper schemaDao() {
		return schemaDao;
	}

	@Override
	public SchemaDAOActions schemaActions() {
		return schemaActions;
	}

	@Override
	public BinaryDaoWrapper binaryDao() {
		return binaryDao;
	}

	@Override
	public S3BinaryDaoWrapper s3binaryDao() {
		return s3binaryDao;
	}

	@Override
	public NodeDaoWrapper nodeDao() {
		return nodeDao;
	}

	@Override
	public ContentDaoWrapper contentDao() {
		return contentDao;
	}

	@Override
	public ImageVariantDaoWrapper imageVariantDao() {
		return imageVariantDao;
	}
}
