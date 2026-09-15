package com.example.pinkok_backend.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * 테스트용 H2 DB의 모든 테이블을 비운다.
 *
 * <p>테이블끼리 외래키로 이어져 있어서(users ← refresh_tokens 등) deleteAll 을 순서대로 부르면
 * 테이블이 늘어날 때마다 깨진다. 여기서는 외래키 검사를 잠시 끄고 전부 비운 뒤 다시 켠다.
 *
 * <p>@Transactional 테스트 안에서 불러도 롤백되지 않도록 별도 트랜잭션(REQUIRES_NEW)으로 실행한다.
 */
@Component
public class DatabaseCleaner {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public DatabaseCleaner(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void clean() {
        transactionTemplate.executeWithoutResult(status -> {
            List<String> tables = jdbcTemplate.queryForList(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                            + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'",
                    String.class);

            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
            try {
                for (String table : tables) {
                    jdbcTemplate.execute("TRUNCATE TABLE \"" + table + "\" RESTART IDENTITY");
                }
            } finally {
                jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
            }
        });
    }
}
