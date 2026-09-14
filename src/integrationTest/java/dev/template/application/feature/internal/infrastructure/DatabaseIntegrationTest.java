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
import dev.template.application.feature.api.command.CreateFailure;
import dev.template.application.feature.api.command.FeatureCommands;
import dev.template.application.feature.api.command.Result;
import dev.template.application.feature.api.query.FeatureQueries;
import dev.template.application.feature.api.query.FeatureView;
import dev.template.application.feature.internal.domain.Feature;
import dev.template.application.feature.internal.query.FeatureDataSource;
import dev.template.application.feature.internal.usecase.FeatureRepository;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@org.springframework.security.test.context.support.WithMockUser(
    authorities = {FeatureCommands.WRITE, FeatureQueries.READ})
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@org.junit.jupiter.api.extension.ExtendWith(
    org.springframework.boot.test.system.OutputCaptureExtension.class)
@Testcontainers
@SpringBootTest(
    classes = Application.class,
    useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
    properties = "spring.docker.compose.enabled=false")
@Transactional
class DatabaseIntegrationTest {

  private static final UUID ID = UUID.fromString("0199417c-0000-7000-8000-000000000001");

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(System.getProperty("test.postgres.image"));

  @Autowired private ApplicationContext context;
  @Autowired private DSLContext sql;
  @Autowired private Flyway flyway;
  @Autowired private MockMvc mvc;
  @Autowired private FeatureCommands commands;
  @Autowired private FeatureQueries queries;
  @MockitoSpyBean private FeatureRepository repository;
  @MockitoSpyBean private FeatureDataSource dataSource;

  @Test
  void startsApplicationAndValidatesSharedMigrationHistory() {
    assertThat(context.getBean(Application.class)).isNotNull();
    assertThat(context.getEnvironment().getProperty("spring.application.name"))
        .isEqualTo("spring-application-starter");
    flyway.validate();
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    assertThat(
            sql.fetchSingle("select count(*) from public.flyway_schema_history where success")
                .get(0, Integer.class))
        .isEqualTo(2);
    assertThat(
            sql.fetchSingle("select obj_description('feature.feature'::regclass)")
                .get(0, String.class))
        .isNotBlank();
  }

