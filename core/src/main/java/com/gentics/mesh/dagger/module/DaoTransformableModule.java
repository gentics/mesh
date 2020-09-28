package com.gentics.mesh.dagger.module;

import java.util.Map;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.dao.DaoTransformable;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.dao.TagFamilyDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
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

	@Provides
	@SuppressWarnings("unchecked")
	public static Map<ElementType, DaoTransformable<HibCoreElement, RestModel>> map(
		Map<ElementType, DaoTransformable<? extends HibCoreElement, ? extends RestModel>> daos) {
		return (Map<ElementType, DaoTransformable<HibCoreElement, RestModel>>) (Object) daos;
	}

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.ROLE)
	abstract DaoTransformable<? extends HibCoreElement, ? extends RestModel> role(RoleDaoWrapper dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.GROUP)
	abstract DaoTransformable<? extends HibCoreElement, ? extends RestModel> group(GroupDaoWrapper dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.USER)
	abstract DaoTransformable<? extends HibCoreElement, ? extends RestModel> user(UserDaoWrapper dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.PROJECT)
	abstract DaoTransformable<? extends HibCoreElement, ? extends RestModel> project(ProjectDaoWrapper dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.SCHEMA)
	abstract DaoTransformable<? extends HibCoreElement, ? extends RestModel> schema(SchemaDaoWrapper dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.MICROSCHEMA)
	abstract DaoTransformable<? extends HibCoreElement, ? extends RestModel> microschema(MicroschemaDaoWrapper dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.TAG)
	abstract DaoTransformable<? extends HibCoreElement, ? extends RestModel> tag(TagDaoWrapper dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.TAGFAMILY)
	abstract DaoTransformable<? extends HibCoreElement, ? extends RestModel> tagFamily(TagFamilyDaoWrapper dao);
}
