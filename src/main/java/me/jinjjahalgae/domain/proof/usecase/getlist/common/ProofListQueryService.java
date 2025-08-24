package me.jinjjahalgae.domain.proof.usecase.getlist.common;

import lombok.RequiredArgsConstructor;
import me.jinjjahalgae.domain.proof.entities.Proof;
import me.jinjjahalgae.domain.proof.repository.ProofRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProofListQueryService {

    private final ProofRepository proofRepository;

    public List<Proof> findOriginalProofs(Long contractId, Instant start, Instant end, Long userId, boolean isSupervisor) {
        if (isSupervisor) {
            List<Long> ids = proofRepository.findOriginalProofIdsByMonthForSupervisor(contractId, start, end, userId);
            return proofRepository.findProofsWithProofImagesByIds(ids);
        } else {
            List<Long> ids = proofRepository.findOriginalProofIdsByMonth(contractId, start, end);
            return proofRepository.findProofsWithProofImagesByIds(ids);
        }
    }

    public List<Proof> findReProofs(Long contractId, List<Long> originalProofIds, Long userId, boolean isSupervisor) {
        if (isSupervisor) {
            List<Long> ids = proofRepository.findReProofIdsByMonthForSupervisor(contractId, originalProofIds, userId);
            return proofRepository.findProofsWithProofImagesByIds(ids);
        } else {
            List<Long> ids = proofRepository.findReProofIdsByMonth(contractId, originalProofIds);
            return proofRepository.findProofsWithProofImagesByIds(ids);
        }
    }

    public Map<Long, Proof> mapReproofs(List<Proof> reproofs) {
        return reproofs.stream()
                .collect(Collectors.toMap(Proof::getProofId, Function.identity()));
    }
}
