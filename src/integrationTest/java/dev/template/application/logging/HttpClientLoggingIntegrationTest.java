package dev.template.application.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientPropertiesAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.service.HttpServiceClientAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.registry.ImportHttpServices;

@ExtendWith(OutputCaptureExtension.class)
class HttpClientLoggingIntegrationTest {
  @Test
  void serviceGroupsInheritTimeoutsCorrelationAndNoRetry(CapturedOutput output) throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    AtomicInteger posts = new AtomicInteger();
    var releaseSlow = new CountDownLatch(1);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      server.setExecutor(executor);
      server.createContext(
          "/echo",
          exchange -> {
            byte[] body =
                exchange
                    .getRequestHeaders()
                    .getFirst("X-Correlation-ID")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var stream = exchange.getResponseBody()) {
              stream.write(body);
            }
          });
      server.createContext(
          "/slow",
          exchange -> {
            try {
              releaseSlow.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
              Thread.currentThread().interrupt();
            } finally {
              exchange.close();
            }
          });
      server.createContext(
          "/failure",
          exchange -> {
            posts.incrementAndGet();
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
          });
      server.start();
      MDC.put(CorrelationFilter.KEY, "test-correlation");
      try {
        new ApplicationContextRunner()
            .withUserConfiguration(HttpClientLogging.class, Clients.class)
            .withConfiguration(
                AutoConfigurations.of(
                    HttpClientAutoConfiguration.class,
                    ImperativeHttpClientAutoConfiguration.class,
                    RestClientAutoConfiguration.class,
                    HttpServiceClientPropertiesAutoConfiguration.class,
                    HttpServiceClientAutoConfiguration.class))
            .withPropertyValues(
                "spring.http.clients.connect-timeout=1s",
                "spring.http.clients.read-timeout=200ms",
                "spring.http.serviceclient.test.base-url=http://127.0.0.1:"
                    + server.getAddress().getPort())
            .run(
                context -> {
                  assertThat(context).hasNotFailed();
                  assertThat(context.getBean(HttpClientSettings.class).connectTimeout())
                      .isEqualTo(Duration.ofSeconds(1));
                  var client = context.getBean(Client.class);
                  assertThat(client.echo()).isEqualTo("test-correlation");
                  assertThatThrownBy(client::slow).isInstanceOf(ResourceAccessException.class);
                  assertThatThrownBy(client::failure)
                      .isInstanceOf(RestClientResponseException.class);
                  assertThat(posts).hasValue(1);
                });
        assertThat(output.getAll())
            .contains("HTTP outgoing", "status=200", "status=503")
            .doesNotContain("private-query-token");
      } finally {
        MDC.remove(CorrelationFilter.KEY);
        releaseSlow.countDown();
        server.stop(0);
      }
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  @ImportHttpServices(group = "test", types = Client.class)
  static class Clients {}

  interface Client {
    @GetExchange("/echo?token=private-query-token")
    String echo();

    @GetExchange("/slow")
    String slow();

    @PostExchange("/failure")
    String failure();
  }
}
