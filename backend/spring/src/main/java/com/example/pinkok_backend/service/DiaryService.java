package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.DiaryCreateRequest;
import com.example.pinkok_backend.dto.DiaryMediaRequest;
import com.example.pinkok_backend.dto.DiaryResponse;
import com.example.pinkok_backend.dto.DiaryUpdateRequest;
import com.example.pinkok_backend.entity.Diary;
import com.example.pinkok_backend.entity.DiaryMedia;
import com.example.pinkok_backend.entity.ItineraryItem;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.repository.DiaryMediaRepository;
import com.example.pinkok_backend.repository.DiaryRepository;
import com.example.pinkok_backend.repository.ItineraryItemRepository;
import com.example.pinkok_backend.repository.PinlogClipRepository;
import com.example.pinkok_backend.repository.UserRepository;
import com.example.pinkok_backend.security.TripAccessGuard;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 장소별 기록: 핀(itinerary_items) 하나에 대해 다녀와서 남기는 메모 · 사진 · 영상.
 *
 * <p>여행 카드는 팀원이 함께 쓰므로 기록도 <b>한 핀에 사람마다 하나씩</b> 쌓인다.
 * 조회는 팀원 누구나, 수정·삭제는 쓴 사람만 할 수 있다.
 */
@Service
public class DiaryService {

    /** 한 기록에 붙일 수 있는 첨부 개수 (사진 + 영상 합계) */
    private static final int MAX_MEDIA_PER_DIARY = 10;
    /** 영상은 PinLog 재료라 핀마다 한 개만 받는다 */
    private static final int MAX_VIDEO_PER_DIARY = 1;

    private final DiaryRepository diaryRepository;
    private final DiaryMediaRepository diaryMediaRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final UserRepository userRepository;
    private final TripAccessGuard tripAccessGuard;
    private final FileService fileService;
    private final PinlogClipRepository pinlogClipRepository;

    public DiaryService(DiaryRepository diaryRepository,
                        DiaryMediaRepository diaryMediaRepository,
                        ItineraryItemRepository itineraryItemRepository,
                        UserRepository userRepository,
                        TripAccessGuard tripAccessGuard,
                        FileService fileService,
                        PinlogClipRepository pinlogClipRepository) {
        this.diaryRepository = diaryRepository;
        this.diaryMediaRepository = diaryMediaRepository;
        this.itineraryItemRepository = itineraryItemRepository;
        this.userRepository = userRepository;
        this.tripAccessGuard = tripAccessGuard;
        this.fileService = fileService;
        this.pinlogClipRepository = pinlogClipRepository;
    }

