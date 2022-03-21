package com.gentics.mesh.dagger.module;

import java.util.Map;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.data.dao.DaoTransformable;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.dao.JobDao;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.dao.TagFamilyDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;

/**
 * Provides bindings to conveniently inject a map of daos used for transforming elements. To use the map inject a
 * <code>Map<ElementType, DaoTransformable<HibCoreElement, RestModel>></code>.
 */
@Module
public abstract class DaoTransformableModule {

	@Getter
	@Provides
	@SuppressWarnings("unchecked")
	public static Map<ElementType, DaoTransformable<HibCoreElement<? extends RestModel>, RestModel>> map(
		Map<ElementType, DaoTransformable<? extends HibCoreElement<? extends RestModel>, ? extends RestModel>> daos) {
		return (Map<ElementType, DaoTransformable<HibCoreElement<? extends RestModel>, RestModel>>) (Object) daos;
	}

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.ROLE)
	abstract DaoTransformable<? extends HibCoreElement<? extends RestModel>, ? extends RestModel> role(RoleDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.GROUP)
	abstract DaoTransformable<? extends HibCoreElement<? extends RestModel>, ? extends RestModel> group(GroupDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.USER)
	abstract DaoTransformable<? extends HibCoreElement<? extends RestModel>, ? extends RestModel> user(UserDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.PROJECT)
	abstract DaoTransformable<? extends HibCoreElement<? extends RestModel>, ? extends RestModel> project(ProjectDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.SCHEMA)
	abstract DaoTransformable<? extends HibCoreElement<? extends RestModel>, ? extends RestModel> schema(SchemaDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.MICROSCHEMA)
	abstract DaoTransformable<? extends HibCoreElement<? extends RestModel>, ? extends RestModel> microschema(MicroschemaDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.BRANCH)
	abstract DaoTransformable<? extends HibCoreElement<? extends RestModel>, ? extends RestModel> branch(BranchDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.NODE)
	abstract DaoTransformable<? extends HibCoreElement<? extends RestModel>, ? extends RestModel> node(NodeDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.TAG)
	abstract DaoTransformable<? extends HibCoreElement<? extends RestModel>, ? extends RestModel> tag(TagDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.TAGFAMILY)
	abstract DaoTransformable<? extends HibCoreElement<? extends RestModel>, ? extends RestModel> tagFamily(TagFamilyDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.JOB)
	abstract DaoTransformable<? extends HibCoreElement<? extends RestModel>, ? extends RestModel> job(JobDao dao);

}
