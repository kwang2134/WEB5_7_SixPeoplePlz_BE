package me.jinjjahalgae.domain.contract.usecase.delete;

public interface WithdrawContractUseCase {
    //계약 중도 포기
    void withdrawContract(Long userId, Long contractId);
}
