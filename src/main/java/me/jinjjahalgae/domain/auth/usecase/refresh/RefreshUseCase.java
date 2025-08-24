package me.jinjjahalgae.domain.auth.usecase.refresh;

import me.jinjjahalgae.domain.auth.usecase.refresh.dto.RefreshRequest;
import me.jinjjahalgae.domain.auth.usecase.refresh.dto.RefreshResponse;

public interface RefreshUseCase {
    RefreshResponse refresh(RefreshRequest request);
} 