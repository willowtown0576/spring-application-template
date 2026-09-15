package dev.template.build;

import org.flywaydb.core.Flyway;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Generate;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Jdbc;
import org.jooq.meta.jaxb.Target;
import org.jooq.meta.postgres.PostgresDatabase;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** 使い捨てPostgreSQLへmigrationしjOOQを生成するGradle用entry point。失敗時もDBを破棄する。 */
public final class JooqCodegen {

    /** static methodのみを公開するためのprivate constructor。 */
    private JooqCodegen() {
    }

    /**
     * Gradleが指定した一時DB用の入力で処理する。
     *
     * @param args PostgreSQL image、migrationディレクトリ、生成先の順
     * @throws Exception migrationまたは生成に失敗した場合
     */
    public static void main(final String[] args) throws Exception {
        try (var postgres = new PostgreSQLContainer(args[0])) {
            postgres.start();
            Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .defaultSchema("public").locations("filesystem:" + args[1]).load().migrate();

            GenerationTool.generate(new Configuration()
                .withJdbc(new Jdbc().withUrl(postgres.getJdbcUrl()).withUser(postgres.getUsername())
                    .withPassword(postgres.getPassword()))
                .withGenerator(new Generator()
                    .withDatabase(new Database().withName(PostgresDatabase.class.getName())
                        .withExcludes("(?:information_schema|pg_[^.]*|public|system)\\..*"))
                    .withGenerate(new Generate().withGeneratedAnnotationDate(false))
                    .withTarget(new Target().withPackageName("dev.template.application.jooq").withDirectory(args[2]))));
        }
    }
}
