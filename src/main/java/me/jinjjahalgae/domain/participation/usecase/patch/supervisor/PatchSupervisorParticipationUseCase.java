package me.jinjjahalgae.domain.participation.usecase.patch.supervisor;

import me.jinjjahalgae.domain.user.User;

public interface PatchSupervisorParticipationUseCase {
    void patchSupervisorParticipation(Long contractId, User user);
}
