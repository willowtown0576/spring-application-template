package dev.template.application.feature.internal.command;

import com.github.f4b6a3.uuid.factory.standard.TimeOrderedEpochFactory;
import dev.template.application.feature.internal.usecase.CreateFeatureUseCase;
import dev.template.application.feature.internal.usecase.FeatureRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class FeatureConfiguration {
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  CreateFeatureUseCase createFeatureUseCase(FeatureRepository repository, Clock clock) {
    var factory = new TimeOrderedEpochFactory(clock);
    return new CreateFeatureUseCase(repository, factory::create);
  }
}
