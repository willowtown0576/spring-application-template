package dev.template.application.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import dev.template.application.Application;
import dev.template.application.feature.api.authorization.FeatureAuthority;
import dev.template.application.security.OperationsAuthority;
import dev.template.application.security.WithTestUser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

/** Feature sampleの公開schemaを検証する。sample変更時に契約と一緒に更新する。 */
@Tag("documentation")
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(classes = Application.class, properties = {"spring.docker.compose.enabled=false",
    "springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"})
class FeatureApiDocumentationTest {
    /** Springの接続先として共有する、このtest class専用のPostgreSQL。 */
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(System.getProperty("test.postgres.image"));

    /** 実通信と同じJSON変換。 */
    private final JsonMapper mapper;

    /** 実際のMVC構成とSecurity filterを通してHTTP契約を検証するclient。 */
    private final MockMvc mvc;

    /**
     * テスト対象のapplication依存を受け取る。
     *
     * @param mvc 検証に使用するMockMvc
     * @param mapper JSON変換
     */
    @Autowired
    FeatureApiDocumentationTest(final MockMvc mvc, final JsonMapper mapper) {
        this.mapper = mapper;
        this.mvc = mvc;
    }

    /**
     * sampleの作成・検索契約がOpenAPIに反映されることを確認する。
     * @throws Exception HTTP操作に失敗した場合
     */
    @Test
    void describesFeatureContract() throws Exception {
        mvc.perform(get("/v3/api-docs").with(user("operator").authorities(OperationsAuthority.READ)))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.paths['/api/v1/features'].post.responses['201'].content['application/json'].schema['$ref']")
                    .value("#/components/schemas/CreateFeatureResponse"))
            .andExpect(jsonPath(
                "$.paths['/api/v1/features'].post.responses['403'].content['application/problem+json'].schema['$ref']")
                .value("#/components/schemas/ProblemDetail"))
            .andExpect(jsonPath("$.components.schemas.CreateFeatureRequest.properties.name.maxLength").value(100))
            .andExpect(
                jsonPath("$.paths['/api/v1/features'].get.responses['200'].content['application/json'].schema['$ref']")
                    .value("#/components/schemas/SearchFeaturesResponse"))
            .andExpect(jsonPath(
                "$.paths['/api/v1/features/bulk'].post.responses['200'].content['application/json'].schema.type")
                .value("array"))
            .andExpect(jsonPath("$.paths['/api/v1/features/bulk'].put.responses['409']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/features/{id}'].put.responses['409']").exists())
            .andExpect(jsonPath("$.components.schemas.FeatureDetails.properties.version.type").value("integer"));
    }
    /**
     * schema・security・複数行の例が生成資料に保持されることを確認する。
     * @throws Exception HTTP操作に失敗した場合
     */
    @Test
    void describesRequiredFieldsBoundsAndSecurity() throws Exception {
        mvc.perform(get("/v3/api-docs").with(user("operator").authorities(OperationsAuthority.READ)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.components.schemas.CreateFeaturesRequest.required")
                .value(org.hamcrest.Matchers.containsInAnyOrder("requestId", "rows")))
            .andExpect(jsonPath("$.components.schemas.CreateFeaturesRequest.properties.rows.minItems").value(1))
            .andExpect(jsonPath("$.components.schemas.CreateFeaturesRequest.properties.rows.maxItems").value(100))
            .andExpect(jsonPath("$.components.schemas.UpdateFeatureParam.required")
                .value(org.hamcrest.Matchers.containsInAnyOrder("id", "name", "status", "version")))
            .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.issues.items['$ref']")
                .value("#/components/schemas/FeatureCommandIssue"))
            .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.properties").doesNotExist())
            .andExpect(jsonPath("$.components.securitySchemes.SessionCookie.name").value("JSESSIONID"))
            .andExpect(jsonPath("$.paths['/api/v1/features/bulk'].put.security[0].CsrfHeader").isArray())
            .andExpect(jsonPath(
                "$.paths['/api/v1/features/bulk'].post.requestBody.content['application/json'].examples.twoContracts.value.rows.length()")
                .value(2))
            .andExpect(jsonPath(
                "$.paths['/api/v1/features/bulk'].put.requestBody.content['application/json'].examples.twoDifferentUpdates.value.length()")
                .value(2));
    }

    /**
     * YAMLの生成元にある例を実APIへ送信し、登録・再送・検索・更新・競合の説明と整合することを検証する。
     * @throws Exception HTTP操作に失敗した場合
     */
    @Test
    @Transactional
    @WithTestUser({FeatureAuthority.READ, FeatureAuthority.WRITE})
    void executesDocumentedExamplesAgainstRealApi() throws Exception {
        final DocumentContext document = JsonPath
            .parse(mvc.perform(get("/v3/api-docs").with(user("operator").authorities(OperationsAuthority.READ)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        final Map<String, Object> batch = document.read(
            "$.paths['/api/v1/features/bulk'].post.requestBody.content['application/json'].examples.twoContracts.value");
        final String createBody = mapper.writeValueAsString(batch);
        final String result = mvc
            .perform(
                post("/api/v1/features/bulk").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        final List<String> ids = JsonPath.parse(result).read("$");
        assertThat(ids).hasSize(2);
        mvc.perform(
            post("/api/v1/features/bulk").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(result));
        mvc.perform(post("/api/v1/features/bulk")
            .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(createBody.replace("東京本社", "東京支社")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.issues").value(document.read(
                "$.paths['/api/v1/features/bulk'].post.responses['409'].content['application/problem+json'].examples.changedReplay.value.issues",
                List.class)));
        final Map<String, Object> single = document.read(
            "$.paths['/api/v1/features'].post.requestBody.content['application/json'].examples.tokyoContract.value");
        final String singleResult = mvc
            .perform(post("/api/v1/features").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(single)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        final String singleId = JsonPath.parse(singleResult).read("$.id");
        mvc.perform(get("/api/v1/features/" + singleId)).andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value(single.get("name"))).andExpect(jsonPath("$.version").value(1));
        final String searchResult = mvc
            .perform(get("/api/v1/features").param("name", "設備").param("status", "ACTIVE").param("sort", "UPDATED_AT")
                .param("descending", "true").param("size", "2"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2)).andReturn().getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
        final String next = JsonPath.parse(searchResult).read("$.next");
        assertThat(next).isNotBlank();
        final String secondResult = mvc
            .perform(get("/api/v1/features").param("name", "設備").param("status", "ACTIVE").param("sort", "UPDATED_AT")
                .param("descending", "true").param("size", "2").param("cursor", next))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andReturn().getResponse()
            .getContentAsString();
        final String previous = JsonPath.parse(secondResult).read("$.previous");
        mvc.perform(get("/api/v1/features").param("name", "設備").param("status", "ACTIVE").param("sort", "UPDATED_AT")
            .param("descending", "true").param("size", "2").param("cursor", previous).param("previous", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").value(JsonPath.parse(searchResult).read("$.items", List.class)));
        final String exampleCursor = document.read(
            "$.paths['/api/v1/features'].get.responses['200'].content['application/json'].examples.firstPage.value.next");
        mvc.perform(get("/api/v1/features").param("name", "設備").param("status", "ACTIVE").param("sort", "UPDATED_AT")
            .param("descending", "true").param("size", "2").param("cursor", exampleCursor)).andExpect(status().isOk());
        final List<Map<String, Object>> updates = document.read(
            "$.paths['/api/v1/features/bulk'].put.requestBody.content['application/json'].examples.twoDifferentUpdates.value");
        assertThat(updates).hasSize(2);
        for (int index = 0; index < updates.size(); index++) {
            updates.get(index).put("id", ids.get(index));
        }
        mvc.perform(put("/api/v1/features/bulk").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(updates))).andExpect(status().isOk())
            .andExpect(jsonPath("$").value(ids));
        for (final Map<String, Object> row : updates) {
            mvc.perform(get("/api/v1/features/" + row.get("id"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(row.get("name")))
                .andExpect(jsonPath("$.status").value(row.get("status"))).andExpect(jsonPath("$.version").value(2));
        }
        updates.getFirst().put("version", 2);
        mvc.perform(put("/api/v1/features/bulk")
            .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(updates)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.issues").value(document.read(
                "$.paths['/api/v1/features/bulk'].put.responses['409'].content['application/problem+json'].examples.secondRowStale.value.issues",
                List.class)));
        updates.getLast().put("version", 2);
        mvc.perform(put("/api/v1/features/bulk").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(updates))).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        final Map<String, Object> singleUpdate = document.read(
            "$.paths['/api/v1/features/{id}'].put.requestBody.content['application/json'].examples.renameTokyoContract.value");
        mvc.perform(put("/api/v1/features/" + singleId).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(singleUpdate))).andExpect(status().isOk())
            .andExpect(jsonPath("$").value(singleId));
        mvc.perform(
            post("/api/v1/features").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.issues").value(document.read(
                "$.paths['/api/v1/features'].post.responses['422'].content['application/problem+json'].examples.invalidName.value.issues",
                List.class)));
        final List<Map<String, Object>> deletions = document.read(
            "$.paths['/api/v1/features/bulk-delete'].post.requestBody.content['application/json'].examples.twoTargets.value.targets");
        assertThat(deletions).hasSize(2);
        for (int index = 0; index < deletions.size(); index++) {
            deletions.get(index).put("id", ids.get(index));
            deletions.get(index).put("version", 2);
        }
        mvc.perform(post("/api/v1/features/bulk-delete").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("targets", deletions)))).andExpect(status().isOk())
            .andExpect(jsonPath("$").value(ids));
        for (final String id : ids) {
            mvc.perform(get("/api/v1/features/" + id)).andExpect(status().isNotFound());
        }
        mvc.perform(delete("/api/v1/features/" + singleId).param("version", "2").with(csrf()))
            .andExpect(status().isOk()).andExpect(jsonPath("$").value(singleId));
        mvc.perform(delete("/api/v1/features/" + singleId).param("version", "2").with(csrf()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.issues").value(document.read(
                "$.paths['/api/v1/features/{id}'].delete.responses['404'].content['application/problem+json'].examples.missing.value.issues",
                List.class)));
    }
}
