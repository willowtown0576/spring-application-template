package dev.template.build;

import org.flywaydb.core.Flyway;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Generate;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Jdbc;
import org.jooq.meta.jaxb.SchemaMappingType;
import org.jooq.meta.jaxb.Target;
import org.testcontainers.postgresql.PostgreSQLContainer;

public final class JooqCodegen {

  private JooqCodegen() {}

  public static void main(String[] args) throws Exception {
    try (var postgres = new PostgreSQLContainer(args[0])) {
      postgres.start();
      Flyway.configure()
          .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
          .defaultSchema("public")
          .locations("filesystem:" + args[1])
          .load()
          .migrate();

      GenerationTool.generate(
          new Configuration()
              .withJdbc(
                  new Jdbc()
                      .withUrl(postgres.getJdbcUrl())
                      .withUser(postgres.getUsername())
                      .withPassword(postgres.getPassword()))
              .withGenerator(
                  new Generator()
                      .withDatabase(
                          new Database()
                              .withName("org.jooq.meta.postgres.PostgresDatabase")
                              .withSchemata(new SchemaMappingType().withInputSchema("feature")))
                      .withGenerate(new Generate().withGeneratedAnnotationDate(false))
                      .withTarget(
                          new Target()
                              .withPackageName("dev.template.application.jooq.feature")
                              .withDirectory(args[2]))));
    }
  }
}
