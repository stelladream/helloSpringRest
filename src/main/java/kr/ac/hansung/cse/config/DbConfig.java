package kr.ac.hansung.cse.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 데이터베이스 및 JPA 설정
 *
 * @EnableTransactionManagement : @Transactional 어노테이션 활성화
 * @ComponentScan               : service · repository 패키지의 빈을 자동 등록
 *
 * 빈 구성 흐름:
 *   DataSource → EntityManagerFactory → TransactionManager
 */
@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = {
        "kr.ac.hansung.cse.service",
        "kr.ac.hansung.cse.repository"
})
public class DbConfig {

    /**
     * DataSource: DB 연결 정보
     * DriverManagerDataSource는 학습용 (운영 환경에서는 HikariCP 등 커넥션 풀 사용)
     */
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl("jdbc:mysql://mysql:3306/productdb" +
                  "?useSSL=false" +
                  "&allowPublicKeyRetrieval=true" +
                  "&serverTimezone=Asia/Seoul" +
                  "&useUnicode=true" +
                  "&characterEncoding=UTF-8");
        ds.setUsername("appuser");
        ds.setPassword("apppass");
        return ds;
    }

    /**
     * EntityManagerFactory: JPA의 핵심 객체
     * EntityManager를 생성하는 팩토리이며, 앱 전체에서 하나만 존재합니다.
     * persistence.xml 없이 Java 코드로 JPA 환경을 구성합니다.
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean emf =
                new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource());
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter()); // JPA 구현체: Hibernate
        emf.setPackagesToScan("kr.ac.hansung.cse.model");         // @Entity 클래스 위치
        emf.setJpaProperties(hibernateProperties());
        return emf;
    }

    private Properties hibernateProperties() {
        Properties props = new Properties();
        props.setProperty("hibernate.hbm2ddl.auto",
                "none");                                  // DDL 자동 실행 안 함 (init.sql이 담당)
        props.setProperty("hibernate.show_sql",  "true"); // 실행 SQL 콘솔 출력 (학습용)
        props.setProperty("hibernate.format_sql", "true"); // SQL 줄바꿈 출력
        // dialect, allow_jdbc_metadata_access 생략 → Hibernate가 JDBC 메타데이터로 자동 감지
        return props;
    }

    /**
     * TransactionManager: @Transactional 처리
     * 메서드 실행 전 트랜잭션 시작, 정상 종료 시 commit, 예외 시 rollback
     */
    @Bean
    public PlatformTransactionManager transactionManager(
            EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
