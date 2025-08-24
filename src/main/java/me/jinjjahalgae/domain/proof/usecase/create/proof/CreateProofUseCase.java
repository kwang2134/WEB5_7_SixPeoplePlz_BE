package me.jinjjahalgae.domain.proof.usecase.create.proof;

import me.jinjjahalgae.domain.proof.usecase.create.common.ProofCreateRequest;

public interface CreateProofUseCase {

    void createProof(ProofCreateRequest request, Long contractId, Long userId);
}