  @Test
  void generatedTypesRoundTripMaximumLengthUnicodeName() {
    String name = "名".repeat(100);
    sql.insertInto(FEATURE_).set(FEATURE_.ID, ID).set(FEATURE_.NAME, name).execute();
    assertThat(sql.selectFrom(FEATURE_).where(FEATURE_.ID.eq(ID)).fetchSingle().getName())
        .isEqualTo(name);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   "})
  void rejectsMissingOrBlankName(String name) {
    assertThatThrownBy(
            () -> sql.insertInto(FEATURE_).set(FEATURE_.ID, ID).set(FEATURE_.NAME, name).execute())
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsNameOverDatabaseLimit() {
    assertThatThrownBy(
            () ->
                sql.insertInto(FEATURE_)
                    .set(FEATURE_.ID, ID)
                    .set(FEATURE_.NAME, "a".repeat(101))
                    .execute())
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsDuplicateId() {
    sql.insertInto(FEATURE_).set(FEATURE_.ID, ID).set(FEATURE_.NAME, "first").execute();
    assertThatThrownBy(
            () ->
                sql.insertInto(FEATURE_)
                    .set(FEATURE_.ID, ID)
                    .set(FEATURE_.NAME, "second")
                    .execute())
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void requiresApplicationAssignedId() {
    assertThatThrownBy(() -> sql.insertInto(FEATURE_).set(FEATURE_.NAME, "missing ID").execute())
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void commitsCreatedFeatureAndReadsThroughPublicApi() {
    var result = commands.create("😀".repeat(100));
    UUID id =
        switch (result) {
          case Result.Success<UUID, CreateFailure>(var value) -> value;
          case Result.Failure<UUID, CreateFailure>(var reason) -> throw new AssertionError(reason);
        };
    try {
      assertThat(id.version()).isEqualTo(7);
      assertThat(queries.find(id)).contains(new FeatureView(id, "😀".repeat(100)));
      assertThat(queries.find(ID)).isEmpty();
    } finally {
      sql.deleteFrom(FEATURE_).where(FEATURE_.ID.eq(id)).execute();
    }
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   ", "\u0000", "\uD800"})
  void returnsTypedFailureWithoutWriting(String name) {
    assertThat(commands.create(name)).isEqualTo(new Result.Failure<>(CreateFailure.INVALID_NAME));
    verifyNoInteractions(repository);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void rollsBackWhenRepositoryFailsAfterInsert() {
    var insertedId = new AtomicReference<UUID>();
    var failure = new DataAccessResourceFailureException("simulated failure after write");
    doAnswer(
            invocation -> {
              assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
              Feature feature = invocation.getArgument(0);
              insertedId.set(feature.id());
              invocation.callRealMethod();
              throw failure;
            })
        .when(repository)
        .insert(org.mockito.ArgumentMatchers.any(Feature.class));
    try {
      assertThatThrownBy(() -> commands.create("rollback")).isSameAs(failure);
      assertThat(insertedId.get()).isNotNull();
      assertThat(sql.fetchCount(FEATURE_, FEATURE_.ID.eq(insertedId.get()))).isZero();
    } finally {
      if (insertedId.get() != null) {
        sql.deleteFrom(FEATURE_).where(FEATURE_.ID.eq(insertedId.get())).execute();
      }
    }
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void queryRunsInReadOnlyTransaction() {
    doAnswer(
            invocation -> {
              assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
              assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
              assertThat(sql.fetchSingle("show transaction_read_only").get(0, String.class))
                  .isEqualTo("on");
              return invocation.callRealMethod();
            })
        .when(dataSource)
        .find(ID);
    assertThat(queries.find(ID)).isEmpty();
  }

  @Test
  void restCreatesAndFindsFeature() throws Exception {
    var response =
        mvc.perform(
                post("/api/v1/features")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"REST name\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isString())
            .andReturn()
            .getResponse();
    String location = java.util.Objects.requireNonNull(response.getHeader("Location"));
    assertThat(location).startsWith("/api/v1/features/");
    mvc.perform(
            get(location)
                .with(user("reader").authorities(new SimpleGrantedAuthority(FeatureQueries.READ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(location.substring(location.lastIndexOf('/') + 1)))
        .andExpect(jsonPath("$.name").value("REST name"));
  }

  @Test
  void unauthenticatedRequestsReturnProblemDetails() throws Exception {
    mvc.perform(get("/api/v1/features/" + ID).with(anonymous()))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(401));
    mvc.perform(
            post("/api/v1/features")
                .with(anonymous())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"denied\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
    verifyNoInteractions(repository);
  }

  @Test
  void insufficientAuthorityCannotWriteOrRead() throws Exception {
    mvc.perform(
            post("/api/v1/features")
                .with(csrf())
                .with(user("reader").authorities(new SimpleGrantedAuthority(FeatureQueries.READ)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"denied\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
    mvc.perform(
            get("/api/v1/features/" + ID)
                .with(
                    user("writer").authorities(new SimpleGrantedAuthority(FeatureCommands.WRITE))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
    verifyNoInteractions(repository);
  }

  @Test
  void csrfIsRequiredEvenWithWriteAuthority() throws Exception {
    mvc.perform(
            post("/api/v1/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"denied\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
    mvc.perform(
            post("/api/v1/features")
                .with(csrf().useInvalidToken())
                .with(user("writer").authorities(new SimpleGrantedAuthority(FeatureCommands.WRITE)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"denied\"}"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(repository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"name\":null}", "{broken", "null"})
  void malformedOrMissingInputIsBadRequest(String body) throws Exception {
    mvc.perform(
            post("/api/v1/features")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400));
    verifyNoInteractions(repository);
  }

  @Test
  void businessFailureBecomesUnprocessableContent() throws Exception {
    mvc.perform(
            post("/api/v1/features")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"   \"}"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.status").value(422));
    verifyNoInteractions(repository);
  }

  @Test
  void missingResourceInvalidIdAndUnsupportedVersionUseStandardErrors() throws Exception {
    mvc.perform(get("/api/v1/features/" + ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
    mvc.perform(
            get("/api/v1/features/not-a-uuid")
                .with(user("reader").authorities(new SimpleGrantedAuthority(FeatureQueries.READ))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
    mvc.perform(
            get("/api/v2/features/" + ID)
                .with(user("reader").authorities(new SimpleGrantedAuthority(FeatureQueries.READ))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  @WithMockUser(authorities = FeatureQueries.READ)
  void directCommandInvocationAlsoEnforcesAuthorization() {
    assertThatThrownBy(() -> commands.create("denied")).isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(repository);
  }

  @Test
  @WithMockUser(authorities = FeatureCommands.WRITE)
  void directQueryInvocationAlsoEnforcesAuthorization() {
    assertThatThrownBy(() -> queries.find(ID)).isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(dataSource);
  }

  @Test
  void baselineDoesNotCreateDefaultUsers() {
    assertThat(
            context.getBeansOfType(
                org.springframework.security.core.userdetails.UserDetailsService.class))
        .isEmpty();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void unexpectedRestFailureIsSanitizedAndRolledBack() throws Exception {
    var insertedId = new AtomicReference<UUID>();
    doAnswer(
            invocation -> {
              Feature feature = invocation.getArgument(0);
              insertedId.set(feature.id());
              invocation.callRealMethod();
              throw new DataAccessResourceFailureException(
                  "private database implementation detail");
            })
        .when(repository)
        .insert(org.mockito.ArgumentMatchers.any(Feature.class));
    try {
      mvc.perform(
              post("/api/v1/features")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\":\"rollback\"}"))
          .andExpect(status().isInternalServerError())
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

  @Test
  void actuatorRequiresOperationsAuthorityAndExposesPoolMetrics() throws Exception {
    mvc.perform(get("/actuator/health").with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components").doesNotExist());
    mvc.perform(get("/actuator/metrics").with(anonymous())).andExpect(status().isUnauthorized());
    mvc.perform(get("/actuator/metrics").with(user("user"))).andExpect(status().isForbidden());
    mvc.perform(
            get("/actuator/metrics/hikaricp.connections.max")
                .with(user("operator").authorities(new SimpleGrantedAuthority("ops:read"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("hikaricp.connections.max"));
    mvc.perform(
            get("/actuator/info")
                .with(user("operator").authorities(new SimpleGrantedAuthority("ops:read"))))
        .andExpect(status().isOk());
    mvc.perform(
            get("/actuator/env")
                .with(user("operator").authorities(new SimpleGrantedAuthority("ops:read"))))
        .andExpect(status().isForbidden());
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void batchUsesDurableMetadataWithoutStartupJobs() throws Exception {
    var jobs = context.getBean(org.springframework.batch.core.repository.JobRepository.class);
    var operator = context.getBean(org.springframework.batch.core.launch.JobOperator.class);
    var transactions =
        context.getBean(org.springframework.transaction.PlatformTransactionManager.class);
    assertThat(context.getBeansOfType(org.springframework.batch.core.job.Job.class)).isEmpty();
    assertThat(context.getEnvironment().getProperty("spring.batch.job.enabled")).isEqualTo("false");
    assertThat(jobs.getJobNames()).isEmpty();
    var step =
        new org.springframework.batch.core.step.builder.StepBuilder("metadata-check", jobs)
            .tasklet(
                (contribution, chunk) ->
                    org.springframework.batch.infrastructure.repeat.RepeatStatus.FINISHED,
                transactions)
            .build();
    var job =
        new org.springframework.batch.core.job.builder.JobBuilder("metadata-check", jobs)
            .listener(
                context.getBean(org.springframework.batch.core.listener.JobExecutionListener.class))
            .start(step)
            .build();
    var execution =
        operator.start(
            job,
            new org.springframework.batch.core.job.parameters.JobParametersBuilder()
                .toJobParameters());
    assertThat(execution.getStatus())
        .isEqualTo(org.springframework.batch.core.BatchStatus.COMPLETED);
    assertThat(jobs.getJobExecution(execution.getId()).getStatus())
        .isEqualTo(org.springframework.batch.core.BatchStatus.COMPLETED);
    assertThat(
            sql.fetchSingle(
                    "select count(*) from system.batch_job_execution where status='COMPLETED'")
                .get(0, Integer.class))
        .isEqualTo(1);
    assertThatThrownBy(() -> operator.start(job, execution.getJobParameters()))
        .isInstanceOf(
            org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException.class);
  }

  @Test
  void schedulingAndGracefulShutdownUseBootDefaults() throws Exception {
    var scheduler = context.getBean(org.springframework.scheduling.TaskScheduler.class);
    var ran = new java.util.concurrent.CountDownLatch(1);
    scheduler.schedule(ran::countDown, java.time.Instant.now());
    assertThat(ran.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    assertThat(
            context
                .getBean(
                    org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor
                        .class)
                .getScheduledTasks())
        .isEmpty();
    assertThat(context.getEnvironment().getProperty("server.shutdown")).isEqualTo("graceful");
    assertThat(context.getEnvironment().getProperty("spring.lifecycle.timeout-per-shutdown-phase"))
        .isEqualTo("30s");
  }

  @Test
  void operationLogsDoNotDumpArgumentsOrResults(
      org.springframework.boot.test.system.CapturedOutput output) {
    var result =
        (Result.Success<UUID, CreateFailure>) commands.create("private-person-secret-token");
    assertThat(queries.find(result.value())).isPresent();
    assertThat(commands.create(" ")).isInstanceOf(Result.Failure.class);
    assertThat(output.getAll())
        .contains("Operation start", "Operation success", "Operation failure")
        .doesNotContain("private-person-secret-token", result.value().toString())
        .containsPattern("Operation success operation=.+ durationMs=[0-9]+");
  }

  @Test
  void apiDocumentationIsDisabledByDefault() throws Exception {
    assertThat(context.getBeansOfType(org.springdoc.webmvc.api.OpenApiWebMvcResource.class))
        .isEmpty();
    assertThat(context.getBeansOfType(org.springdoc.webmvc.ui.SwaggerWelcomeWebMvc.class))
        .isEmpty();
    assertThat(context.getEnvironment().getProperty("springdoc.swagger-ui.enabled"))
        .isEqualTo("false");
  }
}
