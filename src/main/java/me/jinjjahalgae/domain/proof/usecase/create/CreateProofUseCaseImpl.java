package me.jinjjahalgae.domain.proof.usecase.create;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jinjjahalgae.domain.contract.entity.Contract;
import me.jinjjahalgae.domain.contract.enums.ContractStatus;
import me.jinjjahalgae.domain.contract.repository.ContractRepository;
import me.jinjjahalgae.domain.notification.enums.NotificationType;
import me.jinjjahalgae.domain.notification.usecase.listener.event.NotificationEvent;
import me.jinjjahalgae.domain.proof.entities.Proof;
import me.jinjjahalgae.domain.proof.entities.ProofImage;
import me.jinjjahalgae.domain.proof.mapper.ProofImageMapper;
import me.jinjjahalgae.domain.proof.mapper.ProofMapper;
import me.jinjjahalgae.domain.proof.repository.ProofImageRepository;
import me.jinjjahalgae.domain.proof.repository.ProofRepository;
import me.jinjjahalgae.domain.proof.usecase.create.dto.ProofCreateRequest;
import me.jinjjahalgae.global.exception.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateProofUseCaseImpl implements CreateProofUseCase, CreateReProofUseCase {

    private final ProofRepository proofRepository;
    private final ProofImageRepository proofImageRepository;
    private final ContractRepository contractRepository;
    private final ApplicationEventPublisher eventPublisher;


    @Override
    @Transactional
    public void createProof(ProofCreateRequest request, Long contractId, Long userId) {
        Contract contract = getContractWithUser(contractId);

        // 유저의 계약이 아닐 경우 예외
        validateUserContract(contract, userId);

        // 진행중이 아니라면 예외
        validateContractInProgress(contract);

        // 이미지가 1장도 없는 경우 예외
        validateHasImage(request);

        // 원본 인증 요청인데 오늘자 인증이 이미 존재하는 경우 예외
        validateNoTodayProof(contractId);

        // 인증 생성
        Proof proof = ProofMapper.toEntity(request.comment(), contract.getTotalSupervisor(), contractId);

        // 이미지 저장
        saveProofWithImages(proof, request);

        // 알림 전송
        publishEvent(NotificationType.PROOF_ADDED, contractId, userId);
    }

    @Override
    @Transactional
    public void createReProof(ProofCreateRequest request, Long proofId, Long userId) {
        // 이미지가 1장도 없는 경우 예외
        validateHasImage(request);

        // 원본 인증 조회 - 원본 인증이 존재하는 지 검증 겸용
        Proof originalProof = proofRepository.findById(proofId)
                .orElseThrow(() -> ErrorCode.PROOF_NOT_FOUND.domainException(proofId + "에 대한 인증이 존재하지 않습니다."));

        // 계약 조회 - 계약이 존재하는 지 검증 겸용
        Contract contract = getContractWithUser(originalProof.getContractId());

        // 유저의 계약이 아닐 경우 예외
        validateUserContract(contract, userId);

        // 진행중이 아니라면 예외
        validateContractInProgress(contract);

        // 해당 계약에 대해 오늘자 재인증이 존재하는 지 검증
        validateNoTodayReproof(contract.getId());

        // 재인증 저장
        Proof reproof = ProofMapper.toEntity(request.comment(), contract.getTotalSupervisor(), contract.getId(), proofId);

        // 이미지 저장
        saveProofWithImages(reproof, request);

        // 알림 전송
        publishEvent(NotificationType.REPROOF_ADDED, contract.getId(), userId);
    }

    // 계약 찾는 메서드
    private Contract getContractWithUser(Long contractId) {
        return contractRepository.findByIdWithUser(contractId)
                .orElseThrow(() -> ErrorCode.CONTRACT_NOT_FOUND.domainException(contractId + "에 해당하는 계약이 존재하지 않습니다."));
    }

    // 유저 계약인지 확인하는 메서드
    private void validateUserContract(Contract contract, Long userId) {
        if (!contract.getUser().getId().equals(userId)) {
            throw ErrorCode.ACCESS_DENIED.domainException("계약에 대한 접근 권한이 없습니다.");
        }
    }

    // 계약이 진행중인지 확인하는 메서드
    private void validateContractInProgress(Contract contract) {
        if (contract.getStatus() != ContractStatus.IN_PROGRESS) {
            throw ErrorCode.CONTRACT_MUST_IN_PROGRESS.domainException("계약 진행중이 아닙니다.");
        }
    }

    // 이미지가 존재하는지 확인하는 메서드
    private void validateHasImage(ProofCreateRequest request) {
        if (request.firstImageKey() == null) {
            throw ErrorCode.IMAGE_REQUIRED.domainException("이미지가 존재하지 않습니다.");
        }
    }

    // 오늘자 인증이 존재하는지 확인하는 메서드
    private void validateNoTodayProof(Long contractId) {
        Instant[] range = getTodayRange();
        boolean exists = proofRepository.existsByContractIdAndCreatedAtToday(contractId, range[0], range[1]);
        if (exists) {
            throw ErrorCode.PROOF_ALREADY_EXISTS.domainException("오늘자 인증이 이미 존재합니다.");
        }
    }

    // 오늘자 재인증이 존재하는지 확인하는 메서드
    private void validateNoTodayReproof(Long contractId) {
        Instant[] range = getTodayRange();
        boolean exists = proofRepository.existsReProofByContractIdAndCreatedAtToday(contractId, range[0], range[1]);
        if (exists) {
            throw ErrorCode.REPROOF_ALREADY_EXISTS.domainException("오늘자 재인증이 이미 존재합니다.");
        }
    }

    // 사진 저장 메서드
    private void saveProofWithImages(Proof proof, ProofCreateRequest request) {
        Proof savedProof = proofRepository.save(proof);

        // 1 번째 이미지 저장
        ProofImage img1 = ProofImageMapper.toEntity(request.firstImageKey(), 1);
        savedProof.addProofImage(proofImageRepository.save(img1));

        // 2 번째 이미지가 존재하는 경우 저장
        if (request.secondImageKey() != null) {
            ProofImage img2 = ProofImageMapper.toEntity(request.secondImageKey(), 2);
            savedProof.addProofImage(proofImageRepository.save(img2));
        }

        // 3 번째 이미지가 존재하는 경우 저장
        if (request.thirdImageKey() != null) {
            ProofImage img3 = ProofImageMapper.toEntity(request.thirdImageKey(), 3);
            savedProof.addProofImage(proofImageRepository.save(img3));
        }
    }

    // 알림 전송 메서드
    private void publishEvent(NotificationType type, Long contractId, Long userId) {
        eventPublisher.publishEvent(new NotificationEvent(type, contractId, userId));
    }

    private Instant[] getTodayRange() {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zone);
        Instant start = today.atStartOfDay(zone).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(zone).toInstant();
        return new Instant[]{start, end};
    }
}
