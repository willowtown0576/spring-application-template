package dev.template.application.feature.internal.infrastructure;

import static dev.template.application.jooq.feature.tables.Feature.FEATURE_;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.template.application.Application;
import dev.template.application.common.Result;
import dev.template.application.feature.api.authorization.FeatureAuthority;
import dev.template.application.feature.api.command.CreateFailure;
import dev.template.application.feature.api.command.CreateFeatureParam;
import dev.template.application.feature.api.command.FeatureCommands;
import dev.template.application.feature.api.query.FeatureQueries;
import dev.template.application.feature.api.query.FeatureResult;
import dev.template.application.feature.api.query.FindFeatureParam;
import dev.template.application.feature.internal.domain.Feature;
import dev.template.application.feature.internal.query.FeatureDataSource;
import dev.template.application.feature.internal.usecase.FeatureRepository;
import dev.template.application.security.OperationsAuthority;
import dev.template.application.security.SystemActor;
import dev.template.application.security.SystemExecution;
import dev.template.application.security.WithTestUser;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springdoc.webmvc.ui.SwaggerWelcomeWebMvc;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** 実PostgreSQLでmigration、永続化、rollback、Method Security、HTTP契約を検証する。 */
@WithTestUser({FeatureAuthority.WRITE, FeatureAuthority.READ})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@Testcontainers
@SpringBootTest(classes = Application.class, useMainMethod = SpringBootTest.UseMainMethod.ALWAYS, properties = {
    "spring.docker.compose.enabled=false", "spring.batch.job.enabled=false", "springdoc.api-docs.enabled=false",
    "springdoc.swagger-ui.enabled=false"})
@Transactional
@Tag("integration")
@MockitoSpyBean(types = {FeatureRepository.class, FeatureDataSource.class})
class DatabaseIntegrationTest {

    private static final UUID ID = UUID.fromString("0199417c-0000-7000-8000-000000000001");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(System.getProperty("test.postgres.image"));

    private final ApplicationContext context;
    private final DSLContext sql;
    private final Flyway flyway;
    private final MockMvc mvc;
    private final FeatureCommands commands;
    private final FeatureQueries queries;
    private final FeatureRepository repository;
    private final FeatureDataSource dataSource;

    /**
     * テスト対象のapplication依存を受け取る。
     *
     * @param context 検証に使用するApplicationContext
     * @param sql 検証に使用するDSLContext
     * @param flyway 検証に使用するFlyway
     * @param mvc 検証に使用するMockMvc
     * @param commands 検証に使用するFeatureCommands
     * @param queries 検証に使用するFeatureQueries
     * @param repository 書き込みのspy
     * @param dataSource 読み取りのspy
     */
    @Autowired
    DatabaseIntegrationTest(final ApplicationContext context, final DSLContext sql, final Flyway flyway,
        final MockMvc mvc, final FeatureCommands commands, final FeatureQueries queries,
        final FeatureRepository repository, final FeatureDataSource dataSource) {
        this.context = context;
        this.sql = sql;
        this.flyway = flyway;
        this.mvc = mvc;
        this.commands = commands;
        this.queries = queries;
        this.repository = repository;
        this.dataSource = dataSource;
    }

