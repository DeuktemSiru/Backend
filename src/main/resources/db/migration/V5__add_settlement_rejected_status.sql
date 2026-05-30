-- settlement.status CHECK 제약을 최신 enum(PENDING, COMPLETED, REJECTED)에 맞춰 재생성한다.
-- 운영 DB는 spring.jpa.hibernate.ddl-auto=validate 로 운영되므로,
-- 기존 (PENDING, COMPLETED) CHECK 가 남아 있으면 정산 반려 시 500 에러가 발생한다.

ALTER TABLE settlement DROP CONSTRAINT IF EXISTS settlement_status_check;

ALTER TABLE settlement
    ADD CONSTRAINT settlement_status_check
    CHECK (status IN ('PENDING', 'COMPLETED', 'REJECTED'));
