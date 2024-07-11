package com.gentics.mesh.hibernate.data.dao;

import java.util.Optional;

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
import com.gentics.mesh.core.data.dao.DaoCollection;
import com.gentics.mesh.hibernate.data.HibQueryFieldMapper;
import com.gentics.mesh.hibernate.data.domain.AbstractFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.AbstractHibListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibBinaryFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibBinaryImpl;
import com.gentics.mesh.hibernate.data.domain.HibImageVariantImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibS3BinaryFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibS3BinaryImpl;
import com.gentics.mesh.hibernate.util.TypeInfoUtil;

/**
 * DAO and entity actions collection implementation for Enterprise Mesh.
 */
@Singleton
public class HibDaoCollectionImpl implements DaoCollection {

	private final UserDaoImpl userDao;
	private final UserDAOActions userActions;

	private final GroupDaoImpl groupDao;
	private final GroupDAOActions groupActions;

	private final RoleDaoImpl roleDao;
	private final RoleDAOActions roleActions;

	private final TagDaoImpl tagDao;
	private final TagDAOActions tagActions;

	private final TagFamilyDaoImpl tagFamilyDao;
	private final TagFamilyDAOActions tagFamilyActions;

	private final ProjectDaoImpl projectDao;
	private final ProjectDAOActions projectActions;

	private final BranchDaoImpl branchDao;
	private final BranchDAOActions branchActions;

	private final SchemaDaoImpl schemaDao;
	private final SchemaDAOActions schemaActions;

	private final MicroschemaDaoImpl microschemaDao;
	private final MicroschemaDAOActions microschemaActions;

	private final NodeDaoImpl nodeDao;
	private final ContentDaoImpl contentDao;

	private final LanguageDaoImpl languageDao;
	private final BinaryDaoImpl binaryDao;
	private final S3BinaryDaoImpl s3BinaryDao;
	private final JobDaoImpl jobDao;
	private final ImageVariantDaoImpl imageVariantDao;

	@Inject
	public HibDaoCollectionImpl(
			UserDaoImpl userDao,
			UserDAOActions userActions,
	
			GroupDaoImpl groupDao,
			GroupDAOActions groupActions,
	
			RoleDaoImpl roleDao,
			RoleDAOActions roleActions,
	
			ProjectDaoImpl projectDao,
			ProjectDAOActions projectActions,
	
			TagFamilyDaoImpl tagFamilyDao,
			TagFamilyDAOActions tagFamilyActions,
	
			TagDaoImpl tagDao,
			TagDAOActions tagActions,
	
			BranchDaoImpl branchDao,
			BranchDAOActions branchActions,
	
			SchemaDaoImpl schemaDao,
			SchemaDAOActions schemaActions,
	
			MicroschemaDaoImpl microschemaDao,
			MicroschemaDAOActions microschemaActions,
	
			NodeDaoImpl nodeDao,
			ContentDaoImpl contentDao,
	
			LanguageDaoImpl languageDao,
			BinaryDaoImpl binaryDao,
			S3BinaryDaoImpl s3BinaryDao,
			JobDaoImpl jobDao,
			ImageVariantDaoImpl imageVariantDao
	) {

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
		this.s3BinaryDao = s3BinaryDao;
		this.jobDao = jobDao;
		this.imageVariantDao = imageVariantDao;
	}

	@Override
	public UserDaoImpl userDao() {
		return userDao;
	}

	@Override
	public UserDAOActions userActions() {
		return userActions;
	}

	@Override
	public GroupDaoImpl groupDao() {
		return groupDao;
	}

	@Override
	public GroupDAOActions groupActions() {
		return groupActions;
	}

	@Override
	public RoleDaoImpl roleDao() {
		return roleDao;
	}

	@Override
	public RoleDAOActions roleActions() {
		return roleActions;
	}

	@Override
	public ProjectDaoImpl projectDao() {
		return projectDao;
	}

	@Override
	public ProjectDAOActions projectActions() {
		return projectActions;
	}

	@Override
	public LanguageDaoImpl languageDao() {
		return languageDao;
	}

	@Override
	public JobDaoImpl jobDao() {
		return jobDao;
	}

	@Override
	public TagFamilyDaoImpl tagFamilyDao() {
		return tagFamilyDao;
	}

	@Override
	public TagFamilyDAOActions tagFamilyActions() {
		return tagFamilyActions;
	}

	@Override
	public TagDaoImpl tagDao() {
		return tagDao;
	}

	@Override
	public TagDAOActions tagActions() {
		return tagActions;
	}

	@Override
	public BranchDaoImpl branchDao() {
		return branchDao;
	}

	@Override
	public BranchDAOActions branchActions() {
		return branchActions;
	}

	@Override
	public MicroschemaDaoImpl microschemaDao() {
		return microschemaDao;
	}

	@Override
	public MicroschemaDAOActions microschemaActions() {
		return microschemaActions;
	}

	@Override
	public SchemaDaoImpl schemaDao() {
		return schemaDao;
	}

	@Override
	public SchemaDAOActions schemaActions() {
		return schemaActions;
	}

	@Override
	public BinaryDaoImpl binaryDao() {
		return binaryDao;
	}

	@Override
	public S3BinaryDaoImpl s3binaryDao() {
		return s3BinaryDao;
	}

	@Override
	public NodeDaoImpl nodeDao() {
		return nodeDao;
	}

	@Override
	public ContentDaoImpl contentDao() {
		return contentDao;
	}

	@Override
	public ImageVariantDaoImpl imageVariantDao() {
		return imageVariantDao;
	}

	public Optional<AbstractHibDao<?>> maybeFindDao(Class<?> cls) {
		return Optional.ofNullable(TypeInfoUtil.getType(cls)).flatMap(this::maybeFindDao).map(AbstractHibDao.class::cast);
	}

	public Optional<HibQueryFieldMapper> maybeFindFieldMapper(Class<?> cls) {
		return maybeFindDao(cls).map(HibQueryFieldMapper.class::cast).or(() -> {
			HibQueryFieldMapper ret = null;
			if (cls == HibNodeFieldContainerImpl.class) {
				ret = contentDao;
			} else
			if (cls == HibMicronodeContainerImpl.class) {
				ret = ContentUtils.micronodeQueryFieldMapper();
			} else
			if (cls == HibNodeFieldContainerEdgeImpl.class) {
				ret = contentDao;
			} else
			if (cls == HibMicronodeFieldEdgeImpl.class) {
				ret = ContentUtils.micronodeQueryFieldMapper();
			} else
			if (cls == HibBinaryImpl.class || cls == HibBinaryFieldEdgeImpl.class) {
				ret = binaryDao;
			} else
			if (cls == HibS3BinaryImpl.class || cls == HibS3BinaryFieldEdgeImpl.class) {
				ret = s3BinaryDao;
			} else
			if (cls == AbstractHibListFieldEdgeImpl.class) {
				ret = ContentUtils.listFieldQueryMapper();
			} else
			if (cls == AbstractFieldEdgeImpl.class) {
				ret = ContentUtils.referenceFieldQueryMapper();
			} else
			if (cls == HibImageVariantImpl.class) {
				ret = imageVariantDao;
			}
			return Optional.ofNullable(ret);
		});
	}
}
