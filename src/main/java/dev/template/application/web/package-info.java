/** HTTPとMVC画面のadapter。業務操作の呼び出し先はfeatureの公開APIとする。 */
@ApplicationModule(allowedDependencies = {"feature::command", "feature::query", "feature::model", "logging", "common"})
package dev.template.application.web;

import org.springframework.modulith.ApplicationModule;
