package dev.template.build;

import org.flywaydb.core.Flyway;

public final class DocumentationMigration {
  private DocumentationMigration() {}

  public static void main(String[] args) {
    Flyway.configure()
        .dataSource(args[0], "documentation", System.getenv("DOCS_DB_PASSWORD"))
        .defaultSchema("public")
        .locations("filesystem:" + args[1])
        .load()
        .migrate();
  }
}
