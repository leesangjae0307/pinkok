package com.example.pinkok_backend.gemini;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * gemini-2.5-flash 대략적인 단가로 비용을 추정한다. 실제 청구서와는 차이가 있을 수 있으니
 * (Google이 가격을 바꿀 수 있음) 참고용 추정치다 — 예산 관리용으로 "대충 얼마나 썼는지" 보는 용도.
 * 나중에 https://ai.google.dev/pricing 에서 실제 단가로 업데이트할 것.
 */
public final class GeminiPricing {

    private static final BigDecimal INPUT_PRICE_PER_MILLION_TOKENS = new BigDecimal("0.10");
    private static final BigDecimal OUTPUT_PRICE_PER_MILLION_TOKENS = new BigDecimal("0.40");
    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    private GeminiPricing() {
    }

    public static BigDecimal estimateUsd(Integer inputTokens, Integer outputTokens) {
        BigDecimal cost = perMillion(inputTokens, INPUT_PRICE_PER_MILLION_TOKENS)
                .add(perMillion(outputTokens, OUTPUT_PRICE_PER_MILLION_TOKENS));
        return cost.setScale(6, RoundingMode.HALF_UP);
    }

    private static BigDecimal perMillion(Integer tokens, BigDecimal pricePerMillion) {
        if (tokens == null || tokens <= 0) {
            return BigDecimal.ZERO;
        }
        return pricePerMillion.multiply(BigDecimal.valueOf(tokens))
                .divide(ONE_MILLION, 10, RoundingMode.HALF_UP);
    }
}
