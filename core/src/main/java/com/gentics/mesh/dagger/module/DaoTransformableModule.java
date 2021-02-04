package com.gentics.mesh.dagger.module;

import java.util.Map;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.dao.OrientDBBranchDao;
import com.gentics.mesh.core.data.dao.DaoTransformable;
import com.gentics.mesh.core.data.dao.OrientDBGroupDao;
import com.gentics.mesh.core.data.dao.OrientDBJobDao;
import com.gentics.mesh.core.data.dao.OrientDBMicroschemaDao;
import com.gentics.mesh.core.data.dao.OrientDBNodeDao;
import com.gentics.mesh.core.data.dao.OrientDBProjectDao;
import com.gentics.mesh.core.data.dao.OrientDBRoleDao;
import com.gentics.mesh.core.data.dao.OrientDBSchemaDao;
import com.gentics.mesh.core.data.dao.OrientDBTagDao;
import com.gentics.mesh.core.data.dao.OrientDBTagFamilyDao;
import com.gentics.mesh.core.data.dao.OrientDBUserDao;
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
	public static Map<ElementType, DaoTransformable<HibCoreElement, RestModel>> map(
		Map<ElementType, DaoTransformable<? extends HibCoreElement, ? extends RestModel>> daos) {
		return (Map<ElementType, DaoTransformable<HibCoreElement, RestModel>>) (Object) daos;
	}

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.ROLE)
	abstract DaoTransformable<? extends HibCoreElement, ? extends RestModel> role(OrientDBRoleDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.GROUP)
	abstract DaoTransformable<? extends HibCoreElement, ? extends RestModel> group(OrientDBGroupDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.USER)
	abstract DaoTransformable<? extends HibCoreElement, ? extends RestModel> user(OrientDBUserDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.PROJECT)
	abstract DaoTransformable<? extends HibCoreElement, ? extends RestModel> project(OrientDBProjectDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.SCHEMA)
	abstract DaoTransformable<? extends HibCoreElement, ? extends RestModel> schema(OrientDBSchemaDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.MICROSCHEMA)
	abstract DaoTransformable<? extends HibCoreElement, ? extends RestModel> microschema(OrientDBMicroschemaDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.BRANCH)
	abstract DaoTransformable<? extends HibCoreElement, ? extends RestModel> branch(OrientDBBranchDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.NODE)
	abstract DaoTransformable<? extends HibCoreElement, ? extends RestModel> node(OrientDBNodeDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.TAG)
	abstract DaoTransformable<? extends HibCoreElement, ? extends RestModel> tag(OrientDBTagDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.TAGFAMILY)
	abstract DaoTransformable<? extends HibCoreElement, ? extends RestModel> tagFamily(OrientDBTagFamilyDao dao);

	@Binds
	@IntoMap
	@ElementTypeKey(ElementType.JOB)
	abstract DaoTransformable<? extends HibCoreElement, ? extends RestModel> job(OrientDBJobDao dao);

}
