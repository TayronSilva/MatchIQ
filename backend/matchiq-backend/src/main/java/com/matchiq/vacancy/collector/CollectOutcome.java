package com.matchiq.vacancy.collector;

import java.util.List;

public record CollectOutcome(
        List<CollectorResult> results,
        int newJobs,
        boolean cooldownActive,
        long cooldownRemainingMinutes
) {
    public static CollectOutcome cooldown(long remainingMinutes) {
        return new CollectOutcome(List.of(), 0, true, remainingMinutes);
    }
}
