package com.example.pinkok_backend.ai;

import com.example.pinkok_backend.entity.AiRequest;
import com.example.pinkok_backend.gemini.GeminiCallException;
import com.example.pinkok_backend.gemini.GeminiClient;
import com.example.pinkok_backend.gemini.GeminiGenerateRequest;
import com.example.pinkok_backend.repository.AiRequestRepository;
import com.example.pinkok_backend.service.GeminiJobRunner;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Gemini 응답이 오지 않을 때 요청이 PROCESSING 에 멈추지 않고 실패로 끝나는지.
 * 앱은 FAILED 를 받아야 "추출에 실패했어요. 다시 시도해주세요" 를 보여줄 수 있다.
 */
class GeminiTimeoutTest {

    private static List<GeminiGenerateRequest.Part> parts() {
        return List.of(GeminiGenerateRequest.Part.ofText("장소를 뽑아줘"));
    }

    private static AiRequest newRequest() {
        AiRequest aiRequest = new AiRequest();
        aiRequest.setId(1L);
        aiRequest.setStatus(GeminiJobRunner.STATUS_PENDING);
        aiRequest.setRetryCount(0);
        return aiRequest;
    }

    @Test
    @DisplayName("응답이 늦게 오면 정해둔 시간 안에 타임아웃으로 끊긴다")
    void clientGivesUpAfterReadTimeout() throws Exception {
        // 응답을 3초 뒤에야 주는 가짜 Gemini 서버
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            GeminiClient client = new GeminiClient("http://127.0.0.1:" + server.getAddress().getPort(),
                    "test-key", "gemini-test", 1000, 300);

            long started = System.currentTimeMillis();
            GeminiCallException e = assertThrows(GeminiCallException.class,
                    () -> client.generate(GeminiGenerateRequest.of(new GeminiGenerateRequest.Content(),
                            new GeminiGenerateRequest.GenerationConfig())));

            assertThat(System.currentTimeMillis() - started).isLessThan(2500);
            assertThat(hasTimeoutCause(e)).isTrue();
        } finally {
            server.stop(0);
        }
    }

    private static boolean hasTimeoutCause(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("타임아웃이 계속되면 재시도 후 FAILED(GEMINI_TIMEOUT) 로 끝난다")
    void timeoutEndsAsFailed() {
        AiRequestRepository repository = Mockito.mock(AiRequestRepository.class);
        GeminiClient client = Mockito.mock(GeminiClient.class);
        Mockito.when(client.generate(Mockito.any()))
                .thenThrow(new GeminiCallException("Gemini 호출에 실패했습니다.", new SocketTimeoutException("Read timed out")));

        GeminiJobRunner runner = new GeminiJobRunner(repository, client, 2, 0, 300_000);
        AiRequest aiRequest = newRequest();

        assertThat(runner.run(aiRequest, parts())).isFalse();
        assertThat(aiRequest.getStatus()).isEqualTo(GeminiJobRunner.STATUS_FAILED);
        assertThat(aiRequest.getErrorCode()).startsWith("GEMINI_TIMEOUT");
        Mockito.verify(client, Mockito.times(3)).generate(Mockito.any());
    }

    @Test
    @DisplayName("재시도까지 합친 전체 시간이 다 되면 남은 재시도를 하지 않고 끝낸다")
    void totalTimeoutStopsRetrying() {
        AiRequestRepository repository = Mockito.mock(AiRequestRepository.class);
        GeminiClient client = Mockito.mock(GeminiClient.class);
        Mockito.when(client.generate(Mockito.any())).thenThrow(new GeminiCallException("503 Service Unavailable"));

        GeminiJobRunner runner = new GeminiJobRunner(repository, client, 3, 0, 0);
        AiRequest aiRequest = newRequest();

        assertThat(runner.run(aiRequest, parts())).isFalse();
        assertThat(aiRequest.getErrorCode()).startsWith("GEMINI_TIMEOUT");
        Mockito.verify(client, Mockito.times(1)).generate(Mockito.any());
    }
}
