package api.base;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import static api.utils.Constants.HOST;
import static api.utils.Constants.WIREMOCK_PORT;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Базовый класс для всех API тестов.
 * Управляет жизненным циклом WireMock сервера.
 */
public class BaseTest {

    protected static WireMockServer wireMockServer;

    /**
     * Запускает WireMock сервер перед всеми тестами.
     */
    @BeforeAll
    static void setUpAll() {
        wireMockServer = new WireMockServer(wireMockConfig().port(WIREMOCK_PORT));
        wireMockServer.start();
        WireMock.configureFor(HOST, WIREMOCK_PORT);
    }

    /**
     * Сбрасывает заглушки WireMock перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        if (wireMockServer != null) {
            wireMockServer.resetAll();
        }
    }

    /**
     * Останавливает WireMock сервер после всех тестов.
     */
    @AfterAll
    static void tearDownAll() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }
}