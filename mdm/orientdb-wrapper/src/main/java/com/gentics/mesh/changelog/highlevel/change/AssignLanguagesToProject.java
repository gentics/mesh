package com.gentics.mesh.changelog.highlevel.change;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.changelog.highlevel.AbstractHighLevelChange;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.dao.PersistingLanguageDao;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.GraphDatabase;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class AssignLanguagesToProject extends AbstractHighLevelChange {

	private static final Logger log = LoggerFactory.getLogger(RestructureWebrootIndex.class);

	private final GraphDatabase db;

	@Inject
	public AssignLanguagesToProject(GraphDatabase db) {
		this.db = db;
	}

	@Override
	public boolean isAllowedInCluster(MeshOptions options) {
		return false;
	}

	@Override
	public String getUuid() {
		return "FEBB52AA891D4540B2F4BD3853639F62";
	}

	@Override
	public String getName() {
		return "Assign languages to project";
	}

	@Override
	public String getDescription() {
		return "Checks the languages existing in the project content, and assigns them to the project";
	}

	@Override
	public void apply() {
		CommonTx ctx = CommonTx.get();
		PersistingLanguageDao languageDao = ctx.languageDao();
		Map<HibProject, Set<String>> projectLangs = ctx.projectDao().findAll().stream()
			.map(project -> Pair.of(project, ctx.nodeDao().findUsedLanguages(project, languageDao.findAll().stream().map(HibLanguage::getLanguageTag).collect(Collectors.toSet()), false)))
			.collect(Collectors.toMap(Pair::getKey, Pair::getValue));

		projectLangs.entrySet().stream()
			.flatMap(projectLang -> projectLang.getValue().stream().map(langTag -> {
				HibLanguage language = languageDao.findByLanguageTag(langTag);
				if (language == null) {
					throw new IllegalStateException("No Language [" + langTag + "] is installed!");
				}
				return Pair.of(projectLang.getKey(), language);
			})).forEach(projectLang -> languageDao.assign(projectLang.getValue(), projectLang.getKey(), ctx.batch(), false));
	}
}
