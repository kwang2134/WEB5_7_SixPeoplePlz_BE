package me.jinjjahalgae.domain.contract.usecase.create;

import me.jinjjahalgae.domain.contract.usecase.create.dto.CreateContractRequest;
import me.jinjjahalgae.domain.contract.usecase.create.dto.CreateContractResponse;

public interface CreateContractUseCase {
    CreateContractResponse createContract(Long userId, CreateContractRequest request);
}
