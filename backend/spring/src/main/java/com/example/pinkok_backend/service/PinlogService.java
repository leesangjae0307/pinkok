package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.ClipOptionResponse;
import com.example.pinkok_backend.dto.PinlogClipRequest;
import com.example.pinkok_backend.dto.PinlogCreateRequest;
import com.example.pinkok_backend.dto.PinlogResponse;
import com.example.pinkok_backend.entity.DiaryMedia;
import com.example.pinkok_backend.entity.ItineraryItem;
import com.example.pinkok_backend.entity.Pinlog;
import com.example.pinkok_backend.entity.PinlogClip;
import com.example.pinkok_backend.entity.Trip;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.pinlog.VideoComposer;
import com.example.pinkok_backend.repository.DiaryMediaRepository;
import com.example.pinkok_backend.repository.PinlogClipRepository;
import com.example.pinkok_backend.repository.PinlogRepository;
import com.example.pinkok_backend.repository.TripRepository;
import com.example.pinkok_backend.repository.UserRepository;
import com.example.pinkok_backend.security.TripAccessGuard;
import com.example.pinkok_backend.storage.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * PinLog: 장소 기록에 남긴 영상·사진을 동선 순서로 이어붙여 만드는 미니 브이로그.
 *
 * <p>여행을 팀원이 함께 쓰므로 한 핀에 여러 사람의 영상이 있을 수 있다. 사용자는 핀마다
 * <b>내 영상 / 팀원 영상</b> 중 하나를 골라 보내고, 아무것도 고르지 않으면 서버가 동선 순서대로
 * 자동으로 고른다(내 것 우선). 세 명이 갔는데 한 명이 깜빡한 장소도 비지 않게 하기 위한 설계다.
 *
 * <p>합성은 수십 초가 걸려서 요청을 붙잡지 않고 별도 스레드에서 처리한다.
 * 앱은 상태(QUEUED → PROCESSING → DONE/FAILED)를 다시 조회해서 확인한다.
 */
@Service
public class PinlogService {

    private static final Logger log = LoggerFactory.getLogger(PinlogService.class);

    private static final String STATUS_QUEUED = "QUEUED";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_FAILED = "FAILED";

    private static final int MAX_CLIPS = 60;
    private static final String FOLDER = "pinlogs/";
    private static final DateTimeFormatter FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");

    private final PinlogRepository pinlogRepository;
    private final PinlogClipRepository pinlogClipRepository;
    private final DiaryMediaRepository diaryMediaRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TripAccessGuard tripAccessGuard;
    private final VideoComposer videoComposer;
    private final FileStorage fileStorage;
    private final FileService fileService;
    private final TaskExecutor pinlogTaskExecutor;

    public PinlogService(PinlogRepository pinlogRepository,
                         PinlogClipRepository pinlogClipRepository,
                         DiaryMediaRepository diaryMediaRepository,
                         TripRepository tripRepository,
                         UserRepository userRepository,
                         TripAccessGuard tripAccessGuard,
                         VideoComposer videoComposer,
                         FileStorage fileStorage,
                         FileService fileService,
                         TaskExecutor pinlogTaskExecutor) {
        this.pinlogRepository = pinlogRepository;
        this.pinlogClipRepository = pinlogClipRepository;
        this.diaryMediaRepository = diaryMediaRepository;
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.tripAccessGuard = tripAccessGuard;
        this.videoComposer = videoComposer;
        this.fileStorage = fileStorage;
        this.fileService = fileService;
        this.pinlogTaskExecutor = pinlogTaskExecutor;
    }

    // ------------------------------------------------------------------
    // 만들기
    // ------------------------------------------------------------------

    @Transactional
    public PinlogResponse create(Long userId, PinlogCreateRequest request) {
        tripAccessGuard.requireMember(request.getTripId(), userId);
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(request.getTripId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "여행을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        List<DiaryMedia> media = resolveMedia(trip.getId(), userId, request.getClips(),
                Boolean.TRUE.equals(request.getIncludePhotos()));

        Pinlog pinlog = new Pinlog();
        pinlog.setTrip(trip);
        pinlog.setUser(user);
        pinlog.setTitle(request.getTitle());
        pinlog.setTemplateCode(request.getTemplateCode());
        pinlog.setStyleCode(request.getStyleCode());
        pinlog.setStatus(STATUS_QUEUED);
        pinlog.setCreatedAt(LocalDateTime.now());
        pinlogRepository.save(pinlog);

        List<PinlogClip> clips = saveClips(pinlog, media, request.getClips());

        startAfterCommit(pinlog.getId());
        return PinlogResponse.of(pinlog, clips, userId);
    }

