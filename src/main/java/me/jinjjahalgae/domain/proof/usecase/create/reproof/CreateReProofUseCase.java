package me.jinjjahalgae.domain.proof.usecase.create.reproof;

import me.jinjjahalgae.domain.proof.usecase.create.common.ProofCreateRequest;

public interface CreateReProofUseCase {

    void createReProof(ProofCreateRequest request, Long proofId, Long userId);
}
