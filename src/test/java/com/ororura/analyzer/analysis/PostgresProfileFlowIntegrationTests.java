package com.ororura.analyzer.analysis;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Runs the same HTTP/JPA flows with real Flyway DDL and PostgreSQL JSONB. */
@EnabledIfEnvironmentVariable(named="GRAPHIFY_TEST_POSTGRES_URL",matches=".+")
class PostgresProfileFlowIntegrationTests extends ProfileFlowIntegrationTests {
    @DynamicPropertySource static void postgres(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url",() -> System.getenv("GRAPHIFY_TEST_POSTGRES_URL"));
        properties.add("spring.datasource.username",() -> "postgres");
        properties.add("spring.datasource.password",() -> "");
        properties.add("spring.flyway.enabled",() -> "true");
        properties.add("spring.jpa.hibernate.ddl-auto",() -> "validate");
    }
    @Test void postgresProtectsPinnedConfigAndUnweightedMarketRows() throws Exception {
        String profile=create("BACKEND","Go");
        var response=mvc.perform(multipart("/api/resume/analyze").session(session).file(pdf("Go"))
                .param("profileId",profile)).andExpect(status().isOk()).andReturn();
        var tree=json.readTree(response.getResponse().getContentAsString());
        UUID run=UUID.fromString(tree.get("id").asText());
        UUID snapshot=UUID.fromString(tree.get("effectiveConfig").get("marketSnapshotId").asText());
        assertThatThrownBy(() -> jdbc.update("update analysis_runs set effective_config = '{}'::jsonb where id = ?",run))
                .hasMessageContaining("immutable");
        assertThatThrownBy(() -> jdbc.update("update market_snapshots set pipeline_version = 'changed' where id = ?",snapshot))
                .hasMessageContaining("immutable");
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success = true",Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select jsonb_typeof(effective_config) from analysis_runs where id = ?",String.class,run)).isEqualTo("object");
    }
}
