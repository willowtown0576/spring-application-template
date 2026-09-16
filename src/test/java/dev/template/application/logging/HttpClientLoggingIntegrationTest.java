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
import org.junit.jupiter.api.Tag;
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

/** 実HTTP通信でtimeout、相関ID、ログ秘匿、更新リクエストの非retryを検証する。 */
@ExtendWith(OutputCaptureExtension.class)
@Tag("integration")
class HttpClientLoggingIntegrationTest {
    /**
     * 成功・遅延・503応答を返すHTTP serverに接続し、相関ID伝播、timeout、POSTの非retry、ログの機密情報除外を検証する。
     *
     * @param output テスト中のログ出力
     * @throws Exception HTTP操作またはテスト環境の処理に失敗した場合
     */
    @Test
    void serviceGroupsInheritTimeoutsCorrelationAndNoRetry(final CapturedOutput output) throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final AtomicInteger posts = new AtomicInteger();
        final var releaseSlow = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            server.setExecutor(executor);
            server.createContext("/echo", exchange -> {
                final byte[] body = exchange.getRequestHeaders().getFirst("X-Correlation-ID")
                    .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (var stream = exchange.getResponseBody()) {
                    stream.write(body);
                }
            });

            // 応答を保留してclient側timeoutを発生させる。
            server.createContext("/slow", exchange -> {
                try {
                    releaseSlow.await(5, TimeUnit.SECONDS);
                } catch (final InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } finally {
                    exchange.close();
                }
            });

            // 更新要求の到達回数でretryの有無を確認する。
            server.createContext("/failure", exchange -> {
                posts.incrementAndGet();
                exchange.sendResponseHeaders(503, -1);
                exchange.close();
            });

            server.start();
            MDC.put(CorrelationFilter.KEY, "test-correlation");
            try {
                // Boot標準のclient groupへ共通設定が伝わることを実通信で確認する。
                new ApplicationContextRunner().withUserConfiguration(HttpClientLogging.class, Clients.class)
                    .withConfiguration(AutoConfigurations.of(HttpClientAutoConfiguration.class,
                        ImperativeHttpClientAutoConfiguration.class, RestClientAutoConfiguration.class,
                        HttpServiceClientPropertiesAutoConfiguration.class, HttpServiceClientAutoConfiguration.class))
                    .withPropertyValues("spring.http.clients.connect-timeout=1s",
                        "spring.http.clients.read-timeout=200ms",
                        "spring.http.serviceclient.test.base-url=http://127.0.0.1:" + server.getAddress().getPort())
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context.getBean(HttpClientSettings.class).connectTimeout())
                            .isEqualTo(Duration.ofSeconds(1));
                        final var client = context.getBean(Client.class);
                        assertThat(client.echo()).isEqualTo("test-correlation");
                        assertThatThrownBy(client::slow).isInstanceOf(ResourceAccessException.class);
                        assertThatThrownBy(client::failure).isInstanceOf(RestClientResponseException.class);
                        assertThat(posts).hasValue(1);
                    });

                assertThat(output.getAll()).contains("HTTP outgoing", "status=200", "status=503")
                    .doesNotContain("private-query-token");
            } finally {
                MDC.remove(CorrelationFilter.KEY);
                releaseSlow.countDown();
                server.stop(0);
            }
        }
    }

    /** テストサーバーの成功・遅延・障害を呼び分けるHTTP契約。 */
    interface Client {
        /**
         * 相関IDを応答で確認する。
         *
         * @return サーバーの応答本文
         */
        @GetExchange("/echo?token=private-query-token")
        String echo();

        /**
         * 更新の失敗が自動retryされないことを確認する。
         *
         * @return サーバーの応答本文
         */
        @PostExchange("/failure")
        String failure();

        /**
         * 応答遅延によるtimeoutを確認する。
         *
         * @return サーバーの応答本文
         */
        @GetExchange("/slow")
        String slow();
    }

    /** Boot標準HTTP Service Groupへテストclientを登録する。 */
    @TestConfiguration(proxyBeanMethods = false)
    @ImportHttpServices(group = "test", types = Client.class)
    static class Clients {
    }
}
