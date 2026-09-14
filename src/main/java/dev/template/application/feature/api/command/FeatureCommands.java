package dev.template.application.feature.api.command;

import java.util.UUID;

public interface FeatureCommands {
  String WRITE = "feature:write";

  Result<UUID, CreateFailure> create(String name);
}
