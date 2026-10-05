package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.TravelStyleResponse;
import com.example.pinkok_backend.repository.TravelStyleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TravelStyleService {

    private final TravelStyleRepository travelStyleRepository;

    public TravelStyleService(TravelStyleRepository travelStyleRepository) {
        this.travelStyleRepository = travelStyleRepository;
    }

    @Transactional(readOnly = true)
    public List<TravelStyleResponse> list() {
        return travelStyleRepository.findAll().stream().map(TravelStyleResponse::from).toList();
    }
}
