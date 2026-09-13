package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.AvatarResponse;
import com.example.pinkok_backend.entity.Avatar;
import com.example.pinkok_backend.repository.AvatarRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AvatarService {

    private final AvatarRepository avatarRepository;

    public AvatarService(AvatarRepository avatarRepository) {
        this.avatarRepository = avatarRepository;
    }

    @Transactional(readOnly = true)
    public List<AvatarResponse> getAll() {
        return avatarRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(AvatarResponse::from)
                .toList();
    }

    /** 회원가입 · 아바타 변경에서 공통으로 쓰는 조회. 없는 id 면 400. */
    @Transactional(readOnly = true)
    public Avatar getEntityOrThrow(Long avatarId) {
        return avatarRepository.findById(avatarId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 아바타입니다."));
    }
}
