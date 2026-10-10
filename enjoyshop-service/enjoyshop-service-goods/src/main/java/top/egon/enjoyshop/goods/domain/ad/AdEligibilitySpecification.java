package top.egon.enjoyshop.goods.domain.ad;

import java.time.Instant;

/** Pure eligibility rule evaluated with one caller-supplied UTC instant per public request. */
public class AdEligibilitySpecification {

    public boolean isEligible(AdBO value, Instant now) {
        return value != null && now != null && value.getDeletedAt() == null
                && value.getStatus() == AdStatusEnum.ENABLED
                && value.getStartAt() != null && !value.getStartAt().isAfter(now)
                && value.getEndAt() != null && value.getEndAt().isAfter(now);
    }
}
