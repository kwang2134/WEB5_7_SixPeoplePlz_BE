package me.jinjjahalgae.domain.proof.usecase.getlist.contractorlist;

import lombok.RequiredArgsConstructor;
import me.jinjjahalgae.domain.contract.entity.Contract;
import me.jinjjahalgae.domain.contract.repository.ContractRepository;
import me.jinjjahalgae.domain.proof.entities.Proof;
import me.jinjjahalgae.domain.proof.mapper.ProofMapper;
import me.jinjjahalgae.domain.proof.repository.ProofRepository;
import me.jinjjahalgae.domain.proof.usecase.getlist.common.ProofListQueryService;
import me.jinjjahalgae.domain.proof.usecase.getlist.contractorlist.dto.ContractorProofListResponse;
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
public class GetContractorProofListUseCaseImpl implements GetContractorProofListUseCase {

    private final ProofRepository proofRepository;
    private final ContractRepository contractRepository;
    private final ProofListQueryService proofListQueryService;

    @Override
    @Transactional(readOnly = true)
    public List<ContractorProofListResponse> getContractorProofList(Long contractId, Integer year, Integer month, Long userId) {
        // 년, 월 값에 대한 검증
        if(year == null || month == null) {
            throw ErrorCode.INVALID_YEAR_MONTH.domainException("년, 월 값이 비어있거나 올바르지 않습니다.");
        }

        // 종료일을 위해 계약을 가져옴
        Contract contract = contractRepository.findByIdWithUser(contractId)
                .orElseThrow(() -> ErrorCode.CONTRACT_NOT_FOUND.domainException(contractId + "에 대한 계약이 존재하지 않습니다."));

        // 계약자인지 확인
        contract.validateContractor(userId);

        ZoneId zone = ZoneId.of("Asia/Seoul");

        // 달의 시작일 00:00:00
        Instant startDate = LocalDateTime.of(year, month, 1, 0, 0).atZone(zone).toInstant();

        // 달의 마지막 날 23:59:59.999999999
        Instant endDate = LocalDateTime.of(year, month, YearMonth.of(year, month).lengthOfMonth(), 23, 59, 59, 999999999).atZone(zone).toInstant();

        // 원본 인증 조회
        List<Proof> proofs = proofListQueryService.findOriginalProofs(contractId, startDate, endDate, userId, false);
        List<Long> originalIds = proofs.stream().map(Proof::getId).toList();

        // 재인증 조회
        List<Proof> reProofs = proofListQueryService.findReProofs(contractId, originalIds, userId, false);
        Map<Long, Proof> reProofMap = proofListQueryService.mapReproofs(reProofs);


        // 인증과 재인증을 하나의 응답으로 매핑
        return proofs.stream()
                .map(org -> {
                    Proof reProof = reProofMap.get(org.getId());
                    return ProofMapper.toContractorListResponse(org, reProof, contract.getEndDate());
                })
                .toList();
    }
}
