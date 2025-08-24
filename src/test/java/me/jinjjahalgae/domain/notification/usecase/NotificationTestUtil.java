package me.jinjjahalgae.domain.notification.usecase;

import me.jinjjahalgae.domain.contract.entity.Contract;
import me.jinjjahalgae.domain.contract.enums.ContractType;
import me.jinjjahalgae.domain.notification.usecase.create.dto.NotificationCreateRequest;
import me.jinjjahalgae.domain.notification.enums.NotificationType;
import me.jinjjahalgae.domain.participation.enums.Role;
import me.jinjjahalgae.domain.participation.usecase.get.validinfo.ParticipantInfoResponse;
import me.jinjjahalgae.domain.user.User;
import me.jinjjahalgae.domain.user.usecase.common.dto.MyInfoResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class NotificationTestUtil {

    public static User createUser(Long id, String name) {
        return User.builder()
                .id(id)
                .name(name)
                .email(name + "@test.com")
                .build();
    }

    public static MyInfoResponse createMyInfoResponse(Long id, String name) {
        return new MyInfoResponse(
                id,
                name,
                name + "_nick",
                name + "@test.com"
        );
    }

    public static Contract createContract(Long id, User user) {
        Contract contract = Contract.builder()
                .user(user)
                .startDate(Instant.now())
                .endDate(Instant.now().plus(30, ChronoUnit.DAYS))
                .title("테스트 계약")
                .goal("테스트 목표")
                .penalty("테스트 벌칙")
                .reward("테스트 보상")
                .totalProof(21)
                .oneOff(false)
                .type(ContractType.BASIC)
                .build();

        contract.initialize();

        ReflectionTestUtils.setField(contract, "id", id);
        ReflectionTestUtils.setField(contract, "uuid", "contract-uuid-" + id);
        ReflectionTestUtils.setField(contract, "currentProof", 0);
        ReflectionTestUtils.setField(contract, "totalSupervisor", 3);

        return contract;
    }

    public static List<ParticipantInfoResponse> createParticipantList() {
        return List.of(
                new ParticipantInfoResponse("계약자", 1L, Role.CONTRACTOR, true),
                new ParticipantInfoResponse("감독자1", 2L, Role.SUPERVISOR, true),
                new ParticipantInfoResponse("감독자2", 3L, Role.SUPERVISOR, true),
                new ParticipantInfoResponse("감독자3", 4L, Role.SUPERVISOR, true)
        );
    }

    public static List<ParticipantInfoResponse> createParticipantListWithInvalidSupervisor() {
        return List.of(
                new ParticipantInfoResponse("계약자", 1L, Role.CONTRACTOR, true),
                new ParticipantInfoResponse("감독자1", 2L, Role.SUPERVISOR, true),
                new ParticipantInfoResponse("무효한감독자", 3L, Role.SUPERVISOR, false)
        );
    }

    public static NotificationCreateRequest createNotificationRequest(NotificationType type, Long contractId, Long actorUserId) {
        return new NotificationCreateRequest(type, contractId, actorUserId);
    }
} 