    @Transactional
    public DiaryResponse create(Long userId, DiaryCreateRequest request) {
        ItineraryItem item = getItemOrThrow(request.getItineraryItemId());
        tripAccessGuard.requireMember(item.getTrip().getId(), userId);

        String requestId = StringUtils.hasText(request.getClientRequestId()) ? request.getClientRequestId().trim() : null;
        Diary existing = diaryRepository.findByItineraryItem_IdAndUser_Id(item.getId(), userId).orElse(null);
        if (existing != null) {
            // 같은 요청을 다시 보낸 것이면(응답을 못 받은 재시도) 이미 만든 기록을 그대로 돌려준다
            if (requestId != null && requestId.equals(existing.getClientRequestId())) {
                return DiaryResponse.of(existing,
                        diaryMediaRepository.findAllByDiary_IdOrderByDisplayOrderAsc(existing.getId()), userId);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "이 장소에는 이미 기록을 남겼습니다. 수정해 주세요.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        Diary diary = new Diary();
        diary.setItineraryItem(item);
        diary.setUser(user);
        diary.setContent(request.getContent());
        diary.setRating(request.getRating());
        diary.setClientRequestId(requestId);
        diary.setCreatedAt(now);
        diary.setUpdatedAt(now);
        diaryRepository.save(diary);

        List<DiaryMedia> media = replaceMedia(diary, request.getMedia(), List.of());
        return DiaryResponse.of(diary, media, userId);
    }

    /**
     * 기록 목록. tripId(여행 전체) 또는 itineraryItemId(핀 하나) 중 하나로만 조회한다.
     */
    @Transactional(readOnly = true)
    public List<DiaryResponse> list(Long userId, Long tripId, Long itineraryItemId) {
        if ((tripId == null) == (itineraryItemId == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "tripId 또는 itineraryItemId 중 하나만 보내주세요.");
        }

        List<Diary> diaries;
        if (tripId != null) {
            tripAccessGuard.requireMember(tripId, userId);
            diaries = diaryRepository.findAllByTripId(tripId);
        } else {
            ItineraryItem item = getItemOrThrow(itineraryItemId);
            tripAccessGuard.requireMember(item.getTrip().getId(), userId);
            diaries = diaryRepository.findAllByItineraryItemId(itineraryItemId);
        }
        if (diaries.isEmpty()) {
            return List.of();
        }

        // 첨부를 기록마다 따로 읽지 않고 한 번에 읽어서 기록별로 나눠 담는다
        Map<Long, List<DiaryMedia>> mediaByDiaryId = new LinkedHashMap<>();
        List<Long> diaryIds = diaries.stream().map(Diary::getId).toList();
        for (DiaryMedia media : diaryMediaRepository.findAllByDiary_IdInOrderByDiary_IdAscDisplayOrderAsc(diaryIds)) {
            mediaByDiaryId.computeIfAbsent(media.getDiary().getId(), id -> new ArrayList<>()).add(media);
        }

        return diaries.stream()
                .map(diary -> DiaryResponse.of(diary, mediaByDiaryId.getOrDefault(diary.getId(), List.of()), userId))
                .toList();
    }

    @Transactional
    public DiaryResponse update(Long diaryId, Long userId, DiaryUpdateRequest request) {
        Diary diary = getDiaryOrThrow(diaryId);
        requireAuthor(diary, userId);

        if (request.getContent() != null) {
            diary.setContent(request.getContent());
        }
        if (request.getRating() != null) {
            diary.setRating(request.getRating());
        }

        List<DiaryMedia> media = diaryMediaRepository.findAllByDiary_IdOrderByDisplayOrderAsc(diaryId);
        if (request.getMedia() != null) {
            media = replaceMedia(diary, request.getMedia(), media);
        }

        diary.setUpdatedAt(LocalDateTime.now());
        diaryRepository.save(diary);
        return DiaryResponse.of(diary, media, userId);
    }

    @Transactional
    public void delete(Long diaryId, Long userId) {
        Diary diary = getDiaryOrThrow(diaryId);
        requireAuthor(diary, userId);
        deleteDiaries(List.of(diary));
    }

    /**
     * 핀 하나에 달린 팀원 모두의 기록을 첨부·파일까지 지운다. 핀을 지우기 직전에 부른다
     * (기록이 핀을 외래키로 물고 있어 남아 있으면 핀을 못 지운다). 권한 확인은 부르는 쪽에서 끝냈다.
     */
    @Transactional
    public void deleteAllOfItem(Long itineraryItemId) {
        deleteDiaries(diaryRepository.findAllByItineraryItemId(itineraryItemId));
    }

    /** 기록 → 첨부를 쓰던 PinLog 클립 행 → 첨부 → 기록 순으로 지우고, 파일은 커밋된 뒤에 지운다. */
    private void deleteDiaries(List<Diary> diaries) {
        if (diaries.isEmpty()) {
            return;
        }
        List<Long> diaryIds = diaries.stream().map(Diary::getId).toList();
        List<DiaryMedia> media = diaryMediaRepository.findAllByDiary_IdInOrderByDiary_IdAscDisplayOrderAsc(diaryIds);
        deleteMediaRows(media);
        diaryRepository.deleteAll(diaries);
        diaryRepository.flush();
        media.forEach(m -> fileService.deleteAfterCommit(m.getFileUrl()));
    }

    /** 첨부 행을 지운다. PinLog 클립이 물고 있으면 클립 행을 먼저 지운다. */
    private void deleteMediaRows(List<DiaryMedia> media) {
        if (media.isEmpty()) {
            return;
        }
        pinlogClipRepository.deleteAllByDiaryMediaIdIn(media.stream().map(DiaryMedia::getId).toList());
        diaryMediaRepository.deleteAll(media);
        diaryMediaRepository.flush();
    }

    /**
     * 첨부를 통째로 새 목록으로 바꾼다.
     *
     * <p>사진 한 장만 지우는 API를 따로 두지 않고 "보낸 목록이 곧 최종 상태"로 정한 이유는,
     * 앱에서 편집 화면을 닫을 때 남아 있는 목록을 그대로 보내면 되기 때문이다.
     *
     * <p>새 목록에 그대로 남은 파일은 행을 지우지 않고 순서만 고친다. 전에는 전부 지우고 다시 넣었는데,
     * PinLog 에 쓰인 첨부는 클립이 외래키로 물고 있어서 기록을 고치기만 해도 실패했다.
     * 목록에서 빠진 첨부만 지우고(그 첨부를 쓰던 클립 행도 함께), 파일도 폴더에서 지운다.
     */
    private List<DiaryMedia> replaceMedia(Diary diary, List<DiaryMediaRequest> requests, List<DiaryMedia> current) {
        List<DiaryMediaRequest> wanted = requests == null ? List.of() : requests;
        validate(wanted);

        // 같은 파일 주소의 기존 행은 다시 쓴다 (validate 가 한 목록 안의 중복 주소를 막는다)
        Map<String, Deque<DiaryMedia>> reusable = new LinkedHashMap<>();
        current.forEach(m -> reusable.computeIfAbsent(m.getFileUrl(), url -> new ArrayDeque<>()).add(m));

        LocalDateTime now = LocalDateTime.now();
        List<DiaryMedia> saved = new ArrayList<>();
        for (int i = 0; i < wanted.size(); i++) {
            DiaryMediaRequest request = wanted.get(i);
            Deque<DiaryMedia> same = reusable.get(request.getFileUrl());
            DiaryMedia media = same == null ? null : same.poll();
            if (media == null) {
                String mediaType = fileService.resolveMediaType(request.getFileUrl());
                media = new DiaryMedia();
                media.setDiary(diary);
                media.setMediaType(mediaType);
                media.setFileUrl(request.getFileUrl());
                media.setThumbnailUrl(fileService.thumbnailUrlOf(request.getFileUrl()));
                media.setDurationMs(FileService.MEDIA_TYPE_VIDEO.equals(mediaType) ? request.getDurationMs() : null);
                media.setCreatedAt(now);
            }
            media.setDisplayOrder(i + 1);
            saved.add(media);
        }

        List<DiaryMedia> removed = reusable.values().stream().flatMap(Deque::stream).toList();
        deleteMediaRows(removed);
        diaryMediaRepository.saveAll(saved);

        Set<String> keptUrls = new HashSet<>(saved.stream().map(DiaryMedia::getFileUrl).toList());
        removed.stream()
                .map(DiaryMedia::getFileUrl)
                .filter(url -> !keptUrls.contains(url))
                .forEach(fileService::deleteAfterCommit);

        return saved;
    }

    private void validate(List<DiaryMediaRequest> requests) {
        if (requests.size() > MAX_MEDIA_PER_DIARY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "첨부는 한 기록에 " + MAX_MEDIA_PER_DIARY + "개까지 넣을 수 있습니다.");
        }

        Set<String> urls = new HashSet<>();
        int videoCount = 0;
        for (DiaryMediaRequest request : requests) {
            if (!urls.add(request.getFileUrl())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 파일을 두 번 넣을 수 없습니다.");
            }
            if (FileService.MEDIA_TYPE_VIDEO.equals(fileService.resolveMediaType(request.getFileUrl()))) {
                videoCount++;
            }
        }
        if (videoCount > MAX_VIDEO_PER_DIARY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "영상은 한 장소에 " + MAX_VIDEO_PER_DIARY + "개만 넣을 수 있습니다.");
        }
    }

    /** 기록은 쓴 사람만 고치고 지울 수 있다. 팀원이라도 남의 기록은 못 건드린다. */
    private void requireAuthor(Diary diary, Long userId) {
        tripAccessGuard.requireMember(diary.getItineraryItem().getTrip().getId(), userId);
        if (!diary.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 쓴 기록만 수정·삭제할 수 있습니다.");
        }
    }

    private Diary getDiaryOrThrow(Long diaryId) {
        return diaryRepository.findById(diaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "기록을 찾을 수 없습니다."));
    }

    private ItineraryItem getItemOrThrow(Long itemId) {
        return itineraryItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "핀을 찾을 수 없습니다."));
    }
}
