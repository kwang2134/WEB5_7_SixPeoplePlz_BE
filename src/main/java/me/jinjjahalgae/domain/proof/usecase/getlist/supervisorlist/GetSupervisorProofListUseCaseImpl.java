package me.jinjjahalgae.domain.proof.usecase.getlist.supervisorlist;

import lombok.RequiredArgsConstructor;
import me.jinjjahalgae.domain.contract.repository.ContractRepository;
import me.jinjjahalgae.domain.feedback.entity.Feedback;
import me.jinjjahalgae.domain.feedback.enums.FeedbackStatus;
import me.jinjjahalgae.domain.feedback.repository.FeedbackRepository;
import me.jinjjahalgae.domain.participation.repository.ParticipationRepository;
import me.jinjjahalgae.domain.proof.entities.Proof;
import me.jinjjahalgae.domain.proof.mapper.ProofMapper;
import me.jinjjahalgae.domain.proof.repository.ProofRepository;
import me.jinjjahalgae.domain.proof.usecase.getlist.common.ProofListQueryService;
import me.jinjjahalgae.domain.proof.usecase.getlist.supervisorlist.dto.SupervisorProofListResponse;
import me.jinjjahalgae.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetSupervisorProofListUseCaseImpl implements GetSupervisorProofListUseCase {

    private final ProofRepository proofRepository;
    private final ContractRepository contractRepository;
    private final FeedbackRepository feedbackRepository;
    private final ParticipationRepository participationRepository;
    private final ProofListQueryService proofListQueryService;

    @Override
    @Transactional(readOnly = true)
    public List<SupervisorProofListResponse> getSupervisorProofList(Long contractId, Integer year, Integer month, Long userId) {
        // 년, 월 값에 대한 검증
        if(year == null || month == null) {
            throw ErrorCode.INVALID_YEAR_MONTH.domainException("년, 월 값이 비어있거나 올바르지 않습니다.");
        }

        // 계약이 존재하는 지 확인
        boolean isContractExist = contractRepository.existsById(contractId);

        // 계약이 존재하지 않다면 예외 발생
        if(!isContractExist) {
            throw ErrorCode.CONTRACT_NOT_FOUND.domainException(contractId + "에 대한 계약이 존재하지 않습니다.");
        }

        // 유저가 계약의 참여자인지 확인
        boolean isUserParticipate = participationRepository.existsByContractIdAndUserId(contractId, userId);

        // 유저가 계약의 참여자가 아닐 경우 예외
        if (!isUserParticipate) {
            throw ErrorCode.ACCESS_DENIED.domainException("계약에 대한 접근 권한이 없습니다.");
        }

        ZoneId zone = ZoneId.of("Asia/Seoul");

        // 달의 시작일 00:00:00
        Instant startDate = LocalDateTime.of(year, month, 1, 0, 0).atZone(zone).toInstant();

        // 달의 마지막 날 23:59:59.999999999
        Instant endDate = LocalDateTime.of(year, month, YearMonth.of(year, month).lengthOfMonth(), 23, 59, 59, 999999999).atZone(zone).toInstant();

        // 원본 인증 조회
        List<Proof> proofs = proofListQueryService.findOriginalProofs(contractId, startDate, endDate, userId, true);
        List<Long> originalIds = proofs.stream().map(Proof::getId).toList();

        // 재인증 조회 및 매핑
        List<Proof> reProofs = proofListQueryService.findReProofs(contractId, originalIds, userId, true);
        Map<Long, Proof> reProofMap = proofListQueryService.mapReproofs(reProofs);

        // 감독자가 해당 계약에서 처리한 피드백들
        List<Feedback> feedbacks = feedbackRepository.findByContractIdAndUserId(contractId, userId);

        // 인증 id를 key로 피드백 상태를 Map에 매핑
        Map<Long, FeedbackStatus> feedbackMap = feedbacks.stream()
                .collect(Collectors.toMap(
                        feedback -> feedback.getProof().getId(),
                        Feedback::getStatus
                ));

        // 응답 dto 변환
        return proofs.stream()
                .map(org -> getSupervisorProofListResponse(org, reProofMap, feedbackMap))
                .toList();
    }

    private SupervisorProofListResponse getSupervisorProofListResponse(Proof org, Map<Long, Proof> reProofMap, Map<Long, FeedbackStatus> feedbackMap) {
        // 원본 인증 id로 얻은 재인증 (없으면 null)
        Proof reProof = reProofMap.get(org.getId());

        // 원본 인증 id로 얻은 피드백 상태
        FeedbackStatus orgFeedbackStatus = feedbackMap.get(org.getId());

        // 재인증 피드백 상태
        FeedbackStatus reProofFeedbackStatus = null;

        // 재인증이 아니라면 상태를 매핑
        if (reProof != null) {
            reProofFeedbackStatus = feedbackMap.get(reProof.getId());
        }
        return ProofMapper.toSupervisorListResponse(org, reProof, orgFeedbackStatus, reProofFeedbackStatus);
    }
}
