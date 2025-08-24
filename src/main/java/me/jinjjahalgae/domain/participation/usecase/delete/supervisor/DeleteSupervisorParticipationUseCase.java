package me.jinjjahalgae.domain.participation.usecase.delete.supervisor;

import me.jinjjahalgae.domain.user.User;

public interface DeleteSupervisorParticipationUseCase {
    void deleteSupervisorParticipation(Long contractId, User user);
}
