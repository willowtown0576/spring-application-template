package dev.template.application.web.rest;

import dev.template.application.feature.api.command.CreateFailure;
import dev.template.application.feature.api.command.FeatureCommands;
import dev.template.application.feature.api.command.Result;
import dev.template.application.feature.api.query.FeatureQueries;
import dev.template.application.feature.api.query.FeatureView;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(path = "/api/{version}/features", version = "v1")
class FeatureController {
  private final FeatureCommands commands;
  private final FeatureQueries queries;

  FeatureController(FeatureCommands commands, FeatureQueries queries) {
    this.commands = commands;
    this.queries = queries;
  }

  @PostMapping
  @ApiResponse(
      responseCode = "201",
      description = "Created",
      headers =
          @Header(name = "Location", schema = @Schema(type = "string", format = "uri-reference")),
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = CreatedFeature.class)))
  @ApiResponse(
      responseCode = "422",
      description = "Invalid feature name",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  ResponseEntity<?> create(@Valid @RequestBody CreateRequest request) {
    return switch (commands.create(request.name())) {
      case Result.Success<UUID, CreateFailure>(var id) ->
          ResponseEntity.created(URI.create("/api/v1/features/" + id)).body(new CreatedFeature(id));
      case Result.Failure<UUID, CreateFailure>(var reason) ->
          switch (reason) {
            case INVALID_NAME ->
                ResponseEntity.unprocessableContent()
                    .body(
                        ProblemDetail.forStatusAndDetail(
                            HttpStatus.UNPROCESSABLE_CONTENT, "Feature name is invalid."));
          };
    };
  }

  @ApiResponse(
      responseCode = "200",
      description = "Found",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = FeatureView.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Not found",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @GetMapping("/{id}")
  FeatureView find(@PathVariable UUID id) {
    return queries
        .find(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature not found."));
  }

  record CreateRequest(
      @NotNull
          @Schema(
              minLength = 1,
              maxLength = 100,
              description = "Unicode code points; blank, NUL and malformed surrogates are rejected")
          String name) {}

  record CreatedFeature(UUID id) {}
}
