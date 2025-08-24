package me.jinjjahalgae.domain.contract.usecase.process;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jinjjahalgae.domain.contract.entity.Contract;
import me.jinjjahalgae.domain.contract.enums.ContractStatus;
import me.jinjjahalgae.domain.contract.repository.ContractRepository;
import me.jinjjahalgae.domain.notification.enums.NotificationType;
import me.jinjjahalgae.domain.notification.model.NotificationData;
import me.jinjjahalgae.domain.notification.usecase.listener.event.NotificationBatchEvent;
import me.jinjjahalgae.domain.proof.enums.ProofStatus;
import me.jinjjahalgae.domain.proof.repository.ProofRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EndContractUseCaseImpl_temp implements EndContractsUseCase, EndOneOffContractUseCase{

    private final ContractRepository contractRepository;
    private final ProofRepository proofRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void endContracts() {
        log.info("계약 종료 처리를 시작합니다.");
        Instant deadline = Instant.now().minus(24, ChronoUnit.HOURS);
        // 어제 또는 이전에 종료되었어야 하는 '진행중' 또는 '결과 대기' 상태의 계약을 모두 조회
        List<Contract> contracts = contractRepository.findContractsToEndBefore(
                List.of(ContractStatus.IN_PROGRESS, ContractStatus.WAIT_RESULT), deadline);
        log.info("종료일이 된 계약 {}건을 발견했습니다.", contracts.size());

        if (contracts.isEmpty()) return;

        processContracts(contracts, true);
        log.info("계약 종료 처리를 성공적으로 완료했습니다.");
    }

    @Override
    @Transactional
    public void endOneOffContract() {
        Instant deadline = Instant.now().minus(24, ChronoUnit.HOURS);

        List<Contract> contracts = contractRepository.findEndedOneOffContracts(deadline);

        if (contracts.isEmpty()) return;

        processContracts(contracts, false);
    }

    private void processContracts(List<Contract> contracts, boolean isGeneral) {
        List<Contract> completeContracts = new ArrayList<>();
        List<Contract> waitContracts = new ArrayList<>();
        List<Contract> failContracts = new ArrayList<>();

        Instant twentyFourHours = Instant.now().minus(24, ChronoUnit.HOURS);

        for (Contract contract : contracts) {
            boolean hasPendingProofs = proofRepository.existsByContractIdAndStatus(contract.getId(), ProofStatus.APPROVE_PENDING);

            int currentProof = contract.getCurrentProof();
            int totalProof = contract.getTotalProof();

            int reProofableCount = isGeneral ? proofRepository.countRecentRejectedProofs(contract.getId(), twentyFourHours) : 0;

            if (currentProof >= totalProof && !hasPendingProofs) {
                // 성공 확정 (이미 목표를 달성했고, 처리 대기중인 인증도 없는 경우)
                completeContracts.add(contract);
            } else if (currentProof + reProofableCount < totalProof && !hasPendingProofs) {
                // 실패 확정 (목표 미달성, 재인증 가능성 없음, 처리 대기 인증도 없는 경우)
                failContracts.add(contract);
            } else {
                // 결과가 애매한 경우 'WAIT_RESULT' 상태로 변경해 하루 더 유예
                waitContracts.add(contract);
            }
        }

        log.info("종료 계약 분류 결과: [성공: {}건], [실패: {}건], [결과 대기: {}건]", completeContracts.size(), failContracts.size(), waitContracts.size());

        // 결과 대기 계약 일괄 업데이트
        updateAndNotify(completeContracts, ContractStatus.COMPLETED, NotificationType.CONTRACT_ENDED_SUCCESS);
        updateAndNotify(failContracts, ContractStatus.FAILED, NotificationType.CONTRACT_ENDED_FAIL);
        updateOnly(waitContracts, ContractStatus.WAIT_RESULT);
    }


    private void updateAndNotify(List<Contract> contracts, ContractStatus status, NotificationType notificationType) {
        if(contracts.isEmpty()) return;
        List<Long> ids = contracts.stream().map(Contract::getId).toList();
        contractRepository.bulkUpdateStatus(ids, status);

        List<NotificationData> notificationDataList = contracts.stream()
                .map(c -> new NotificationData(c.getId(), c.getUser().getId()))
                .toList();

        eventPublisher.publishEvent(new NotificationBatchEvent(notificationType, notificationDataList));
    }

    private void updateOnly(List<Contract> contracts, ContractStatus status) {
        if(contracts.isEmpty()) return;
        List<Long> ids = contracts.stream().map(Contract::getId).toList();
        contractRepository.bulkUpdateStatus(ids, status);
    }
}
