package me.jinjjahalgae.domain.contract.usecase.get.preview;

import me.jinjjahalgae.domain.contract.usecase.get.preview.dto.ContractPreviewResponse;

public interface GetContractPreviewUseCase {
    //계약 상세 조회
    ContractPreviewResponse getContractPreview(Long userId, Long contractId);
}
