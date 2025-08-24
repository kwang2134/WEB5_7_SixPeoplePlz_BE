package me.jinjjahalgae.domain.proof.usecase.create;

import me.jinjjahalgae.domain.proof.usecase.create.dto.ProofCreateRequest;

public interface CreateReProofUseCase {

    void createReProof(ProofCreateRequest request, Long proofId, Long userId);
}
