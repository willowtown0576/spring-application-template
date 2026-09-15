package dev.template.build;

import org.flywaydb.core.Flyway;

/** 一時的なdocumentation DBへFlywayを適用するcodegen subprojectのentry point。 */
public final class DocumentationMigration {
    /** static methodのみを公開するためのprivate constructor。 */
    private DocumentationMigration() {
    }

    /**
     * Gradleが指定した一時DB用の入力で処理する。
     *
     * @param args JDBC URL、migrationディレクトリの順。認証情報は環境変数から受け取る
     */
    public static void main(final String[] args) {
        Flyway.configure().dataSource(args[0], "documentation", System.getenv("DOCS_DB_PASSWORD"))
            .defaultSchema("public").locations("filesystem:" + args[1]).load().migrate();
    }
}
