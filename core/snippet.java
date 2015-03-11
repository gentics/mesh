
		// TraversalDescription tagTraversal = db.traversalDescription().depthFirst().relationships(BasicRelationships.TYPES.HAS_SUB_TAG)
		// .evaluator(includeWhereI18NameIs(parts)).uniqueness(Uniqueness.RELATIONSHIP_GLOBAL);
		// for (Path graphPath : tagTraversal.traverse(rootTag)) {
		// System.out.println(graphPath);
		// // System.out.println(rel.getStartNode() + " " + rel.getType() + " " + rel.getEndNode());
		// }
		return null;
		// for (Relationship rel : graphDb.traversalDescription().depthFirst().relationships(AuthRelationships.TYPES.MEMBER_OF, Direction.OUTGOING)
		// .relationships(AuthRelationships.TYPES.HAS_ROLE, Direction.INCOMING)
		// .relationships(AuthRelationships.TYPES.HAS_PERMISSION, Direction.OUTGOING).uniqueness(Uniqueness.RELATIONSHIP_GLOBAL)
		// .traverse(userNode).relationships()) {

// public static PathEvaluator<Integer> includeWhereI18NameIs(final String[] parts) {
	// return new PathEvaluator.Adapter<Integer>() {
	//
	// @Override
	// public Evaluation evaluate(Path path, BranchState<Integer> state) {
	// if (state.getState() == null) {
	// state.setState(0);
	// }
	// // System.out.println(path.endNode().getLabels());
	// if (path.endNode().hasLabel(DynamicLabel.label(GenericTag.class.getSimpleName()))) {
	// // System.out.println("is Tag");
	// Iterable<Relationship> i18NRelationships = path.endNode().getRelationships(BasicRelationships.TYPES.HAS_I18N_PROPERTIES,
	// Direction.OUTGOING);
	// for (Relationship i18NRelationship : i18NRelationships) {
	// Node i18NPropertiesNode = i18NRelationship.getEndNode();
	// if (parts[state.getState()].equals(i18NPropertiesNode.getProperty("properties-name"))) {
	// log.debug("Found matching i18n name for given node. Continuing traversal.");
	// return Evaluation.INCLUDE_AND_CONTINUE;
	// } else {
	// return Evaluation.EXCLUDE_AND_CONTINUE;
	// }
	// // for (String key : i18NPropertiesNode.getPropertyKeys()) {
	// // System.out.println(key);
	// // }
	// }
	// return Evaluation.EXCLUDE_AND_CONTINUE;
	// }
	//
	// if (path.endNode().hasLabel(DynamicLabel.label(GenericFile.class.getSimpleName()))) {
	// System.out.println("check file");
	// return Evaluation.INCLUDE_AND_CONTINUE;
	// }
	//
	// // Only traverse tags and files
	// return Evaluation.EXCLUDE_AND_CONTINUE;
	// }
	// };
	// }

	// /**
	// * Add a handler for removing a tag with a specific name from a page.
	// */
	// private void addUntagPageHandler() {
	//
	// route("/:uuid/tags/:name").method(DELETE).handler(rh -> {
	// String uuid = rh.request().params().get("uuid");
	// String name = rh.request().params().get("name");
	// rh.response().end(toJson(new GenericResponse<Tag>(contentRepository.untag(uuid, name))));
	// });
	// }

	// /**
	// * Return the specific tag of a page.
	// */
	// private void addGetTagHandler() {
	//
	// route("/:uuid/tags/:name").method(GET).handler(rh -> {
	// String uuid = rh.request().params().get("uuid");
	// String name = rh.request().params().get("name");
	// rh.response().end(toJson(new GenericResponse<Tag>(contentRepository.getTag(uuid, name))));
	// });
	//
	// }

	// /**
	// * Add a tag to the page with id
	// */
	// private void addAddTagHandler() {
	//
	// route("/:uuid/tags/:name").method(PUT).handler(rh -> {
	// String uuid = rh.request().params().get("uuid");
	// String name = String.valueOf(rh.request().params().get("name"));
	// Tag tag = contentRepository.tagGenericContent(uuid, name);
	// rh.response().end(toJson(new GenericResponse<Tag>(tag)));
	//
	// });
	// }
	
		// Tagging
		// addAddTagHandler();
		// addUntagPageHandler();
		// addGetTagHandler();