    /** Command/QueryのParam自体または検索IDがnullの場合、NullPointerExceptionになることを検証する。 */
    @Test
    void rejectsMissingPublicApiParameters() {
        assertThatThrownBy(() -> commands.create(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> queries.find(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new FindFeatureParam(null)).isInstanceOf(NullPointerException.class);
    }

    /** 空のPostgreSQLでapplicationを起動し、共通Flyway履歴、migration、schemaとCOMMENTを検証する。 */
    @Test
    void startsApplicationAndValidatesSharedMigrationHistory() {
        assertThat(context.getBean(Application.class)).isNotNull();

        // 適用済み履歴の整合性と、再実行時に追加適用がないことを確認する。
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(
            sql.fetchSingle("select count(*) from public.flyway_schema_history where success").get(0, Integer.class))
            .isEqualTo(flyway.info().applied().length);
        assertThat(sql.fetchSingle("select obj_description('feature.feature'::regclass)").get(0, String.class))
            .isNotBlank();
    }

    /** 最大長のUnicode名をgenerated jOOQ型で保存・取得し、保存した値と取得した値が一致することを検証する。 */
    @Test
    void generatedTypesRoundTripMaximumLengthUnicodeName() {
        final String name = "名".repeat(100);
        sql.insertInto(FEATURE_).set(FEATURE_.ID, ID).set(FEATURE_.NAME, name).execute();
        assertThat(sql.selectFrom(FEATURE_).where(FEATURE_.ID.eq(ID)).fetchSingle().getName()).isEqualTo(name);
    }

    /**
     * null・空文字・半角空白の名前を直接DBへ書き込み、DB制約が拒否することを検証する。
     *
     * @param name 検証する名前
     */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rejectsMissingOrBlankName(final String name) {
        assertThatThrownBy(() -> sql.insertInto(FEATURE_).set(FEATURE_.ID, ID).set(FEATURE_.NAME, name).execute())
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** 100 code pointを超える名前を指定し、DBの文字数制約によって保存が拒否されることを検証する。 */
    @Test
    void rejectsNameOverDatabaseLimit() {
        assertThatThrownBy(
            () -> sql.insertInto(FEATURE_).set(FEATURE_.ID, ID).set(FEATURE_.NAME, "a".repeat(101)).execute())
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** 同じIDを二度挿入し、主キー制約が重複を拒否することを検証する。 */
    @Test
    void rejectsDuplicateId() {
        sql.insertInto(FEATURE_).set(FEATURE_.ID, ID).set(FEATURE_.NAME, "first").execute();
        assertThatThrownBy(() -> sql.insertInto(FEATURE_).set(FEATURE_.ID, ID).set(FEATURE_.NAME, "second").execute())
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** ID未指定の挿入をDBの必須制約が拒否することを検証する。 */
    @Test
    void requiresApplicationAssignedId() {
        assertThatThrownBy(() -> sql.insertInto(FEATURE_).set(FEATURE_.NAME, "missing ID").execute())
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** 正しい名前をCommandで作成し、実commit後にQueryで同じIDと名前を取得できることを検証する。 */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void commitsCreatedFeatureAndReadsThroughPublicApi() {
        final var result = commands.create(new CreateFeatureParam("😀".repeat(100)));
        final UUID id = switch (result) {
            case Result.Success<UUID, CreateFailure>(var value) -> value;
            case Result.Failure<UUID, CreateFailure>(var reason) -> throw new AssertionError(reason);
        };
        try {
            assertThat(id.version()).isEqualTo(7);
            assertThat(queries.find(new FindFeatureParam(id))).contains(new FeatureResult(id, "😀".repeat(100)));
            assertThat(queries.find(new FindFeatureParam(ID))).isEmpty();
        } finally {
            sql.deleteFrom(FEATURE_).where(FEATURE_.ID.eq(id)).execute();
        }
    }

    /**
     * 不正な名前をCommandへ渡し、INVALID_NAMEを返してRepositoryを呼ばないことを検証する。
     *
     * @param name 検証する名前
     */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\u0000", "\uD800"})
    void returnsTypedFailureWithoutWriting(final String name) {
        assertThat(commands.create(new CreateFeatureParam(name)))
            .isEqualTo(new Result.Failure<>(CreateFailure.INVALID_NAME));
        verifyNoInteractions(repository);
    }

    /** insert直後にRepositoryが例外を送出した場合、transactionがrollbackされ行が残らないことを検証する。 */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollsBackWhenRepositoryFailsAfterInsert() {
        final var insertedId = new AtomicReference<UUID>();
        final var failure = new DataAccessResourceFailureException("simulated failure after write");
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            final Feature feature = invocation.getArgument(0);
            insertedId.set(feature.id());
            invocation.callRealMethod();
            throw failure;
        }).when(repository).insert(ArgumentMatchers.any(Feature.class));
        try {
            assertThatThrownBy(() -> context.getBean(SystemExecution.class).call(SystemActor.BATCH,
                () -> commands.create(new CreateFeatureParam("rollback")))).isSameAs(failure);
            assertThat(insertedId.get()).isNotNull();
            assertThat(sql.fetchCount(FEATURE_, FEATURE_.ID.eq(insertedId.get()))).isZero();
        } finally {
            if (insertedId.get() != null) {
                sql.deleteFrom(FEATURE_).where(FEATURE_.ID.eq(insertedId.get())).execute();
            }
        }
    }

    /** Query経由でDataSourceを呼び、SpringとPostgreSQLの両方でread-only transactionが有効なことを検証する。 */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void queryRunsInReadOnlyTransaction() {
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
            assertThat(sql.fetchSingle("show transaction_read_only").get(0, String.class)).isEqualTo("on");
            return invocation.callRealMethod();
        }).when(dataSource).find(ID);
        assertThat(queries.find(new FindFeatureParam(ID))).isEmpty();
    }

    /**
     * 権限とCSRF tokenを持つPOSTで201・Locationを取得し、GETの名前とIDが一致することを検証する。
     *
     * @throws Exception HTTP操作またはテスト環境の処理に失敗した場合
     */
    @Test
    void restCreatesAndFindsFeature() throws Exception {

        // 作成応答のLocationを、そのまま取得要求に使う。
        final var response = mvc
            .perform(post("/api/v1/features").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"REST name\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isString()).andReturn().getResponse();
        final String location = Objects.requireNonNull(response.getHeader("Location"));
        assertThat(location).startsWith("http://localhost/api/v1/features/");
        mvc.perform(get(location).with(user("reader").authorities(FeatureAuthority.READ))).andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(location.substring(location.lastIndexOf('/') + 1)))
            .andExpect(jsonPath("$.name").value("REST name"));
    }

    /**
     * 未認証の作成・取得requestに対し、401とProblemDetailが返ることを検証する。
     *
     * @throws Exception HTTP操作またはテスト環境の処理に失敗した場合
     */
    @Test
    void unauthenticatedRequestsReturnProblemDetails() throws Exception {
        mvc.perform(get("/api/v1/features/" + ID).with(anonymous())).andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401));
        mvc.perform(post("/api/v1/features").with(anonymous()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"denied\"}")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
        verifyNoInteractions(repository);
    }

    /**
     * 必要なREAD/WRITE権限のない利用者がAPIを呼ぶと403となり、操作できないことを検証する。
     *
     * @throws Exception HTTP操作またはテスト環境の処理に失敗した場合
     */
    @Test
    void insufficientAuthorityCannotWriteOrRead() throws Exception {
        mvc.perform(post("/api/v1/features").with(csrf()).with(user("reader").authorities(FeatureAuthority.READ))
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"denied\"}")).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
        mvc.perform(get("/api/v1/features/" + ID).with(user("writer").authorities(FeatureAuthority.WRITE)))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        verifyNoInteractions(repository);
    }