    /**
     * 쓸 미디어를 정한다. 앱이 고른 게 있으면 그 순서를 그대로 쓰고,
     * 없으면 동선 순서대로 핀마다 하나씩 자동으로 고른다.
     */
    private List<DiaryMedia> resolveMedia(Long tripId, Long userId,
                                          List<PinlogClipRequest> requested, boolean includePhotos) {
        List<DiaryMedia> media = requested == null || requested.isEmpty()
                ? autoSelect(tripId, userId, includePhotos)
                : pickRequested(tripId, requested);

        if (media.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "이어붙일 영상이 없습니다. 장소 기록에 영상을 먼저 남겨주세요.");
        }
        if (media.size() > MAX_CLIPS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "클립은 " + MAX_CLIPS + "개까지 넣을 수 있습니다.");
        }
        return media;
    }

    private List<DiaryMedia> pickRequested(Long tripId, List<PinlogClipRequest> requested) {
        // 한 번에 읽어서 id로 찾아 쓴다 (요청한 순서를 그대로 지키기 위해 목록 순서는 쓰지 않는다)
        Map<Long, DiaryMedia> byId = new LinkedHashMap<>();
        for (DiaryMedia media : diaryMediaRepository.findAllByTripId(tripId)) {
            byId.put(media.getId(), media);
        }

        List<DiaryMedia> picked = new ArrayList<>();
        for (PinlogClipRequest clip : requested) {
            DiaryMedia media = byId.get(clip.getDiaryMediaId());
            if (media == null) {
                // 없는 id 이거나 다른 여행의 미디어 — 둘 다 "이 여행에서 쓸 수 없는 것"이다
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "이 여행의 기록에 없는 사진·영상입니다. (diaryMediaId=" + clip.getDiaryMediaId() + ")");
            }
            picked.add(media);
        }
        return picked;
    }

    /**
     * 핀마다 하나씩 자동으로 고른다. 우선순위는 <b>내 영상 → 팀원 영상</b>이고,
     * {@code includePhotos} 를 켜면 영상이 없는 핀을 사진으로 채운다.
     *
     * <p>사진을 기본으로 넣지 않는 이유는 앱의 클립 선택 화면이 영상만 다루기 때문이다.
     * 다만 합성 자체는 사진을 정지 장면으로 처리할 수 있게 해뒀다 — 영상 합성이 끝내 문제가 되면
     * 이 값만 켜서 사진 슬라이드쇼로 전환할 수 있다(PLAN 비상계획).
     */
    private List<DiaryMedia> autoSelect(Long tripId, Long userId, boolean includePhotos) {
        List<DiaryMedia> all = diaryMediaRepository.findAllByTripId(tripId).stream()
                .filter(m -> includePhotos || isVideo(m))
                .toList();
        Map<Long, List<DiaryMedia>> byPin = groupByPin(all);

        List<DiaryMedia> picked = new ArrayList<>();
        for (List<DiaryMedia> candidates : byPin.values()) {
            candidates.stream()
                    .min(Comparator
                            .comparingInt((DiaryMedia m) -> isVideo(m) ? 0 : 1)
                            .thenComparingInt(m -> isMine(m, userId) ? 0 : 1)
                            .thenComparing(DiaryMedia::getId))
                    .ifPresent(picked::add);
        }
        return picked;
    }

    private List<PinlogClip> saveClips(Pinlog pinlog, List<DiaryMedia> media, List<PinlogClipRequest> requested) {
        List<PinlogClip> clips = new ArrayList<>();
        for (int i = 0; i < media.size(); i++) {
            PinlogClipRequest request = requested == null || requested.isEmpty() ? null : requested.get(i);

            PinlogClip clip = new PinlogClip();
            clip.setPinlog(pinlog);
            clip.setDiaryMedia(media.get(i));
            clip.setClipOrder(i + 1);
            clip.setStartMs(request == null || request.getStartMs() == null ? 0 : request.getStartMs());
            clip.setEndMs(request == null ? null : request.getEndMs());
            clips.add(clip);
        }
        pinlogClipRepository.saveAll(clips);
        return clips;
    }

    /**
     * 합성은 저장이 <b>확정된 뒤에</b> 시작한다.
     *
     * <p>커밋 전에 다른 스레드가 조회하면 방금 저장한 PinLog 를 못 찾아서 QUEUED 상태로 영영 멈춘다
     * (AI 요청에서 실제로 겪은 문제다). afterCommit 으로 걸면 그 상황 자체가 생기지 않는다.
     */
    private void startAfterCommit(Long pinlogId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            pinlogTaskExecutor.execute(() -> compose(pinlogId));
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                pinlogTaskExecutor.execute(() -> compose(pinlogId));
            }
        });
    }

    // ------------------------------------------------------------------
    // 합성 (별도 스레드에서 실행됨)
    // ------------------------------------------------------------------

    void compose(Long pinlogId) {
        Pinlog pinlog = pinlogRepository.findById(pinlogId).orElse(null);
        if (pinlog == null) {
            log.warn("PinLog 를 찾지 못해 합성을 건너뛴다 (id={})", pinlogId);
            return;
        }

        pinlog.setStatus(STATUS_PROCESSING);
        pinlogRepository.save(pinlog);

        Path workDir = null;
        try {
            // 원본은 저장소(지금은 서버 폴더, 나중엔 클라우드)에 있으므로 작업용 폴더로 복사해서 쓴다.
            // 경로에 한글이 섞이면 FFmpeg 가 파일을 못 찾는 경우가 있어 임시 폴더를 쓰는 편이 안전하다.
            workDir = Files.createTempDirectory("pinlog-" + pinlogId + "-");
            List<VideoComposer.Clip> clips = copyToWorkDir(pinlogId, workDir);

            Path output = workDir.resolve("pinlog.mp4");
            Path thumbnail = workDir.resolve("pinlog_thumb.jpg");
            VideoComposer.Result result = videoComposer.compose(clips, output, thumbnail);

            String basePath = FOLDER + LocalDate.now().format(FOLDER_FORMAT) + "/" + UUID.randomUUID();
            String videoUrl = store(output, basePath + ".mp4");
            String thumbnailUrl = Files.exists(thumbnail) ? store(thumbnail, basePath + "_thumb.jpg") : null;

            pinlog.setVideoUrl(videoUrl);
            pinlog.setThumbnailUrl(thumbnailUrl);
            pinlog.setDurationMs(result.durationMs());
            pinlog.setResolution(result.resolution());
            pinlog.setStatus(STATUS_DONE);
            pinlog.setCompletedAt(LocalDateTime.now());
            pinlogRepository.save(pinlog);

        } catch (Exception e) {
            // 실패해도 흔적을 남긴다. 아무 기록 없이 멈춰 있으면 원인을 찾을 수 없다.
            log.warn("PinLog 합성 실패 (id={})", pinlogId, e);
            pinlog.setStatus(STATUS_FAILED);
            pinlog.setErrorMessage(truncate(e.getMessage(), 500));
            pinlog.setCompletedAt(LocalDateTime.now());
            pinlogRepository.save(pinlog);

        } finally {
            deleteQuietly(workDir);
        }
    }

    private List<VideoComposer.Clip> copyToWorkDir(Long pinlogId, Path workDir) throws IOException {
        List<PinlogClip> clips = pinlogClipRepository.findAllByPinlogId(pinlogId);
        List<VideoComposer.Clip> sources = new ArrayList<>();

        for (int i = 0; i < clips.size(); i++) {
            PinlogClip clip = clips.get(i);
            DiaryMedia media = clip.getDiaryMedia();

            String path = fileStorage.pathOf(media.getFileUrl());
            if (path == null || !fileStorage.exists(path)) {
                throw new IOException("원본 파일을 찾을 수 없습니다: " + media.getFileUrl());
            }

            Path copied = workDir.resolve("src" + i + extensionOf(path));
            Files.write(copied, fileStorage.read(path));
            sources.add(new VideoComposer.Clip(copied, !isVideo(media), clip.getStartMs(), clip.getEndMs()));
        }
        return sources;
    }

    private String store(Path file, String path) throws IOException {
        try (InputStream in = new ByteArrayInputStream(Files.readAllBytes(file))) {
            return fileStorage.save(in, path);
        }
    }

    // ------------------------------------------------------------------
    // 조회 · 삭제
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PinlogResponse get(Long pinlogId, Long userId) {
        Pinlog pinlog = getOrThrow(pinlogId);
        tripAccessGuard.requireMember(pinlog.getTrip().getId(), userId);
        return PinlogResponse.of(pinlog, pinlogClipRepository.findAllByPinlogId(pinlogId), userId);
    }

    @Transactional(readOnly = true)
    public List<PinlogResponse> list(Long tripId, Long userId) {
        tripAccessGuard.requireMember(tripId, userId);

        List<Pinlog> pinlogs = pinlogRepository.findAllByTripId(tripId);
        if (pinlogs.isEmpty()) {
            return List.of();
        }

        Map<Long, List<PinlogClip>> clipsByPinlog = new LinkedHashMap<>();
        List<Long> ids = pinlogs.stream().map(Pinlog::getId).toList();
        for (PinlogClip clip : pinlogClipRepository.findAllByPinlogIdIn(ids)) {
            clipsByPinlog.computeIfAbsent(clip.getPinlog().getId(), id -> new ArrayList<>()).add(clip);
        }

        return pinlogs.stream()
                .map(p -> PinlogResponse.of(p, clipsByPinlog.getOrDefault(p.getId(), List.of()), userId))
                .toList();
    }

    /**
     * 핀마다 고를 수 있는 클립 목록 (팀원들 것 모두). PinLog 만들기 화면이 이걸로 그려진다.
     * 기본은 <b>영상만</b>이고, {@code includePhotos} 를 켜면 사진도 함께 돌려준다.
     */
    @Transactional(readOnly = true)
    public List<ClipOptionResponse> clipOptions(Long tripId, Long userId, boolean includePhotos) {
        tripAccessGuard.requireMember(tripId, userId);

        List<DiaryMedia> all = diaryMediaRepository.findAllByTripId(tripId).stream()
                .filter(m -> includePhotos || isVideo(m))
                .toList();
        Map<Long, List<DiaryMedia>> byPin = groupByPin(all);
        return byPin.values().stream()
                .map(media -> ClipOptionResponse.of(
                        media.get(0).getDiary().getItineraryItem(), sortForDisplay(media, userId), userId))
                .toList();
    }

    @Transactional
    public void delete(Long pinlogId, Long userId) {
        Pinlog pinlog = getOrThrow(pinlogId);
        tripAccessGuard.requireMember(pinlog.getTrip().getId(), userId);
        if (!pinlog.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 만든 PinLog만 삭제할 수 있습니다.");
        }

        String videoUrl = pinlog.getVideoUrl();
        pinlogClipRepository.deleteAllByPinlog_Id(pinlogId);
        pinlogRepository.delete(pinlog);

        // 합성 결과 영상은 PinLog 전용이라 같이 지운다 (원본 기록 영상은 그대로 둔다)
        fileService.deleteAfterCommit(videoUrl);
    }

    // ------------------------------------------------------------------
    // 잡일
    // ------------------------------------------------------------------

    /** 핀(장소) 별로 묶는다. 쿼리가 이미 동선 순서로 읽어오므로 순서가 보존되는 LinkedHashMap 을 쓴다. */
    private Map<Long, List<DiaryMedia>> groupByPin(List<DiaryMedia> media) {
        Map<Long, List<DiaryMedia>> byPin = new LinkedHashMap<>();
        for (DiaryMedia m : media) {
            byPin.computeIfAbsent(m.getDiary().getItineraryItem().getId(), id -> new ArrayList<>()).add(m);
        }
        return byPin;
    }

    /** 고르는 화면에서는 영상을 먼저, 그중에서도 내 것을 먼저 보여준다. */
    private List<DiaryMedia> sortForDisplay(List<DiaryMedia> media, Long userId) {
        return media.stream()
                .sorted(Comparator
                        .comparingInt((DiaryMedia m) -> isVideo(m) ? 0 : 1)
                        .thenComparingInt(m -> isMine(m, userId) ? 0 : 1)
                        .thenComparing(DiaryMedia::getId))
                .toList();
    }

    private boolean isVideo(DiaryMedia media) {
        return FileService.MEDIA_TYPE_VIDEO.equals(media.getMediaType());
    }

    private boolean isMine(DiaryMedia media, Long userId) {
        return media.getDiary().getUser().getId().equals(userId);
    }

    private Pinlog getOrThrow(Long pinlogId) {
        return pinlogRepository.findById(pinlogId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PinLog를 찾을 수 없습니다."));
    }

    private String extensionOf(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(dot);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "알 수 없는 오류";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    /** 작업용 임시 폴더 정리. 실패해도 합성 결과에는 영향이 없으므로 조용히 넘어간다. */
    private void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // 정리 실패는 무시한다
                }
            });
        } catch (IOException ignored) {
            // 정리 실패는 무시한다
        }
    }
}
