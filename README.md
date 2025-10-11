# 🔧 리팩토링 개요 및 목적

초기 프로젝트는 **유스케이스 기반 구조**로, 각 유스케이스마다 `execute()` 메서드를 가진 단일 인터페이스-구현체 패턴을 사용했습니다.  
그러나 운영 과정에서 다음과 같은 문제점이 드러났습니다.

- 유스케이스 간 **공통 로직 중복** 다수 발생  
- 인터페이스가 **실질적 추상화 역할을 수행하지 못함**  
- 메서드 명 `execute()`의 **모호성**으로 기능 파악이 어려움  

이에 따라, **중복 코드 통합**, **명확한 메서드 네이밍**, **공통 로직 분리**를 목표로 리팩토링을 진행했습니다.


# 🧩 리팩토링 주요 작업

### ✅ 1. 중복 공통 로직 통합 및 내부 메서드 추출
- 유사한 비즈니스 로직(계약 검증, 알림, 이미지 저장 등)을 **내부 메서드로 일원화**
- **다중 인터페이스 구현**을 통해 중복 구현체 제거 및 재사용성 강화

### ✅ 2. 명확한 메서드 네이밍
- 기존 `execute()` → 역할 중심 메서드명으로 변경  
  예: `cancelContract()`, `createProof()`, `endContracts()`

### ✅ 3. 공통 쿼리 로직 서비스 분리
- 계약자용 / 감독자용 조회 로직에서 중복된 **데이터 조회·매핑 로직을 QueryService로 분리**
- 조회 유스케이스는 **권한 검증 및 응답 변환에만 집중**


# 🧱 주요 리팩토링 사례 및 효과

## 1️⃣ 계약 취소 / 중도포기 유스케이스 통합  
**Before:**  
- `CancelContractUseCaseImpl`, `WithdrawContractUseCaseImpl` — 중복된 검증·권한 로직 존재  
- 메서드명이 모두 `execute()`로 구분 어려움  

**After:**  
- `TerminateContractUseCaseImpl` 단일 클래스에서 두 인터페이스 구현  
- 공통 검증 로직을 `validContract()`로 통합  
- 취소(`cancelContract`) / 포기(`withdrawContract`) 처리 로직 분리  

**핵심 코드**
```java
@Service
@RequiredArgsConstructor
public class TerminateContractUseCaseImpl implements CancelContractUseCase, WithdrawContractUseCase {

    private final ContractRepository contractRepository;
    private final DeleteInviteInfoUseCaseImpl deleteInviteInfoUseCase;

    @Override
    @Transactional
    public void cancelContract(Long userId, Long contractId) {
        Contract contract = validContract(userId, contractId);
        contract.cancel();
        contractRepository.delete(contract);
        deleteInviteInfoUseCase.execute(contractId);
    }

    @Override
    @Transactional
    public void withdrawContract(Long userId, Long contractId) {
        Contract contract = validContract(userId, contractId);
        contract.withdraw();
    }

    /**
     * 공통 유효성 검증 로직 통합
     */
    private Contract validContract(Long userId, Long contractId) {
        Contract contract = contractRepository.findByIdWithUser(contractId)
                .orElseThrow(() -> ErrorCode.CONTRACT_NOT_FOUND.domainException("존재하지 않는 계약입니다."));
        contract.validateContractor(userId);
        return contract;
    }
}
```

**✨ 효과:**  
- 중복 코드 제거 (10줄 내외 공통 로직 일원화)  
- 유지보수성, 확장성 향상  
- 명확한 역할 구분 및 테스트 용이성 확보  


## 2️⃣ 일반 / 단건 계약 종료 유스케이스 통합  
**Before:**  
- `EndContractsUseCaseImpl`, `EndSingleContractUseCaseImpl` 등 별도 구현  
- 상태 분류, 알림 발송 등 중복 배치 로직 존재  

**After:**
- `EndContractUseCaseImpl_temp` 하나의 구현체에서 두 인터페이스 모두 구현  
- 공통 로직(`processContracts`, `updateAndNotify`, `updateOnly`) 내부 메서드로 일원화  
- 타입별 차이는 `isGeneral` 플래그로 분기 처리

