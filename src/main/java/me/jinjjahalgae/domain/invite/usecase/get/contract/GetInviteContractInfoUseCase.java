package me.jinjjahalgae.domain.invite.usecase.get.contract;

import me.jinjjahalgae.domain.invite.usecase.get.contract.dto.InviteContractInfoResponse;

public interface GetInviteContractInfoUseCase {
    InviteContractInfoResponse getInviteContractInfo(String contractUuid);
}
