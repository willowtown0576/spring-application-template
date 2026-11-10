package dev.template.application.feature.internal.infrastructure;

import static dev.template.application.jooq.feature.tables.Feature.FEATURE_;
import static dev.template.application.jooq.feature.tables.RegistrationRequest.REGISTRATION_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.template.application.Application;
import dev.template.application.common.Result;
import dev.template.application.feature.api.authorization.FeatureAuthority;
import dev.template.application.feature.api.command.CreateFeatureParam;
import dev.template.application.feature.api.command.CreateFeaturesParam;
import dev.template.application.feature.api.command.FeatureCommandFailure;
import dev.template.application.feature.api.command.FeatureCommands;
import dev.template.application.feature.api.command.UpdateFeatureParam;
import dev.template.application.feature.api.command.UpdateFeaturesParam;
import dev.template.application.feature.api.model.FeatureStatus;
import dev.template.application.feature.api.query.FeatureDetails;
import dev.template.application.feature.api.query.FeatureQueries;
import dev.template.application.feature.api.query.FeatureSortField;
import dev.template.application.feature.api.query.FindFeatureByIdParam;
import dev.template.application.feature.api.query.SearchFeaturesParam;
import dev.template.application.feature.internal.usecase.FeatureRepository;
import dev.template.application.security.WithTestUser;
import dev.template.application.web.FeatureSearchCursorCodec;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
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
import org.springframework.http.MediaType;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** 実DBで検索境界、一括操作、再送、競合とHTTP契約を検証する。 */
@WithTestUser({FeatureAuthority.WRITE, FeatureAuthority.READ})
@AutoConfigureMockMvc
@Testcontainers
@SpringBootTest(classes = Application.class, properties = {"spring.docker.compose.enabled=false",
    "spring.batch.job.enabled=false"})
@Tag("integration")
@Transactional
@MockitoSpyBean(types = FeatureRepository.class)
class FeatureOperationsIntegrationTest {
    /** class専用のPostgreSQL。 */
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(System.getProperty("test.postgres.image"));
    /** 更新API。 */
    private final FeatureCommands commands;
    /** カーソル変換。 */
    private final FeatureSearchCursorCodec cursors;
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
     * @param cursors カーソル
     * @param repository 書込Port
     */
    @Autowired
    FeatureOperationsIntegrationTest(final FeatureCommands commands, final FeatureQueries queries, final DSLContext sql,
        final MockMvc mvc, final FeatureSearchCursorCodec cursors, final FeatureRepository repository) {
        this.commands = commands;
        this.queries = queries;
        this.sql = sql;
        this.mvc = mvc;
        this.cursors = cursors;
        this.repository = repository;
    }

