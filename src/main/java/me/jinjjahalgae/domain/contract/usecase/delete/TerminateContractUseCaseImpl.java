package me.jinjjahalgae.domain.contract.usecase.delete;

import lombok.RequiredArgsConstructor;
import me.jinjjahalgae.domain.contract.entity.Contract;
import me.jinjjahalgae.domain.contract.repository.ContractRepository;
import me.jinjjahalgae.global.exception.ErrorCode;
import me.jinjjahalgae.global.storage.redis.usecase.invite.delete.DeleteInviteInfoUseCaseImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TerminateContractUseCaseImpl implements CancelContractUseCase, WithdrawContractUseCase{

    private final ContractRepository contractRepository;
    private final DeleteInviteInfoUseCaseImpl deleteInviteInfoUseCase;
    
    @Override
    @Transactional
    public void cancelContract(Long userId, Long contractId) {
        Contract contract = validContract(userId, contractId);

        //계약이 시작했는가 (계약 취소는 대기 상태에서만 가능) + 그렇다면 취소 및 삭제
        contract.cancel();
        contractRepository.delete(contract);

        //남아있는 초대 정보 삭제
        deleteInviteInfoUseCase.execute(contractId);
    }

    @Override
    @Transactional
    public void withdrawContract(Long userId, Long contractId) {
        Contract contract = validContract(userId, contractId);

        //계약이 진행중인가? (중도 포기는 진행중인 계약만 가능)
        //계약 상태가 진해중이 아니면 예외, 진행중이면 계약 상태 변경 (진행중 -> 포기)
        contract.withdraw();
    }

    private Contract validContract(Long userId, Long contractId) {
        //유저 확인
        //권한 확인 -> 기존 다른 확인 부분과 같음
        Contract contract = contractRepository.findByIdWithUser(contractId)
                .orElseThrow(() -> ErrorCode.CONTRACT_NOT_FOUND.domainException("존재하지 않는 계약입니다."));

        //유저 검증 -> 계약자인가?
        contract.validateContractor(userId);
        return contract;
    }
}
