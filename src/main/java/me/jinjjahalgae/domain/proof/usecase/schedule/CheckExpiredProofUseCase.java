package me.jinjjahalgae.domain.proof.usecase.schedule;

import java.time.Instant;

public interface CheckExpiredProofUseCase {

    void checkExpiredProof(Instant now);
}
