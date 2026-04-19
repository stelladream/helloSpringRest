package kr.ac.hansung.cse.exception;

/**
 * =====================================================================
 * ProductNotFoundException - 상품 미발견 커스텀 예외
 * =====================================================================
 *
 * [커스텀 예외 클래스를 만드는 이유]
 * 1. 의미 명확화: "IllegalArgumentException" 보다 "ProductNotFoundException"이
 *    훨씬 더 명확하게 오류 원인을 전달합니다.
 * 2. 예외 타입 구분: @ExceptionHandler에서 예외 종류별로 다른 처리 로직을
 *    적용할 수 있습니다.
 * 3. 도메인 정보 포함: productId처럼 예외 상황과 관련된 추가 정보를
 *    예외 객체에 담을 수 있습니다.
 *
 * [RuntimeException vs Exception (검사 예외 vs 비검사 예외)]
 * ┌───────────────────┬──────────────────┬──────────────────────────────┐
 * │ 구분              │ RuntimeException │ Exception (검사 예외)         │
 * ├───────────────────┼──────────────────┼──────────────────────────────┤
 * │ try-catch 강제    │ 아니오           │ 예 (컴파일 오류)              │
 * │ 트랜잭션 롤백     │ 자동 롤백        │ 수동 설정 필요               │
 * │ Spring 관례       │ 주로 사용        │ IOException 등 인프라 예외   │
 * └───────────────────┴──────────────────┴──────────────────────────────┘
 * → Spring 프로젝트에서는 RuntimeException 계열을 주로 사용합니다.
 */
public class ProductNotFoundException extends RuntimeException {

    // 어떤 ID를 조회했을 때 발생했는지 기록
    private final Long productId;

    /**
     * @param id 존재하지 않는 상품 ID
     */
    public ProductNotFoundException(Long id) {
        // 부모 클래스(RuntimeException)에 오류 메시지 전달
        super("존재하지 않는 상품 ID: " + id);
        this.productId = id;
    }

    /**
     * 예외 핸들러에서 ID 정보를 활용할 때 사용합니다.
     * 예) 로그에 어떤 ID를 조회했다가 실패했는지 기록
     */
    public Long getProductId() {
        return productId;
    }
}