**핵심 코드**
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class EndContractUseCaseImpl implements EndContractsUseCase, EndOneOffContractUseCase {

    private final ContractRepository contractRepository;
    private final ProofRepository proofRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void endContracts() {
        Instant deadline = Instant.now().minus(24, ChronoUnit.HOURS);
        List<Contract> contracts = contractRepository.findContractsToEndBefore(
                List.of(ContractStatus.IN_PROGRESS, ContractStatus.WAIT_RESULT), deadline);

        if (contracts.isEmpty()) return;
        processContracts(contracts, true);
    }

    @Override
    @Transactional
    public void endOneOffContract() {
        Instant deadline = Instant.now().minus(24, ChronoUnit.HOURS);
        List<Contract> contracts = contractRepository.findEndedOneOffContracts(deadline);

        if (contracts.isEmpty()) return;
        processContracts(contracts, false);
    }

    /** 공통 종료 처리 메서드 */
    private void processContracts(List<Contract> contracts, boolean isGeneral) {
        List<Contract> complete = new ArrayList<>();
        List<Contract> wait = new ArrayList<>();
        List<Contract> fail = new ArrayList<>();

        Instant checkTime = Instant.now().minus(24, ChronoUnit.HOURS);

        for (Contract c : contracts) {
            boolean hasPendingProof = proofRepository.existsByContractIdAndStatus(c.getId(), ProofStatus.APPROVE_PENDING);
            int reProofableCount = isGeneral ? proofRepository.countRecentRejectedProofs(c.getId(), checkTime) : 0;

            if (c.getCurrentProof() >= c.getTotalProof() && !hasPendingProof)
                complete.add(c);
            else if (c.getCurrentProof() + reProofableCount < c.getTotalProof() && !hasPendingProof)
                fail.add(c);
            else
                wait.add(c);
        }

        updateAndNotify(complete, ContractStatus.COMPLETED, NotificationType.CONTRACT_ENDED_SUCCESS);
        updateAndNotify(fail, ContractStatus.FAILED, NotificationType.CONTRACT_ENDED_FAIL);
        updateOnly(wait, ContractStatus.WAIT_RESULT);
    }
}

```

**✨ 효과:**  
- 클래스 2개 → 1개로 통합, 약 **200 → 120라인**으로 축소  
- 배치성 로직 재사용성 강화  
- 신규 계약 상태/정책 추가 시 단일 파일에서 관리 가능  


## 3️⃣ 인증 / 재인증 생성 유스케이스 통합  
**Before:**  
- `CreateProofUseCaseImpl`, `CreateReProofUseCaseImpl` 두 구현체  
- 이미지 저장, 계약 검증, 알림 등 로직 중복  

**After:**  
- 단일 `CreateProofUseCaseImpl`에서 두 인터페이스 모두 구현  
- 핵심 프로세스를 `createProof`, `createReProof`로 분리  
- 공통 로직(검증, 이미지 저장, 알림)을 내부 메서드로 추출

**핵심 코드**
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateProofUseCaseImpl implements CreateProofUseCase, CreateReProofUseCase {

    private final ProofRepository proofRepository;
    private final ProofImageRepository proofImageRepository;
    private final ContractRepository contractRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** ✅ 인증 생성 */
    @Override
    @Transactional
    public void createProof(ProofCreateRequest request, Long contractId, Long userId) {
        Contract contract = getContractWithUser(contractId);
        validateUserContract(contract, userId);
        validateContractInProgress(contract);
        validateHasImage(request);
        validateNoTodayProof(contractId);

        Proof proof = ProofMapper.toEntity(request.comment(), contract.getTotalSupervisor(), contractId);
        saveProofWithImages(proof, request);
        publishEvent(NotificationType.PROOF_ADDED, contractId, userId);
    }

    /** ✅ 재인증 생성 */
    @Override
    @Transactional
    public void createReProof(ProofCreateRequest request, Long proofId, Long userId) {
        validateHasImage(request);

        Proof originalProof = proofRepository.findById(proofId)
                .orElseThrow(() -> ErrorCode.PROOF_NOT_FOUND.domainException(proofId + "에 대한 인증이 존재하지 않습니다."));

        Contract contract = getContractWithUser(originalProof.getContractId());
        validateUserContract(contract, userId);
        validateContractInProgress(contract);
        validateNoTodayReproof(contract.getId());

        Proof reproof = ProofMapper.toEntity(request.comment(), contract.getTotalSupervisor(), contract.getId(), proofId);
        saveProofWithImages(reproof, request);
        publishEvent(NotificationType.REPROOF_ADDED, contract.getId(), userId);
    }

    /** 공통 검증 및 저장 메서드들 */
    private Contract getContractWithUser(Long contractId) {
        return contractRepository.findByIdWithUser(contractId)
                .orElseThrow(() -> ErrorCode.CONTRACT_NOT_FOUND.domainException(contractId + "에 해당하는 계약이 존재하지 않습니다."));
    }

    private void validateUserContract(Contract contract, Long userId) {
        if (!contract.getUser().getId().equals(userId))
            throw ErrorCode.ACCESS_DENIED.domainException("계약에 대한 접근 권한이 없습니다.");
    }

    private void validateContractInProgress(Contract contract) {
        if (contract.getStatus() != ContractStatus.IN_PROGRESS)
            throw ErrorCode.CONTRACT_MUST_IN_PROGRESS.domainException("계약 진행중이 아닙니다.");
    }

    private void validateHasImage(ProofCreateRequest request) {
        if (request.firstImageKey() == null)
            throw ErrorCode.IMAGE_REQUIRED.domainException("이미지가 존재하지 않습니다.");
    }

    private void validateNoTodayProof(Long contractId) {
        Instant[] range = getTodayRange();
        if (proofRepository.existsByContractIdAndCreatedAtToday(contractId, range[0], range[1]))
            throw ErrorCode.PROOF_ALREADY_EXISTS.domainException("오늘자 인증이 이미 존재합니다.");
    }

    private void validateNoTodayReproof(Long contractId) {
        Instant[] range = getTodayRange();
        if (proofRepository.existsReProofByContractIdAndCreatedAtToday(contractId, range[0], range[1]))
            throw ErrorCode.REPROOF_ALREADY_EXISTS.domainException("오늘자 재인증이 이미 존재합니다.");
    }

    private void saveProofWithImages(Proof proof, ProofCreateRequest request) {
        Proof saved = proofRepository.save(proof);

        ProofImage img1 = ProofImageMapper.toEntity(request.firstImageKey(), 1);
        saved.addProofImage(proofImageRepository.save(img1));

        if (request.secondImageKey() != null)
            saved.addProofImage(proofImageRepository.save(ProofImageMapper.toEntity(request.secondImageKey(), 2)));

        if (request.thirdImageKey() != null)
            saved.addProofImage(proofImageRepository.save(ProofImageMapper.toEntity(request.thirdImageKey(), 3)));
    }

    private void publishEvent(NotificationType type, Long contractId, Long userId) {
        eventPublisher.publishEvent(new NotificationEvent(type, contractId, userId));
    }

    private Instant[] getTodayRange() {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zone);
        Instant start = today.atStartOfDay(zone).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(zone).toInstant();
        return new Instant[]{start, end};
    }
}

```

