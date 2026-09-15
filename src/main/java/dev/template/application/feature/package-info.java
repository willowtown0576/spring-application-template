/** Featureの公開契約と内部実装。共通基盤と所有schemaのjOOQ型だけに依存する。 */
@ApplicationModule(allowedDependencies = {"jooq", "common"})
package dev.template.application.feature;

import org.springframework.modulith.ApplicationModule;
