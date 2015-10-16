package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LANGUAGE;

import java.util.Stack;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public class LanguageRootImpl extends AbstractRootVertex<Language>implements LanguageRoot {

	public static void checkIndices(Database database) {
		database.addEdgeIndex(HAS_LANGUAGE);
	}

	@Override
	protected Class<LanguageImpl> getPersistanceClass() {
		return LanguageImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_LANGUAGE;
	}

	@Override
	public void addLanguage(Language language) {
		addItem(language);
	}

	// TODO add unique index

	@Override
	public Language create(String languageName, String languageTag) {
		LanguageImpl language = getGraph().addFramedVertex(LanguageImpl.class);
		language.setName(languageName);
		language.setLanguageTag(languageTag);
		addLanguage(language);
		return language;
	}

	@Override
	public void create(InternalActionContext rc, Handler<AsyncResult<Language>> handler) {
		throw new NotImplementedException("Languages can be created using REST");
	}

	/**
	 * Find the language with the specified http://en.wikipedia.org/wiki/IETF_language_tag[IETF language tag].
	 * 
	 * @param languageTag
	 * @return Found language or null if none could be found
	 */
	@Override
	public Language findByLanguageTag(String languageTag) {
		return out(HAS_LANGUAGE).has(LanguageImpl.class).has("languageTag", languageTag).nextOrDefaultExplicit(LanguageImpl.class, null);
	}

	/**
	 * The tag language is currently fixed to english since we only want to store tags based on a single language. The idea is that tags will be localizable in
	 * the future.
	 */
	@Override
	public Language getTagDefaultLanguage() {
		return findByLanguageTag(TagImpl.DEFAULT_TAG_LANGUAGE_TAG);
	}

	@Override
	public void resolveToElement(Stack<String> stack, Handler<AsyncResult<? extends MeshVertex>> resultHandler) {
		throw new NotImplementedException();
	}

	@Override
	public void delete() {
		throw new NotImplementedException("The language root should never be deleted.");
	}

}
