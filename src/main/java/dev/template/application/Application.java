package dev.template.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** starterの起動点。環境設定・migration・各技術基盤はSpring Bootの構成に委譲する。 */
@SpringBootApplication
public class Application {

    /**
     * Spring Boot applicationを起動する。
     *
     * @param args Spring Bootへ渡す起動引数
     */
    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
