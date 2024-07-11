package com.gentics.mesh.graphql.filter;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.graphql.context.GraphQLContext;

/**
 * Microschema filter.
 */
public class MicroschemaFilter extends SchemaElementFilter<MicroschemaResponse, MicroschemaVersionModel, MicroschemaReference, Microschema, MicroschemaVersion> {

		private static final ElementType ELEMENT = ElementType.MICROSCHEMA;
		private static final String NAME = "MicroschemaFilter";

		public static MicroschemaFilter filter(GraphQLContext context) {
			return context.getOrStore(NAME, () -> new MicroschemaFilter(context));
		}

		private MicroschemaFilter(GraphQLContext context) {
			super(context, NAME, "Filters microschemas", ELEMENT);
		}

		@Override
		protected ElementType getEntityType() {
			return ELEMENT;
		}

		@Override
		protected Class<? extends MicroschemaVersionModel> getSchemaModelVersionClass() {
			return MicroschemaModelImpl.class;
		}

		@Override
		protected MicroschemaDao getSchemaElementDao() {
			return Tx.get().microschemaDao();
		}
}
