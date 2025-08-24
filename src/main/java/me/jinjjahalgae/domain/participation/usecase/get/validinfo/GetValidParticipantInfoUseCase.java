package me.jinjjahalgae.domain.participation.usecase.get.validinfo;

import java.util.List;

public interface GetValidParticipantInfoUseCase {

    List<ParticipantInfoResponse> getValidParticipationInfo(long contractId);
}
