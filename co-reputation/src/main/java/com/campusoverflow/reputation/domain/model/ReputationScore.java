package com.campusoverflow.reputation.domain.model;

import com.campusoverflow.shared.domain.InvariantViolationException;

/**
 * 声誉分（统一语言术语 #26）：整数且永不低于 1（{@code INV-R-01}）。
 */
public record ReputationScore(int value) {

    public static final int MIN = 1;
    public static final String INVARIANT_CODE = "INV-R-01";

    public ReputationScore {
        if (value < MIN) {
            throw new InvariantViolationException(INVARIANT_CODE, "声誉分不能低于 " + MIN);
        }
    }

    public static ReputationScore initial() {
        return new ReputationScore(MIN);
    }

    /** 加减分后自动夹在最小值上，扣分不会写出负数。 */
    public ReputationScore plus(int delta) {
        return new ReputationScore(Math.max(MIN, Math.addExact(value, delta)));
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
