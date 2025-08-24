package me.jinjjahalgae.domain.proof.usecase.create;

import me.jinjjahalgae.domain.proof.usecase.create.dto.ProofCreateRequest;

public interface CreateProofUseCase {

    void createProof(ProofCreateRequest request, Long contractId, Long userId);
}
