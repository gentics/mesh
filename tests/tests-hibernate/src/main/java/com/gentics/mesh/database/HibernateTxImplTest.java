package com.gentics.mesh.database;

import static com.gentics.mesh.test.TestSize.PROJECT;

import jakarta.persistence.PersistenceException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = PROJECT, startServer = true, inMemoryDB = true)
public class HibernateTxImplTest extends AbstractMeshTest {

	@Test
	public void nestedTransactionCommitting() {
		tx((tx) -> {
			tx.userDao().create("aaa", null);
			tx((tx2) -> {
				tx.userDao().create("bbb", null);
				tx((tx3) -> {
					tx.userDao().create("ccc", null);
					tx3.commit();
				});
			});
		});

		tx((tx) -> {
			Assertions.assertThat(tx.userDao().findByUsername("aaa")).isNotNull();
			Assertions.assertThat(tx.userDao().findByUsername("bbb")).isNotNull();
			Assertions.assertThat(tx.userDao().findByUsername("ccc")).isNotNull();
		});
	}

	@Test
	public void nestedTransactionAborting() {
		Assertions.assertThatThrownBy(() -> {
			tx((tx) -> {
				tx.languageDao().create("lang1", "langTag1");
				tx((tx2) -> {
					tx2.languageDao().create("lang2", "langTag2");
					tx((tx3) -> {
						tx3.languageDao().create("lang3", "langTag3");
						tx3.languageDao().create("lang3", "langTag3"); // throws
					});
					tx2.commit();
				});
			});
		}).isInstanceOf(PersistenceException.class);

		tx((tx) -> {
			Assertions.assertThat(tx.languageDao().findByLanguageTag("langTag1")).isNull();
			Assertions.assertThat(tx.languageDao().findByLanguageTag("langTag2")).isNull();
			Assertions.assertThat(tx.languageDao().findByLanguageTag("langTag3")).isNull();
		});
	}

	@Test
	public void nestedTransactionFailingConstraint() {
		Assertions.assertThatThrownBy(() -> {
			tx((tx) -> {
				tx.languageDao().create("lang1", "langTag1");
				tx((tx2) -> {
					tx2.languageDao().create("lang2", "langTag2");
					tx((tx3) -> {
						tx3.languageDao().create("lang2", "langTag2");
					});
				});
			});
		}).isInstanceOf(PersistenceException.class);

		tx((tx) -> {
			Assertions.assertThat(tx.languageDao().findByLanguageTag("langTag1")).isNull();
			Assertions.assertThat(tx.languageDao().findByLanguageTag("langTag2")).isNull();
			Assertions.assertThat(tx.languageDao().findByLanguageTag("langTag3")).isNull();
		});
	}

	@Test
	public void nestedTransactionCommittingParentRollingBack() {
		tx((tx) -> {
			tx.languageDao().create("lang1", "langTag1");
			tx((tx2) -> {
				tx2.languageDao().create("lang2", "langTag2");
				tx((tx3) -> {
					tx3.languageDao().create("lang3", "langTag3");
					tx3.commit();
				});
			});
			tx.rollback();
		});

		tx((tx) -> {
			Assertions.assertThat(tx.languageDao().findByLanguageTag("langTag1")).isNull();
			Assertions.assertThat(tx.languageDao().findByLanguageTag("langTag2")).isNull();
			Assertions.assertThat(tx.languageDao().findByLanguageTag("langTag3")).isNull();
		});
	}
}