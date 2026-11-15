package dev.template.application.feature.internal.infrastructure;

import static dev.template.application.jooq.feature.tables.Feature.FEATURE_;
import static dev.template.application.jooq.feature.tables.FeatureHistory.FEATURE_HISTORY;
import static dev.template.application.jooq.feature.tables.RegistrationRequest.REGISTRATION_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.template.application.Application;
import dev.template.application.common.Result;
import dev.template.application.feature.api.authorization.FeatureAuthority;
import dev.template.application.feature.api.command.CreateFeatureParam;
import dev.template.application.feature.api.command.CreateFeaturesParam;
import dev.template.application.feature.api.command.DeleteFeatureParam;
import dev.template.application.feature.api.command.DeleteFeaturesParam;
import dev.template.application.feature.api.command.FeatureCommandFailure;
import dev.template.application.feature.api.command.FeatureCommands;
import dev.template.application.feature.api.command.FeatureDeletionTarget;
import dev.template.application.feature.api.command.UpdateFeatureParam;
import dev.template.application.feature.api.model.FeatureStatus;
import dev.template.application.feature.api.query.FeatureQueries;
import dev.template.application.feature.api.query.FindFeatureByIdParam;
import dev.template.application.feature.internal.usecase.FeatureRepository;
import dev.template.application.security.WithTestUser;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** 実DBで削除時の履歴内容・全件rollback・同時更新・HTTP認可を検証する。 */
@WithTestUser({FeatureAuthority.WRITE, FeatureAuthority.READ})
@AutoConfigureMockMvc
@Testcontainers
@SpringBootTest(classes = Application.class, properties = {"spring.docker.compose.enabled=false",
    "spring.batch.job.enabled=false"})
@Tag("integration")
@Transactional
@MockitoSpyBean(types = FeatureRepository.class)
class FeatureDeletionIntegrationTest {
    /** class専用のPostgreSQL。 */
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(System.getProperty("test.postgres.image"));
    /** 更新API。 */
    private final FeatureCommands commands;
    /** HTTP検証。 */
    private final MockMvc mvc;
    /** 参照API。 */
    private final FeatureQueries queries;
    /** 例外を注入する書込Port。 */
    private final FeatureRepository repository;
    /** DB直接検証。 */
    private final DSLContext sql;

    /**
     * 検証対象を受け取る。
     * @param commands 更新API
     * @param queries 参照API
     * @param sql DB
     * @param mvc HTTP
     * @param repository 書込Port
     */
    @Autowired
    FeatureDeletionIntegrationTest(final FeatureCommands commands, final FeatureQueries queries, final DSLContext sql,
        final MockMvc mvc, final FeatureRepository repository) {
        this.commands = commands;
        this.queries = queries;
        this.sql = sql;
        this.mvc = mvc;
        this.repository = repository;
    }

    /**
     * 初期versionの削除対象を作る。
     * @param ids 対象ID
     * @return 入力順の対象
     */
    private static List<FeatureDeletionTarget> targets(final List<UUID> ids) {
        return ids.stream().map(id -> new FeatureDeletionTarget(id, 1)).toList();
    }

