package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 최근 것부터. 알림함은 보통 최근 몇십 개만 보므로 전체를 다 읽지 않는다. */
    List<Notification> findTop100ByUser_IdOrderByCreatedAtDesc(Long userId);

    /** 탭에 띄울 "안 읽은 개수" 뱃지용. */
    int countByUser_IdAndIsReadFalse(Long userId);

    /**
     * 안 읽은 알림을 한 번에 읽음 처리한다.
     * 하나씩 불러와 고치면 알림이 쌓였을 때 느려져서, DB에서 한 번에 바꾼다.
     */
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.isRead = true, n.readAt = :now "
            + "where n.user.id = :userId and n.isRead = false")
    int markAllRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
