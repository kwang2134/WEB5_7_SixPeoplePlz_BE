package me.jinjjahalgae.domain.contract.usecase.get.historylist;

import me.jinjjahalgae.domain.contract.usecase.get.list.dto.ContractListResponse;
import me.jinjjahalgae.domain.contract.usecase.get.historylist.dto.ContractHistoryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface GetContractHistoryListUseCase {

    Page<ContractListResponse> getContractHistory(Long userId, ContractHistoryRequest request, Pageable pageable);
}
