package me.jinjjahalgae.domain.invite.usecase.create.invite;

import me.jinjjahalgae.domain.invite.usecase.create.invite.dto.InviteLinkResponse;
import me.jinjjahalgae.domain.user.User;

public interface CreateInviteLinkUseCase {
    InviteLinkResponse createInviteLink(Long contractId, User user);
}