    /**
     * WRITE権限があってもCSRF tokenのないPOSTが403となることを検証する。
     *
     * @throws Exception HTTP操作またはテスト環境の処理に失敗した場合
     */
    @Test
    void csrfIsRequiredEvenWithWriteAuthority() throws Exception {
        mvc.perform(post("/api/v1/features").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"denied\"}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(post("/api/v1/features").with(csrf().useInvalidToken())
            .with(user("writer").authorities(FeatureAuthority.WRITE)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"denied\"}")).andExpect(status().isForbidden());
        verifyNoInteractions(repository);
    }

    /**
     * 欠落または不正なJSONを送信し、400のProblemDetailが返ることを検証する。
     *
     * @param body 送信するJSON本文
     * @throws Exception HTTP操作またはテスト環境の処理に失敗した場合
     */
    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":null}", "{broken", "null"})
    void malformedOrMissingInputIsBadRequest(final String body) throws Exception {
        mvc.perform(post("/api/v1/features").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(repository);
    }

    /**
     * 形式上正しいJSONの名前が業務制約に違反すると、422のProblemDetailが返ることを検証する。
     *
     * @throws Exception HTTP操作またはテスト環境の処理に失敗した場合
     */
    @Test
    void businessFailureBecomesUnprocessableContent() throws Exception {
        mvc.perform(
            post("/api/v1/features").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \"}"))
            .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.status").value(422));
        verifyNoInteractions(repository);
    }

    /**
     * 未登録ID、不正UUID、未対応versionを指定し、それぞれ標準の404/400応答になることを検証する。
     *
     * @throws Exception HTTP操作またはテスト環境の処理に失敗した場合
     */
    @Test
    void missingResourceInvalidIdAndUnsupportedVersionUseStandardErrors() throws Exception {
        mvc.perform(get("/api/v1/features/" + ID)).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
        mvc.perform(get("/api/v1/features/not-a-uuid").with(user("reader").authorities(FeatureAuthority.READ)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/not-a-version/features/" + ID).with(user("reader").authorities(FeatureAuthority.READ)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    /** READ権限だけでCommandを直接呼び、Method Securityが作成を拒否することを検証する。 */
    @Test
    @WithTestUser(FeatureAuthority.READ)
    void directCommandInvocationAlsoEnforcesAuthorization() {
        assertThatThrownBy(() -> commands.create(new CreateFeatureParam("denied")))
            .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository);
    }

    /** WRITE権限だけでQueryを直接呼び、Method Securityが取得を拒否することを検証する。 */
    @Test
    @WithTestUser(FeatureAuthority.WRITE)
    void directQueryInvocationAlsoEnforcesAuthorization() {
        assertThatThrownBy(() -> queries.find(new FindFeatureParam(ID))).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(dataSource);
    }

    /** 設定利用者のpasswordがencodeされ、sample操作の権限を持つことを検証する。 */
    @Test
    void baselineRegistersConfiguredUser() {
        final var user = context.getBean(UserDetailsService.class).loadUserByUsername("test-user");
        assertThat(user.getPassword()).startsWith("{bcrypt}").doesNotContain("test-password");
        assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority).contains("feature:read",
            "feature:write");
    }

    /**
     * フォーム認証のsessionでRESTを実行し、CSRF保護とlogoutによる失効を検証する。
     * @throws Exception HTTP操作に失敗した場合
     */
    @Test
    @WithAnonymousUser
    void loginSessionAuthenticatesRestAndLogoutInvalidatesIt() throws Exception {
        TestSecurityContextHolder.clearContext();
        mvc.perform(post("/login").with(csrf()).param("username", "test-user").param("password", "wrong-password"))
            .andExpect(status().is3xxRedirection()).andExpect(MockMvcResultMatchers.redirectedUrl("/login?error"));

        // 正しいcredentialでsessionを確立する。
        final var response = mvc
            .perform(
                post("/login").with(csrf()).param("username", "test-user").param("password", "test-password-12345"))
            .andExpect(status().is3xxRedirection()).andReturn();
        final var session = (MockHttpSession) response.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/api/v1/features/" + ID).session(session)).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/features").session(session).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"authenticated-rest\"}")).andExpect(status().isForbidden());

        // CSRF tokenがある場合だけ更新できることを確認する。
        mvc.perform(post("/api/v1/features").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"authenticated-rest\"}")).andExpect(status().isCreated());
        mvc.perform(get("/actuator/metrics").session(session)).andExpect(status().isForbidden());

        // logoutでsessionを失効させる。
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/v1/features/" + ID)).andExpect(status().isUnauthorized());
    }

    /**
     * DB書き込み後の技術障害に対し、HTTP応答が詳細を伏せた500となり保存内容がrollbackされることを検証する。
     *
     * @throws Exception HTTP操作またはテスト環境の処理に失敗した場合
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void unexpectedRestFailureIsSanitizedAndRolledBack() throws Exception {
        final var insertedId = new AtomicReference<UUID>();
        doAnswer(invocation -> {
            final Feature feature = invocation.getArgument(0);
            insertedId.set(feature.id());
            invocation.callRealMethod();
            throw new DataAccessResourceFailureException("private database implementation detail");
        }).when(repository).insert(ArgumentMatchers.any(Feature.class));
        try {
            mvc.perform(post("/api/v1/features").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"rollback\"}")).andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
            assertThat(insertedId.get()).isNotNull();
            assertThat(sql.fetchCount(FEATURE_, FEATURE_.ID.eq(insertedId.get()))).isZero();
        } finally {
            if (insertedId.get() != null) {
                sql.deleteFrom(FEATURE_).where(FEATURE_.ID.eq(insertedId.get())).execute();
            }
        }
    }

    /**
     * healthの匿名公開、運用権限を持つmetrics/info参照、未公開endpointの保護を検証する。
     *
     * @throws Exception HTTP操作またはテスト環境の処理に失敗した場合
     */
    @Test
    void actuatorRequiresOperationsAuthorityAndExposesPoolMetrics() throws Exception {
        mvc.perform(get("/actuator/health").with(anonymous())).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP")).andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/metrics").with(anonymous())).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/metrics").with(user("user"))).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/metrics/hikaricp.connections.max")
            .with(user("operator").authorities(OperationsAuthority.READ))).andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("hikaricp.connections.max"));
        mvc.perform(get("/actuator/info").with(user("operator").authorities(OperationsAuthority.READ)))
            .andExpect(status().isOk());
        mvc.perform(get("/actuator/env").with(user("operator").authorities(OperationsAuthority.READ)))
            .andExpect(status().isForbidden());
    }

    /**
     * テストJobを明示実行し、起動時の自動Job実行がなく、実行状態がPostgreSQLへ保存されることを検証する。
     *
     * @throws Exception HTTP操作またはテスト環境の処理に失敗した場合
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void batchUsesDurableMetadataWithoutStartupJobs() throws Exception {
        final var jobs = context.getBean(JobRepository.class);
        final var operator = context.getBean(JobOperator.class);
        final var transactions = context.getBean(PlatformTransactionManager.class);

        // worker内でシステム主体を適用するJobを組み立てる。
        final var createdId = new AtomicReference<UUID>();
        final var system = context.getBean(SystemExecution.class);
        final var step = new StepBuilder("metadata-check", jobs)
            .tasklet((contribution, chunk) -> system.call(SystemActor.BATCH, () -> {
                final var created = (Result.Success<UUID, CreateFailure>) commands
                    .create(new CreateFeatureParam("batch-created"));
                createdId.set(created.value());
                assertThat(queries.find(new FindFeatureParam(created.value()))).isPresent();
                return RepeatStatus.FINISHED;
            }), transactions).build();
        final var job = new JobBuilder("metadata-check", jobs).listener(context.getBean(JobExecutionListener.class))
            .start(step).build();

        // 別スレッドでもsystem主体の適用前後にSecurityContextが残らないことを確認する。
        final JobExecution execution;
        try (var worker = Executors.newSingleThreadExecutor()) {
            execution = worker.submit(() -> {
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
                final var result = operator.start(job, new JobParametersBuilder().toJobParameters());
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
                return result;
            }).get();
        }
        try {
            assertThat(queries.find(new FindFeatureParam(createdId.get()))).isPresent();
        } finally {
            sql.deleteFrom(FEATURE_).where(FEATURE_.ID.eq(createdId.get())).execute();
        }

        // この実行IDに対応する履歴と再実行拒否を検証する。
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobs.getJobExecution(execution.getId()).getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(sql.fetchSingle(
            "select count(*) from system.batch_job_execution where job_execution_id = ? and status='COMPLETED'",
            execution.getId()).get(0, Integer.class)).isEqualTo(1);
        assertThatThrownBy(() -> operator.start(job, execution.getJobParameters()))
            .isInstanceOf(JobInstanceAlreadyCompleteException.class);
    }

    /**
     * 標準schedulerへ処理を登録し、指定した処理が実行されることを検証する。
     *
     * @throws Exception HTTP操作またはテスト環境の処理に失敗した場合
     */
    @Test
    void schedulerExecutesSubmittedTask() throws Exception {
        final var scheduler = context.getBean(TaskScheduler.class);
        final var ran = new CountDownLatch(1);
        scheduler.schedule(ran::countDown, context.getBean(Clock.class).instant());
        assertThat(ran.await(5, TimeUnit.SECONDS)).isTrue();
    }

    /**
     * scheduler workerで参照を許可し、更新と主体なしの呼び出しを拒否し、終了後contextが空であることを検証する。
     * @throws Exception worker処理が失敗した場合
     */
    @Test
    void schedulerCallsPublicApiWithReadOnlyAuthority() throws Exception {
        final var result = new CompletableFuture<Void>();
        final var system = context.getBean(SystemExecution.class);
        context.getBean(TaskScheduler.class).schedule(() -> {
            try {
                assertThatThrownBy(() -> queries.find(new FindFeatureParam(ID)))
                    .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
                system.call(SystemActor.SCHEDULER, () -> {
                    assertThat(queries.find(new FindFeatureParam(ID))).isEmpty();
                    assertThatThrownBy(() -> commands.create(new CreateFeatureParam("scheduler-denied")))
                        .isInstanceOf(AccessDeniedException.class);
                    return null;
                });
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
                result.complete(null);
            } catch (final Throwable failure) {
                result.completeExceptionally(failure);
            }
        }, context.getBean(Clock.class).instant());
        result.get(10, TimeUnit.SECONDS);
        verifyNoInteractions(repository);
    }

    /**
     * Command実行ログに処理結果と所要時間が記録され、引数と戻り値の内容が出力されないことを検証する。
     *
     * @param output テスト中のログ出力
     */
    @Test
    void operationLogsDoNotDumpArgumentsOrResults(final CapturedOutput output) {
        final var result = (Result.Success<UUID, CreateFailure>) commands
            .create(new CreateFeatureParam("private-person-secret-token"));
        assertThat(queries.find(new FindFeatureParam(result.value()))).isPresent();
        assertThat(commands.create(new CreateFeatureParam(" "))).isInstanceOf(Result.Failure.class);
        assertThat(output.getAll()).contains("Operation start", "Operation success", "Operation failure")
            .doesNotContain("private-person-secret-token", result.value().toString())
            .containsPattern("Operation success operation=.+ durationMs=[0-9]+");
    }

    /**
     * API資料を無効にした構成で、OpenAPI endpointが登録されないことを検証する。
     *
     * @throws Exception HTTP操作またはテスト環境の処理に失敗した場合
     */
    @Test
    void apiDocumentationCanBeDisabled() throws Exception {
        assertThat(context.getBeansOfType(OpenApiWebMvcResource.class)).isEmpty();
        assertThat(context.getBeansOfType(SwaggerWelcomeWebMvc.class)).isEmpty();
        assertThat(context.getEnvironment().getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
    }
}
