package me.jinjjahalgae.domain.proof.usecase.getlist.contractorlist;

import me.jinjjahalgae.domain.proof.usecase.getlist.contractorlist.dto.ContractorProofListResponse;

import java.util.List;

public interface GetContractorProofListUseCase {

    List<ContractorProofListResponse> getContractorProofList(Long contractId, Integer year, Integer month, Long userId);
}
