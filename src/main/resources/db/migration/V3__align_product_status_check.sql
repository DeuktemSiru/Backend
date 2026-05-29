-- product.status CHECK 제약을 최신 enum(AVAILABLE, PAUSED, SOLD_OUT, EXPIRED, DELETED)에 맞춰 재생성합니다.
-- 운영 DB는 spring.jpa.hibernate.ddl-auto=validate 로 운영되므로,
-- 기존에 (AVAILABLE, SOLD_OUT, EXPIRED) 만 허용하던 CHECK 가 남아 있어
-- "판매중지"(PAUSED) / "취소·삭제"(DELETED) 상태 변경 시 500 에러가 발생하는 문제를 해결합니다.

ALTER TABLE product DROP CONSTRAINT IF EXISTS product_status_check;

ALTER TABLE product
    ADD CONSTRAINT product_status_check
    CHECK (status IN ('AVAILABLE', 'PAUSED', 'SOLD_OUT', 'EXPIRED', 'DELETED'));
