package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);

    /** 초대할 사람을 아이디로 지목할 때. 탈퇴한 사용자는 찾지 않는다. */
    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    /**
     * 팀원 초대용 사용자 검색. 아이디와 닉네임 중 하나라도 검색어를 포함하면 찾는다.
     *
     * <p>닉네임은 중복될 수 있어 그것만으로는 누구인지 특정할 수 없다. 그래서 <b>검색은 둘 다</b>로 하되,
     * 실제 초대는 유일한 값인 아이디(username)로 지목한다. 화면에는 닉네임과 아이디를 같이 보여줘서
     * 같은 닉네임이 여럿이어도 사용자가 구분할 수 있게 한다.
     */
    @Query("""
            select u from User u
            left join fetch u.avatar
            where u.deletedAt is null
              and (lower(u.username) like lower(concat('%', :keyword, '%'))
                   or lower(u.nickname) like lower(concat('%', :keyword, '%')))
            order by u.username asc
            """)
    List<User> search(@Param("keyword") String keyword, Pageable pageable);

    /** 소셜 로그인(카카오·구글)에서 같은 계정인지 확인할 때. */
    Optional<User> findByProviderAndProviderUid(String provider, String providerUid);
}
