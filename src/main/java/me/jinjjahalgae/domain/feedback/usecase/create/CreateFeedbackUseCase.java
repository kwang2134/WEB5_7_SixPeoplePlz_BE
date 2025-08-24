package me.jinjjahalgae.domain.feedback.usecase.create;

import me.jinjjahalgae.domain.feedback.usecase.create.dto.CreateFeedbackRequest;

public interface CreateFeedbackUseCase {
    void createFeedback(Long userId, Long proofId, CreateFeedbackRequest req);
}
