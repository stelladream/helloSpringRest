-- =====================================================================
-- init.sql - 데이터베이스 초기화 스크립트
-- =====================================================================
-- Docker Compose 실행 시 MySQL 컨테이너가 처음 시작될 때 자동으로 실행됩니다.
-- 위치: /docker-entrypoint-initdb.d/ 에 마운트됩니다.
--
-- 주의: 이 스크립트는 데이터 볼륨이 비어있을 때만 실행됩니다.
--       기존 볼륨이 남아있으면 재실행되지 않습니다.
--       재실행하려면: docker-compose down && rm -rf ./mysql-data && docker-compose up -d
-- =====================================================================

-- 클라이언트 인코딩 설정 (한글 깨짐 방지)
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =====================================================================
-- 데이터베이스 및 사용자 설정
-- =====================================================================

CREATE DATABASE IF NOT EXISTS productdb
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'appuser'@'%' IDENTIFIED BY 'apppass';
GRANT ALL PRIVILEGES ON productdb.* TO 'appuser'@'%';
FLUSH PRIVILEGES;

USE productdb;

-- =====================================================================
-- 테이블 생성 (FK 참조 순서에 따라 피참조 테이블을 먼저 생성)
-- =====================================================================

-- 카테고리 테이블
CREATE TABLE IF NOT EXISTS category (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 상품 상세 테이블
CREATE TABLE IF NOT EXISTS product_detail (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    manufacturer VARCHAR(200) NULL,       -- 제조사
    warranty     VARCHAR(100) NULL,       -- 보증 기간 (예: '1년')
    spec         TEXT         NULL,       -- 상세 스펙
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 태그 테이블
CREATE TABLE IF NOT EXISTS tag (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,    -- 태그명 중복 불가
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 상품 테이블 (category, product_detail FK 포함)
CREATE TABLE IF NOT EXISTS product (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    name              VARCHAR(100)   NOT NULL,
    price             DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    description       TEXT           NULL,
    category_id       BIGINT         NULL,
    product_detail_id BIGINT         NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id)       REFERENCES category(id),
    CONSTRAINT fk_product_detail
        FOREIGN KEY (product_detail_id) REFERENCES product_detail(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 상품-태그 조인 테이블 (@ManyToMany 중간 테이블)
CREATE TABLE IF NOT EXISTS product_tag (
    product_id BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    PRIMARY KEY (product_id, tag_id),
    FOREIGN KEY (product_id) REFERENCES product(id),
    FOREIGN KEY (tag_id)     REFERENCES tag(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 샘플 데이터
-- =====================================================================

INSERT INTO category (name) VALUES
    ('전자제품'), ('도서'), ('스포츠'), ('식품'), ('의류');

INSERT INTO tag (name) VALUES
    ('신상품'), ('베스트셀러'), ('할인'), ('친환경'), ('프리미엄');

INSERT INTO product (name, category_id, price, description) VALUES
    ('Apple MacBook Pro 14인치', 1, 2990000,
     'M3 Pro 칩 탑재, 18GB 유니파이드 메모리, 512GB SSD.\n전문가를 위한 고성능 노트북입니다.'),
    ('삼성 갤럭시 S24 Ultra',   1, 1550000,
     '200MP 카메라, AI 기반 사진 처리, S펜 내장.\n최신 안드로이드 플래그십 스마트폰입니다.'),
    ('스프링 부트 실전 활용 마스터', 2, 38000,
     '김영한 저. JPA, Spring Data, QueryDSL 등 실무 필수 기술을 다루는 베스트셀러입니다.'),
    ('나이키 에어맥스 270',      3, 149000,
     '270도 에어 쿠셔닝 시스템으로 최고의 편안함을 제공합니다.\n다양한 컬러로 출시되었습니다.'),
    ('비비고 왕교자 만두 1.2kg', 4,  12900,
     '속재료가 꽉 찬 프리미엄 교자 만두. 에어프라이어, 찜, 구이 모두 가능합니다.'),
    ('무신사 스탠다드 오버핏 맨투맨', 5, 39000,
     '면 100% 소재의 편안한 오버핏 맨투맨. 봄/가을 활용도 높은 베이직 아이템입니다.');

SELECT CONCAT('샘플 데이터 ', COUNT(*), '개 삽입 완료') AS result FROM product;