**✨ 효과:**  
- 약 40~50줄 중복 제거  
- 재인증 규칙 변경 시 단일 파일 수정만으로 대응  
- 테스트 및 유지보수 용이성 대폭 향상  


## 4️⃣ 인증 목록 조회(계약자/감독자) – 공통 쿼리 서비스 분리  
**Before:**  
- `GetContractorProofListUseCaseImpl`, `GetSupervisorProofListUseCaseImpl` 각각 별도 클래스  
- 월별 원본/재인증 목록 조회 및 매핑 로직 중복  

**After:**  
- `ProofListQueryService` 신설  
- `findOriginalProofs`, `findReProofs`, `mapReproofs` 등 공통 데이터 로직 일원화  
- 각 유스케이스는 쿼리 서비스 호출 및 응답 변환만 담당

**핵심 코드**
```java
@Component
@RequiredArgsConstructor
public class ProofListQueryService {

    private final ProofRepository proofRepository;

    /** 원본 인증 조회 (감독자/계약자 공통) */
    public List<Proof> findOriginalProofs(Long contractId, Instant start, Instant end, Long userId, boolean isSupervisor) {
        List<Long> ids = isSupervisor
                ? proofRepository.findOriginalProofIdsByMonthForSupervisor(contractId, start, end, userId)
                : proofRepository.findOriginalProofIdsByMonth(contractId, start, end);

        return proofRepository.findProofsWithProofImagesByIds(ids);
    }

    /** 재인증 조회 (감독자/계약자 공통) */
    public List<Proof> findReProofs(Long contractId, List<Long> originalProofIds, Long userId, boolean isSupervisor) {
        List<Long> ids = isSupervisor
                ? proofRepository.findReProofIdsByMonthForSupervisor(contractId, originalProofIds, userId)
                : proofRepository.findReProofIdsByMonth(contractId, originalProofIds);

        return proofRepository.findProofsWithProofImagesByIds(ids);
    }

    /** 재인증 매핑 */
    public Map<Long, Proof> mapReproofs(List<Proof> reproofs) {
        return reproofs.stream()
                .collect(Collectors.toMap(Proof::getProofId, Function.identity()));
    }
}


// 변경 후 사용
List<Proof> proofs = proofListQueryService.findOriginalProofs(contractId, start, end, userId, false);
List<Long> originalIds = proofs.stream().map(Proof::getId).toList();

List<Proof> reProofs = proofListQueryService.findReProofs(contractId, originalIds, userId, false);
Map<Long, Proof> reProofMap = proofListQueryService.mapReproofs(reProofs);
```

**✨ 효과:**  
- 중복 코드 약 30~40줄 제거  
- 조회 로직 단순화, 가독성 향상  
- 신규 주체(관리자 등) 추가 시 손쉬운 확장  


# 🚀 리팩토링 성과 요약

| 구분 | 주요 개선 | 효과 |
|------|------------|------|
| 코드 중복 제거 | 유사 로직 통합 (약 100줄+ 감소) | 유지보수성 향상 |
| 구조 단순화 | 구현체 통합, 내부 메서드 추출 | 클래스 수/파일 수 감소 |
| 가독성 향상 | 명확한 메서드 명칭, 책임 분리 | 코드 이해도 향상 |
| 확장성 강화 | 인터페이스 다중구현 구조 | 신규 로직 추가 용이 |
| 안정성 향상 | 공통 검증·알림 로직 단일화 | 버그 및 예외 누락 감소 |


# 🧠 정리

이번 리팩토링은 단순한 코드 축소를 넘어,  
**유스케이스 단위의 추상화와 재사용성 확보**를 목표로 진행되었습니다.  

중복된 인증 조회 로직을 통합하고  
조회 책임을 별도의 서비스로 분리함으로써  
유스케이스 구조를 보다 단순하고 명확하게 정리했습니다
