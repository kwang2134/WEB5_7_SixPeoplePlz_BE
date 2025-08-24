package me.jinjjahalgae.domain.contract.usecase.update;

import me.jinjjahalgae.domain.contract.usecase.update.dto.ContractUpdateRequest;

public interface UpdateContractUseCase {
    //계약 수정
    void updateContract(Long userId, Long contractId, ContractUpdateRequest request);
}