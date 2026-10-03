package com.example.pinkok_backend.ai;

import com.example.pinkok_backend.gemini.GeminiCallException;
import com.example.pinkok_backend.gemini.GeminiClient;
import com.example.pinkok_backend.gemini.GeminiGenerateRequest;
import com.example.pinkok_backend.gemini.GeminiGenerateResponse;
import com.example.pinkok_backend.kakao.KakaoLocalApiClient;
import com.example.pinkok_backend.support.DatabaseCleaner;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 장소 추출(정훈 담당 - Gemini 호출) 테스트.
 * 실제 Gemini 서버는 호출하지 않고 GeminiClient 를 Mockito 로 대체한다.
 * 비동기 처리는 TestAsyncConfig 로 같은 스레드에서 즉시 실행되게 바꿔서, Send 직후 바로 결과를 검증할 수 있다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestAsyncConfig.class)
class AiRequestTest {

    private static final String EMPTY_RESULT_JSON = "{\"title\":null,\"places\":[]}";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @MockitoBean
    GeminiClient geminiClient;

    /** 추출이 성공하면 좌표 변환(PlaceCandidateService)이 이어서 돌기 때문에, 실제 카카오 서버를 부르지 않도록 막아둔다. */
    @MockitoBean
    KakaoLocalApiClient kakaoLocalApiClient;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        Mockito.when(geminiClient.modelName()).thenReturn("gemini-2.5-flash-test");
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    private String signupAndLogin() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ai@pinkok.com\",\"username\":\"aiuser01\",\"password\":\"pass1234\",\"nickname\":\"테스터\"}"))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ai@pinkok.com\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private static GeminiGenerateResponse successResponse(String json, int promptTokens, int outputTokens) {
        GeminiGenerateResponse.Part part = new GeminiGenerateResponse.Part();
        part.setText(json);
        GeminiGenerateResponse.Content content = new GeminiGenerateResponse.Content();
        content.setParts(List.of(part));
        GeminiGenerateResponse.Candidate candidate = new GeminiGenerateResponse.Candidate();
        candidate.setContent(content);
        candidate.setFinishReason("STOP");

        GeminiGenerateResponse.UsageMetadata usage = new GeminiGenerateResponse.UsageMetadata();
        usage.setPromptTokenCount(promptTokens);
        usage.setCandidatesTokenCount(outputTokens);
        usage.setTotalTokenCount(promptTokens + outputTokens);

        GeminiGenerateResponse response = new GeminiGenerateResponse();
        response.setCandidates(List.of(candidate));
        response.setUsageMetadata(usage);
        return response;
    }

    @Test
    @DisplayName("TEXT 입력 - 성공하면 SUCCESS 상태 + 추출된 장소 목록(검증된 응답 스키마)이 담긴다")
    void create_textInput_success() throws Exception {
        String token = signupAndLogin();
        String extractedJson = "{\"title\":\"제주 여행\",\"places\":["
                + "{\"name\":\"협재 해수욕장\",\"address\":\"제주 한림읍\",\"category\":\"해변\",\"description\":\"하얀 모래와 에메랄드빛 바다\",\"lat\":33.39,\"lng\":126.24}"
                + "]}";
        Mockito.when(geminiClient.generate(Mockito.any())).thenReturn(successResponse(extractedJson, 120, 40));

        mockMvc.perform(post("/ai-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputType\":\"TEXT\",\"sourceText\":\"오늘은 협재 해수욕장에 다녀왔어요\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.title").value("제주 여행"))
                .andExpect(jsonPath("$.places[0].name").value("협재 해수욕장"))
                .andExpect(jsonPath("$.places[0].category").value("해변"))
                .andExpect(jsonPath("$.places[0].lat").value(33.39))
                .andExpect(jsonPath("$.retryCount").value(0));
    }

    @Test
    @DisplayName("LINK 입력(유튜브) - fileData로 영상을 직접 Gemini에 보낸다 (검증된 프로토타입 방식)")
    void create_youtubeLink_sendsFileDataNotJustText() throws Exception {
        String token = signupAndLogin();
        Mockito.when(geminiClient.generate(Mockito.any())).thenReturn(successResponse(EMPTY_RESULT_JSON, 50, 5));

        mockMvc.perform(post("/ai-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputType\":\"LINK\",\"sourceUrl\":\"https://youtube.com/watch?v=abc123\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        ArgumentCaptor<GeminiGenerateRequest> captor = ArgumentCaptor.forClass(GeminiGenerateRequest.class);
        Mockito.verify(geminiClient).generate(captor.capture());
        List<GeminiGenerateRequest.Part> parts = captor.getValue().getContents().get(0).getParts();

        assertTrue(parts.get(0).getFileData() != null, "유튜브는 텍스트가 아니라 fileData로 보내야 한다");
        assertTrue(parts.get(0).getFileData().getFileUri().contains("youtube.com"));
        assertTrue(parts.get(0).getFileData().getMimeType().equals("video/mp4"));
    }

    @Test
    @DisplayName("LINK 입력(유튜브 아님) - fileData 없이 URL을 텍스트로만 설명한다")
    void create_nonYoutubeLink_sendsTextOnly() throws Exception {
        String token = signupAndLogin();
        Mockito.when(geminiClient.generate(Mockito.any())).thenReturn(successResponse(EMPTY_RESULT_JSON, 50, 5));

        mockMvc.perform(post("/ai-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputType\":\"LINK\",\"sourceUrl\":\"https://blog.naver.com/somewhere\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        ArgumentCaptor<GeminiGenerateRequest> captor = ArgumentCaptor.forClass(GeminiGenerateRequest.class);
        Mockito.verify(geminiClient).generate(captor.capture());
        List<GeminiGenerateRequest.Part> parts = captor.getValue().getContents().get(0).getParts();

        assertNull(parts.get(0).getFileData(), "유튜브가 아니면 fileData를 쓰지 않는다");
        assertNotNull(parts.get(0).getText());
        assertTrue(parts.get(0).getText().contains("blog.naver.com"));
    }

    @Test
    @DisplayName("입력 검증 - LINK인데 sourceUrl이 없으면 400, 호출 자체를 안 한다")
    void create_missingSourceUrl_returns400() throws Exception {
        String token = signupAndLogin();

        mockMvc.perform(post("/ai-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputType\":\"LINK\"}"))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(geminiClient);
    }

    @Test
    @DisplayName("잘못된 inputType이면 400")
    void create_invalidInputType_returns400() throws Exception {
        String token = signupAndLogin();

        mockMvc.perform(post("/ai-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputType\":\"AUDIO\",\"sourceText\":\"아무거나\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Gemini 호출이 일시적으로 실패하면 재시도하고, 재시도 중 성공하면 SUCCESS")
    void create_retriesOnTransientFailure_thenSucceeds() throws Exception {
        String token = signupAndLogin();
        Mockito.when(geminiClient.generate(Mockito.any()))
                .thenThrow(new GeminiCallException("일시적 오류"))
                .thenReturn(successResponse(EMPTY_RESULT_JSON, 10, 2));

        mockMvc.perform(post("/ai-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputType\":\"TEXT\",\"sourceText\":\"아무 글\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.retryCount").value(1));

        Mockito.verify(geminiClient, Mockito.times(2)).generate(Mockito.any());
    }

    @Test
    @DisplayName("최대 재시도까지 계속 실패하면 FAILED로 끝난다")
    void create_allRetriesFail_returnsFailed() throws Exception {
        String token = signupAndLogin();
        Mockito.when(geminiClient.generate(Mockito.any()))
                .thenThrow(new GeminiCallException("계속 실패"));

        mockMvc.perform(post("/ai-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputType\":\"TEXT\",\"sourceText\":\"아무 글\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorCode").exists());

        // max-retries=2 -> 최초 시도 + 재시도 2번 = 총 3번 호출
        Mockito.verify(geminiClient, Mockito.times(3)).generate(Mockito.any());
    }

    @Test
    @DisplayName("남의 AI 요청은 조회할 수 없다")
    void get_othersRequest_returns403() throws Exception {
        String ownerToken = signupAndLogin();
        Mockito.when(geminiClient.generate(Mockito.any())).thenReturn(successResponse(EMPTY_RESULT_JSON, 10, 2));

        MvcResult created = mockMvc.perform(post("/ai-requests")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputType\":\"TEXT\",\"sourceText\":\"아무 글\"}"))
                .andExpect(status().isAccepted())
                .andReturn();
        Number id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"stranger@pinkok.com\",\"username\":\"strangerai\",\"password\":\"pass1234\",\"nickname\":\"낯선사람\"}"))
                .andExpect(status().isCreated());
        MvcResult strangerLogin = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"stranger@pinkok.com\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String strangerToken = JsonPath.read(strangerLogin.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/ai-requests/" + id).header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IMAGE 입력 - 업로드한 스크린샷을 base64로 담아 Gemini에 보낸다")
    void create_imageInput_sendsUploadedImageAsBase64() throws Exception {
        String token = signupAndLogin();
        Mockito.when(geminiClient.generate(Mockito.any())).thenReturn(successResponse(EMPTY_RESULT_JSON, 30, 5));

        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        MockMultipartFile file = new MockMultipartFile("file", "screenshot.png", "image/png", out.toByteArray());

        MvcResult uploadResult = mockMvc.perform(multipart("/files")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String imageUrl = JsonPath.read(uploadResult.getResponse().getContentAsString(), "$[0].url");

        mockMvc.perform(post("/ai-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"inputType\":\"IMAGE\",\"imageUrls\":[\"%s\"]}", imageUrl)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        ArgumentCaptor<GeminiGenerateRequest> captor = ArgumentCaptor.forClass(GeminiGenerateRequest.class);
        Mockito.verify(geminiClient).generate(captor.capture());
        List<GeminiGenerateRequest.Part> parts = captor.getValue().getContents().get(0).getParts();

        // 검증된 프로토타입 순서: 이미지(들) 먼저, 프롬프트 텍스트는 맨 뒤
        assertTrue(parts.size() == 2 && parts.get(0).getInlineData() != null,
                "이미지 파트(inlineData)가 먼저 오고 프롬프트 텍스트가 뒤에 와야 한다");
        assertTrue(parts.get(0).getInlineData().getData().length() > 0);
        assertNotNull(parts.get(1).getText());
    }

    @Test
    @DisplayName("업로드한 적 없는 이미지 주소를 보내면 PENDING/PROCESSING 에 멈추지 않고 FAILED(INPUT_ERROR) 로 끝난다")
    void create_unreadableImage_failsInsteadOfGettingStuck() throws Exception {
        String token = signupAndLogin();

        mockMvc.perform(post("/ai-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputType\":\"IMAGE\",\"imageUrls\":[\"/files/2026/09/does-not-exist.png\"]}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorCode").value(org.hamcrest.Matchers.startsWith("INPUT_ERROR")));

        // 실패 기록 때 modelName() 은 부르므로 verifyNoInteractions 가 아니라 "generate 를 안 불렀다"만 확인한다
        Mockito.verify(geminiClient, Mockito.never()).generate(Mockito.any());
    }

    @Test
    @DisplayName("로그인 없이 요청하면 401")
    void create_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/ai-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputType\":\"TEXT\",\"sourceText\":\"아무 글\"}"))
                .andExpect(status().isUnauthorized());
    }
}
