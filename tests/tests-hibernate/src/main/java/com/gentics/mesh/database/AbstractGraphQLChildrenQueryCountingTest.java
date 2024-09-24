package com.gentics.mesh.database;

public abstract class AbstractGraphQLChildrenQueryCountingTest extends AbstractCountingTest {

	protected static final String QUERY_ROOT = "query { node (uuid: \"%s\", lang: [\"en\"]) { %s } }";

	protected static final String CHILDREN_QUERY = "%s: children(filter: {schema: {is: %s}} %s) {\n"
			+ "elements { %s }\n"
			+ "totalCount\n"
			+ "}";

	protected static final String CHILDREN_FIELDS = "\n"
			+ "		uuid\n"
			+ "		language\n"
			+ "		schema {name}\n";
}