    /** 更新と削除が同じversionへ同時に到達しても、一つだけ成功する。 */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentUpdateAndDeleteCannotBothSucceed() throws Exception {
        final var input = new CreateFeaturesParam("actor", UUID.randomUUID(), List.of(new CreateFeatureParam("race")));
        final List<UUID> ids = created(input);
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var gate = new CountDownLatch(1);
            final var deletion = executor.submit(new DelegatingSecurityContextCallable<>(() -> {
                assertThat(gate.await(10, TimeUnit.SECONDS)).isTrue();
                return commands.delete(new DeleteFeatureParam("actor", targets(ids).getFirst()));
            }));
            final var update = executor.submit(new DelegatingSecurityContextCallable<>(() -> {
                assertThat(gate.await(10, TimeUnit.SECONDS)).isTrue();
                return commands.update(new UpdateFeatureParam(ids.getFirst(), "revised", FeatureStatus.ACTIVE, 1));
            }));
            gate.countDown();
            final var results = List.of(deletion.get(15, TimeUnit.SECONDS), update.get(15, TimeUnit.SECONDS));
            assertThat(results.stream().filter(Result.Success.class::isInstance).count()).isEqualTo(1);
            assertThat(results.stream().filter(Result.Failure.class::isInstance).count()).isEqualTo(1);
            final var active = queries.findById(new FindFeatureByIdParam(ids.getFirst()));
            if (active.isPresent()) {
                assertThat(active.orElseThrow().version()).isEqualTo(2);
                assertThat(sql.fetchCount(FEATURE_HISTORY, FEATURE_HISTORY.ID.in(ids))).isZero();
            } else {
                assertThat(sql.selectFrom(FEATURE_HISTORY).where(FEATURE_HISTORY.ID.in(ids)).fetchSingle().getVersion())
                    .isEqualTo(1);
            }
        } finally {
            cleanup(input.requestId(), ids);
        }
    }

    /** 元データを全カラム保存し、同じ削除日時と主体で一覧から取り除く。登録再送は復活させない。 */
    @Test
    void movesExactSnapshotAndKeepsCreationReplayStable() {
        final var input = new CreateFeaturesParam("actor", UUID.randomUUID(),
            List.of(new CreateFeatureParam("history-a"), new CreateFeatureParam("history-b", FeatureStatus.INACTIVE)));
        final List<UUID> ids = created(input);
        assertThat(
            commands.update(new UpdateFeatureParam(ids.getFirst(), "history-revised", FeatureStatus.INACTIVE, 1)))
            .isInstanceOf(Result.Success.class);
        final var confirmed = ids.stream().map(id -> new FeatureDeletionTarget(id, id.equals(ids.getFirst()) ? 2 : 1))
            .toList();
        final var before = sql.selectFrom(FEATURE_).where(FEATURE_.ID.in(ids)).orderBy(FEATURE_.ID).fetch();
        assertThat(commands.deleteMany(new DeleteFeaturesParam("deleter", confirmed)))
            .isEqualTo(new Result.Success<>(ids));
        assertThat(sql.fetchCount(FEATURE_, FEATURE_.ID.in(ids))).isZero();
        final var history = sql.selectFrom(FEATURE_HISTORY).where(FEATURE_HISTORY.ID.in(ids))
            .orderBy(FEATURE_HISTORY.ID).fetch();
        assertThat(history).hasSize(2);
        for (int index = 0; index < before.size(); index++) {
            assertThat(history.get(index).getId()).isEqualTo(before.get(index).getId());
            assertThat(history.get(index).getName()).isEqualTo(before.get(index).getName());
            assertThat(history.get(index).getStatus()).isEqualTo(before.get(index).getStatus());
            assertThat(history.get(index).getCreatedAt()).isEqualTo(before.get(index).getCreatedAt());
            assertThat(history.get(index).getUpdatedAt()).isEqualTo(before.get(index).getUpdatedAt());
            assertThat(history.get(index).getVersion()).isEqualTo(before.get(index).getVersion());
            assertThat(history.get(index).getDeletedBy()).isEqualTo("deleter");
            assertThat(history.get(index).getDeletedAt()).isAfterOrEqualTo(history.get(index).getUpdatedAt());
        }
        assertThat(history.getFirst().getDeletedAt()).isEqualTo(history.getLast().getDeletedAt());
        assertThat(created(input)).isEqualTo(ids);
        assertThat(queries.findById(new FindFeatureByIdParam(ids.getFirst()))).isEmpty();
        assertThat(commands.delete(new DeleteFeatureParam("deleter", targets(ids).getFirst())))
            .isInstanceOf(Result.Failure.class);
        assertThat(sql.fetchCount(FEATURE_HISTORY, FEATURE_HISTORY.ID.in(ids))).isEqualTo(2);
    }

    /** 一件の競合または未検出があれば、他の対象も履歴へ移動しない。 */
    @Test
    void rejectsAllTargetsBeforeWritingWhenStaleOrMissing() {
        final List<UUID> ids = created(new CreateFeaturesParam("actor", UUID.randomUUID(),
            List.of(new CreateFeatureParam("one"), new CreateFeatureParam("two"))));
        commands.update(new UpdateFeatureParam(ids.getLast(), "changed", FeatureStatus.ACTIVE, 1));
        assertThat(commands.deleteMany(new DeleteFeaturesParam("actor", targets(ids)))).isInstanceOfSatisfying(
            Result.Failure.class, failure -> assertThat(((FeatureCommandFailure) failure.reason()).issues())
                .containsExactly(new FeatureCommandFailure.Issue(2, "version", FeatureCommandFailure.Reason.CONFLICT)));
        assertThat(commands.deleteMany(new DeleteFeaturesParam("actor",
            List.of(targets(ids).getFirst(), new FeatureDeletionTarget(UUID.randomUUID(), 1)))))
            .isInstanceOf(Result.Failure.class);
        assertThat(sql.fetchCount(FEATURE_, FEATURE_.ID.in(ids))).isEqualTo(2);
        assertThat(sql.fetchCount(FEATURE_HISTORY, FEATURE_HISTORY.ID.in(ids))).isZero();
    }

    /** HTTPでversion、認可、CSRF、削除後404と主体の偽装拒否を検証する。 */
    @Test
    void restDeletionUsesAuthenticatedActorAndRequiresWriteAuthority() throws Exception {
        final List<UUID> ids = created(
            new CreateFeaturesParam("actor", UUID.randomUUID(), List.of(new CreateFeatureParam("rest-one"),
                new CreateFeatureParam("rest-two"), new CreateFeatureParam("rest-three"))));
        final String path = "/api/v1/features/" + ids.getFirst();
        mvc.perform(delete(path).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(delete(path).param("version", "0").with(csrf())).andExpect(status().isUnprocessableContent());
        mvc.perform(delete(path).param("version", "1")).andExpect(status().isForbidden());
        mvc.perform(delete(path).param("version", "1").with(csrf()).with(anonymous()))
            .andExpect(status().isUnauthorized());
        mvc.perform(
            delete(path).param("version", "1").with(csrf()).with(user("reader").authorities(FeatureAuthority.READ)))
            .andExpect(status().isForbidden());
        mvc.perform(delete(path).param("version", "1").with(csrf())).andExpect(status().isOk())
            .andExpect(jsonPath("$").value(ids.getFirst().toString()));
        mvc.perform(get(path)).andExpect(status().isNotFound());
        mvc.perform(delete(path).param("version", "1").with(csrf())).andExpect(status().isNotFound());
        final String body = "{\"actor\":\"forged\",\"targets\":[{\"id\":\"" + ids.get(1)
            + "\",\"version\":1},{\"id\":\"" + ids.get(2) + "\",\"version\":1}]}";
        mvc.perform(
            post("/api/v1/features/bulk-delete").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        assertThat(sql.selectFrom(FEATURE_HISTORY).where(FEATURE_HISTORY.ID.in(ids)).fetch())
            .allSatisfy(row -> assertThat(row.getDeletedBy()).isEqualTo("test-user"));
    }

    /** 履歴INSERT後に実DELETEがFK制約で失敗しても、履歴は残らず元データを維持する。 */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollsBackArchiveWhenDatabaseRefusesDelete() {
        final var input = new CreateFeaturesParam("actor", UUID.randomUUID(),
            List.of(new CreateFeatureParam("guarded")));
        final List<UUID> ids = created(input);
        sql.execute("create table feature.deletion_guard (feature_id uuid references feature.feature(id))");
        try {
            sql.execute("insert into feature.deletion_guard values (?)", ids.getFirst());
            assertThatThrownBy(() -> commands.delete(new DeleteFeatureParam("actor", targets(ids).getFirst())))
                .isInstanceOf(DataIntegrityViolationException.class);
            assertThat(sql.fetchCount(FEATURE_, FEATURE_.ID.in(ids))).isEqualTo(1);
            assertThat(sql.fetchCount(FEATURE_HISTORY, FEATURE_HISTORY.ID.in(ids))).isZero();
        } finally {
            sql.execute("drop table feature.deletion_guard");
            cleanup(input.requestId(), ids);
        }
    }

    /** 二行目の保存後の障害でも、全行の履歴と元データをrollbackする。 */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollsBackHistoryAndDeletionAfterPartialWrites() {
        final var input = new CreateFeaturesParam("actor", UUID.randomUUID(),
            List.of(new CreateFeatureParam("rollback-one"), new CreateFeatureParam("rollback-two")));
        final List<UUID> ids = created(input);
        final var calls = new AtomicInteger();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            if (calls.incrementAndGet() == 2) {
                throw new IllegalStateException("injected deletion failure");
            }
            return null;
        }).when(repository).archiveAndDelete(any(), any(), any());
        try {
            assertThatThrownBy(() -> commands.deleteMany(new DeleteFeaturesParam("actor", targets(ids))))
                .isInstanceOf(IllegalStateException.class);
            assertThat(sql.fetchCount(FEATURE_, FEATURE_.ID.in(ids))).isEqualTo(2);
            assertThat(sql.fetchCount(FEATURE_HISTORY, FEATURE_HISTORY.ID.in(ids))).isZero();
        } finally {
            cleanup(input.requestId(), ids);
        }
    }

    /** 不正対象・主体・重複・件数を拒否し、100件の上限で履歴退避できる。 */
    @Test
    void validatesDeletionInputsAndUpperBound() {
        assertThat(commands.deleteMany(new DeleteFeaturesParam("actor", List.of()))).isInstanceOf(Result.Failure.class);
        assertThat(commands.delete(new DeleteFeatureParam("", new FeatureDeletionTarget(UUID.randomUUID(), 1))))
            .isInstanceOf(Result.Failure.class);
        assertThat(commands.delete(new DeleteFeatureParam("actor", null))).isInstanceOf(Result.Failure.class);
        final var target = new FeatureDeletionTarget(UUID.randomUUID(), 0);
        assertThat(commands.deleteMany(new DeleteFeaturesParam("actor", List.of(target, target))))
            .isInstanceOf(Result.Failure.class);
        assertThat(commands.deleteMany(new DeleteFeaturesParam("actor",
            IntStream.range(0, 101).mapToObj(index -> new FeatureDeletionTarget(UUID.randomUUID(), 1)).toList())))
            .isInstanceOf(Result.Failure.class);
        final List<UUID> ids = created(new CreateFeaturesParam("actor", UUID.randomUUID(),
            IntStream.range(0, 100).mapToObj(index -> new CreateFeatureParam("delete-" + index)).toList()));
        assertThat(commands.deleteMany(new DeleteFeaturesParam("actor", targets(ids))))
            .isEqualTo(new Result.Success<>(ids));
        assertThat(sql.fetchCount(FEATURE_HISTORY, FEATURE_HISTORY.ID.in(ids))).isEqualTo(100);
    }

    /**
     * 非transaction testがcommitした専用のデータを取り除く。
     * @param requestId 登録キー
     * @param ids 登録ID
     */
    private void cleanup(final UUID requestId, final List<UUID> ids) {
        sql.deleteFrom(FEATURE_HISTORY).where(FEATURE_HISTORY.ID.in(ids)).execute();
        sql.deleteFrom(FEATURE_).where(FEATURE_.ID.in(ids)).execute();
        sql.deleteFrom(REGISTRATION_REQUEST).where(REGISTRATION_REQUEST.REQUEST_ID.eq(requestId)).execute();
    }

    /**
     * 登録成功のIDを得る。
     * @param input 登録入力
     * @return 登録ID
     */
    private List<UUID> created(final CreateFeaturesParam input) {
        return switch (commands.createMany(input)) {
            case Result.Success(var ids) -> ids;
            case Result.Failure(var failure) -> throw new AssertionError(failure);
        };
    }
}