    /** 同時再送は同じ結果になり、同じversionへの競合更新は一つだけ成功する。 */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentReplayAndUpdateAreSerialized() throws Exception {
        final var input = new CreateFeaturesParam("concurrent", UUID.randomUUID(),
            List.of(new CreateFeatureParam("concurrent")));
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var gate = new CountDownLatch(1);
            final Callable<List<UUID>> operation = () -> {
                assertThat(gate.await(10, TimeUnit.SECONDS)).isTrue();
                return created(input);
            };
            final var first = executor.submit(new DelegatingSecurityContextCallable<>(operation));
            final var second = executor.submit(new DelegatingSecurityContextCallable<>(operation));
            gate.countDown();
            final List<UUID> ids = first.get(15, TimeUnit.SECONDS);
            assertThat(second.get(15, TimeUnit.SECONDS)).isEqualTo(ids);
            try {
                final var one = executor.submit(new DelegatingSecurityContextCallable<>(() -> commands
                    .update(new UpdateFeatureParam(ids.getFirst(), "winner-a", FeatureStatus.ACTIVE, 1))));
                final var two = executor.submit(new DelegatingSecurityContextCallable<>(() -> commands
                    .update(new UpdateFeatureParam(ids.getFirst(), "winner-b", FeatureStatus.ACTIVE, 1))));
                final List<Result<UUID, FeatureCommandFailure>> results = List.of(one.get(15, TimeUnit.SECONDS),
                    two.get(15, TimeUnit.SECONDS));
                assertThat(results.stream().filter(Result.Success.class::isInstance).count()).isEqualTo(1);
                assertThat(results.stream().filter(Result.Failure.class::isInstance).count()).isEqualTo(1);
            } finally {
                sql.deleteFrom(REGISTRATION_REQUEST).where(REGISTRATION_REQUEST.REQUEST_ID.eq(input.requestId()))
                    .execute();
                sql.deleteFrom(FEATURE_).where(FEATURE_.ID.in(ids)).execute();
            }
        }
    }

    /** 境界行が削除されても保存した値から次ページを取得できる。 */
    @Test
    void deletedBoundaryDoesNotInvalidateSeek() {
        sql.deleteFrom(FEATURE_).execute();
        created(new CreateFeaturesParam("actor", UUID.randomUUID(),
            List.of(new CreateFeatureParam("a"), new CreateFeatureParam("b"), new CreateFeatureParam("c"))));
        final var first = queries
            .search(new SearchFeaturesParam("", null, FeatureSortField.NAME, false, 1, null, false));
        sql.deleteFrom(FEATURE_).where(FEATURE_.ID.eq(first.next().id())).execute();
        final var second = queries
            .search(new SearchFeaturesParam("", null, FeatureSortField.NAME, false, 1, first.next(), false));
        assertThat(second.items()).extracting(FeatureDetails::name).containsExactly("b");
    }

    /** 同値のソートキーでも全方向の前後移動に欠落と重複がなく、検索文字をliteralとして扱う。 */
    @Test
    void keysetPagesRoundTripWithTiesAndLiteralFilters() {
        sql.deleteFrom(FEATURE_).execute();
        created(new CreateFeaturesParam("actor", UUID.randomUUID(),
            List.of(new CreateFeatureParam("Alpha%_"), new CreateFeatureParam("alpha%_"),
                new CreateFeatureParam("alpha%_"), new CreateFeatureParam("alphaXX"),
                new CreateFeatureParam("other", FeatureStatus.INACTIVE))));
        for (final FeatureSortField sort : FeatureSortField.values()) {
            for (final boolean descending : new boolean[]{true, false}) {
                final var firstParam = new SearchFeaturesParam(" ALPHA%_ ", FeatureStatus.ACTIVE, sort, descending, 2,
                    null, false);
                final var first = queries.search(firstParam);
                assertThat(first.items()).hasSize(2);
                assertThat(first.previous()).isNull();
                final var second = queries.search(
                    new SearchFeaturesParam("alpha%_", FeatureStatus.ACTIVE, sort, descending, 2, first.next(), false));
                assertThat(second.items()).hasSize(1);
                assertThat(second.next()).isNull();
                final var back = queries.search(new SearchFeaturesParam("alpha%_", FeatureStatus.ACTIVE, sort,
                    descending, 2, second.previous(), true));
                assertThat(back.items()).isEqualTo(first.items());
                assertThat(second.items()).doesNotContainAnyElementsOf(first.items());
                final String token = cursors.encode(first.next(), firstParam);
                assertThat(cursors.search("ALPHA%_", FeatureStatus.ACTIVE, sort, descending, 2, token, false).cursor())
                    .isEqualTo(first.next());
                assertThatThrownBy(
                    () -> cursors.search("changed", FeatureStatus.ACTIVE, sort, descending, 2, token, false))
                    .isInstanceOf(ResponseStatusException.class);
            }
        }
        assertThat(queries
            .search(new SearchFeaturesParam("", FeatureStatus.INACTIVE, FeatureSortField.NAME, false, 20, null, false))
            .items()).extracting(FeatureDetails::name).containsExactly("other");
    }

    /** 登録日時、初期version、再送と操作キーの内容不一致を検証する。 */
    @Test
    void registrationIsAtomicAndReplayable() {
        final var input = new CreateFeaturesParam("actor", UUID.randomUUID(),
            List.of(new CreateFeatureParam("one"), new CreateFeatureParam("two", FeatureStatus.INACTIVE)));
        final List<UUID> ids = created(input);
        assertThat(created(input)).isEqualTo(ids);
        assertThat(sql.fetchCount(FEATURE_, FEATURE_.ID.in(ids))).isEqualTo(2);
        assertThat(sql.selectFrom(FEATURE_).where(FEATURE_.ID.in(ids)).fetch()).allSatisfy(row -> {
            assertThat(row.getVersion()).isEqualTo(1);
            assertThat(row.getCreatedAt().toInstant()).isEqualTo(row.getUpdatedAt().toInstant());
        });
        assertThat(queries.findById(new FindFeatureByIdParam(ids.get(1))).orElseThrow().status())
            .isEqualTo(FeatureStatus.INACTIVE);
        assertThat(commands.createMany(
            new CreateFeaturesParam("actor", input.requestId(), List.of(new CreateFeatureParam("changed")))))
            .isInstanceOfSatisfying(Result.Failure.class,
                failure -> assertThat(((FeatureCommandFailure) failure.reason()).issues())
                    .extracting(FeatureCommandFailure.Issue::reason)
                    .containsExactly(FeatureCommandFailure.Reason.REPLAY_MISMATCH));
        final var invalid = new CreateFeaturesParam("actor", UUID.randomUUID(),
            List.of(new CreateFeatureParam("valid"), new CreateFeatureParam(" ")));
        assertThat(commands.createMany(invalid)).isInstanceOf(Result.Failure.class);
        assertThat(sql.fetchCount(REGISTRATION_REQUEST, REGISTRATION_REQUEST.REQUEST_ID.eq(invalid.requestId())))
            .isZero();
    }

    /** HTTP境界の入力、再送、不正cursor、認可とCSRFを検証する。 */
    @Test
    void restContractsRejectStaleAndMalformedRequests() throws Exception {
        final String request = "{\"requestId\":\"" + UUID.randomUUID()
            + "\",\"rows\":[{\"name\":\"rest-bulk\",\"status\":\"ACTIVE\"}]}";
        final String body = mvc
            .perform(
                post("/api/v1/features/bulk").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/v1/features/bulk").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk()).andExpect(content().string(body));
        mvc.perform(post("/api/v1/features/bulk").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(request.replace("rest-bulk", "changed"))).andExpect(status().isConflict())
            .andExpect(jsonPath("$.issues[0].reason").value("REPLAY_MISMATCH"));
        final UUID id = UUID.fromString(body.substring(2, body.length() - 2));
        mvc.perform(put("/api/v1/features/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"updated\",\"status\":\"INACTIVE\",\"version\":1}")).andExpect(status().isOk());
        mvc.perform(put("/api/v1/features/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"stale\",\"status\":\"ACTIVE\",\"version\":1}")).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/features").param("cursor", "invalid")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/features").param("size", "101")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/features/bulk").contentType(MediaType.APPLICATION_JSON).content("[]"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/features/bulk").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("[]"))
            .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.issues[0].row").value(0));
    }

    /** 単件と複数で同じ項目検証と行番号付き失敗を返す。 */
    @Test
    void singleAndMultipleCommandsShareValidationFailures() throws Exception {
        final var invalidCreate = new CreateFeatureParam(" ", null);
        assertThat(commands.create(invalidCreate)).isEqualTo(
            commands.createMany(new CreateFeaturesParam("actor", UUID.randomUUID(), List.of(invalidCreate))));
        final var invalidUpdate = new UpdateFeatureParam(null, " ", null, 0);
        assertThat(commands.update(invalidUpdate))
            .isEqualTo(commands.updateMany(new UpdateFeaturesParam(List.of(invalidUpdate))));
        mvc.perform(
            post("/api/v1/features").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
            .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.issues[0].row").value(1))
            .andExpect(jsonPath("$.issues[0].field").value("name"))
            .andExpect(jsonPath("$.issues[0].reason").value("INVALID_NAME"));
    }

    /** 保存途中の更新例外が先行行の更新も取り消す。 */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void technicalFailureRollsBackAllUpdates() {
        final UUID key = UUID.randomUUID();
        final List<UUID> ids = created(new CreateFeaturesParam("update-rollback", key,
            List.of(new CreateFeatureParam("before-one"), new CreateFeatureParam("before-two"))));
        final var before = ids.stream().map(id -> queries.findById(new FindFeatureByIdParam(id)).orElseThrow())
            .toList();
        final var calls = new AtomicInteger();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            if (calls.incrementAndGet() == 2) {
                throw new IllegalStateException("injected update failure");
            }
            return null;
        }).when(repository).update(any(), anyLong());
        try {
            assertThatThrownBy(() -> commands.updateMany(new UpdateFeaturesParam(
                ids.stream().map(id -> new UpdateFeatureParam(id, "after", FeatureStatus.INACTIVE, 1)).toList())))
                .isInstanceOf(IllegalStateException.class);
            assertThat(ids.stream().map(id -> queries.findById(new FindFeatureByIdParam(id)).orElseThrow()).toList())
                .isEqualTo(before);
        } finally {
            sql.deleteFrom(REGISTRATION_REQUEST).where(REGISTRATION_REQUEST.REQUEST_ID.eq(key)).execute();
            sql.deleteFrom(FEATURE_).where(FEATURE_.ID.in(ids)).execute();
        }
    }

    /** 保存途中の例外で行と再送記録を共にrollbackする。 */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void technicalFailureRollsBackRegistrationAndLedger() {
        final UUID requestId = UUID.randomUUID();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("injected failure");
        }).when(repository).completeCreation(eq("rollback"), eq(requestId), anyList());
        assertThatThrownBy(() -> created(
            new CreateFeaturesParam("rollback", requestId, List.of(new CreateFeatureParam("rollback-marker")))))
            .isInstanceOf(IllegalStateException.class);
        assertThat(sql.fetchCount(FEATURE_, FEATURE_.NAME.eq("rollback-marker"))).isZero();
        assertThat(sql.fetchCount(REGISTRATION_REQUEST, REGISTRATION_REQUEST.REQUEST_ID.eq(requestId))).isZero();
    }

    /** 異なる値の一括更新、変更なし、競合による全件拒否と登録日時維持を検証する。 */
    @Test
    void updatesAllRowsOrNoneAndLeavesNoOpsUnchanged() {
        final List<UUID> ids = created(new CreateFeaturesParam("actor", UUID.randomUUID(),
            List.of(new CreateFeatureParam("a"), new CreateFeatureParam("b"))));
        final var original = sql.selectFrom(FEATURE_).where(FEATURE_.ID.eq(ids.getFirst())).fetchSingle();
        final var changes = List.of(new UpdateFeatureParam(ids.get(0), "first", FeatureStatus.ACTIVE, 1),
            new UpdateFeatureParam(ids.get(1), "second", FeatureStatus.INACTIVE, 1));
        assertThat(commands.updateMany(new UpdateFeaturesParam(changes))).isEqualTo(new Result.Success<>(ids));
        final FeatureDetails first = queries.findById(new FindFeatureByIdParam(ids.getFirst())).orElseThrow();
        final FeatureDetails second = queries.findById(new FindFeatureByIdParam(ids.getLast())).orElseThrow();
        assertThat(first.version()).isEqualTo(2);
        assertThat(second.updatedAt()).isEqualTo(first.updatedAt());
        assertThat(sql.select(FEATURE_.CREATED_AT).from(FEATURE_).where(FEATURE_.ID.eq(ids.getFirst()))
            .fetchSingle(FEATURE_.CREATED_AT)).isEqualTo(original.getCreatedAt());
        assertThat(commands.update(new UpdateFeatureParam(first.id(), first.name(), first.status(), 2)))
            .isInstanceOf(Result.Success.class);
        assertThat(queries.findById(new FindFeatureByIdParam(first.id()))).contains(first);
        assertThat(commands.updateMany(
            new UpdateFeaturesParam(List.of(new UpdateFeatureParam(first.id(), "not saved", FeatureStatus.ACTIVE, 2),
                new UpdateFeatureParam(second.id(), "stale", FeatureStatus.ACTIVE, 1)))))
            .isInstanceOf(Result.Failure.class);
        assertThat(queries.findById(new FindFeatureByIdParam(first.id()))).contains(first);
        assertThat(commands.updateMany(new UpdateFeaturesParam(List.of(changes.getFirst(), changes.getFirst()))))
            .isInstanceOf(Result.Failure.class);
        assertThat(commands.update(new UpdateFeatureParam(UUID.randomUUID(), "absent", FeatureStatus.ACTIVE, 1)))
            .isInstanceOf(Result.Failure.class);
    }

    /** 件数上限とnull項目を検査し、入力行番号を保って拒否する。 */
    @Test
    void validatesBatchBoundariesAndNullRowsBeforeWriting() {
        final UUID key = UUID.randomUUID();
        assertThat(commands.createMany(new CreateFeaturesParam("actor", key, List.of())))
            .isInstanceOf(Result.Failure.class);
        final var maximum = IntStream.range(0, 100).mapToObj(index -> new CreateFeatureParam("row-" + index)).toList();
        assertThat(created(new CreateFeaturesParam("actor", key, maximum))).hasSize(100);
        final var excessive = IntStream.range(0, 101).mapToObj(index -> new CreateFeatureParam("row-" + index))
            .toList();
        assertThat(commands.createMany(new CreateFeaturesParam("actor", UUID.randomUUID(), excessive)))
            .isInstanceOf(Result.Failure.class);
        final var invalid = new CreateFeaturesParam("actor", UUID.randomUUID(),
            Arrays.asList(new CreateFeatureParam("valid"), null));
        assertThat(commands.createMany(invalid)).isInstanceOfSatisfying(Result.Failure.class,
            failure -> assertThat(((FeatureCommandFailure) failure.reason()).issues())
                .allSatisfy(issue -> assertThat(issue.row()).isEqualTo(2)));
        assertThat(commands.updateMany(new UpdateFeaturesParam(List.of(new UpdateFeatureParam(null, "name", null, 0)))))
            .isInstanceOf(Result.Failure.class);
    }

    /**
     * 登録成功からIDを取り出す。
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
