package me.jinjjahalgae.domain.proof.usecase.get.detail;

import me.jinjjahalgae.domain.proof.usecase.get.detail.dto.ProofDetailResponse;

public interface GetProofDetailUseCase {

    ProofDetailResponse getProofDetail(Long proofId, Long userId);
}
