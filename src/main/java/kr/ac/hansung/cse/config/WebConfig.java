package kr.ac.hansung.cse.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 웹 계층 설정
 *
 * @EnableWebMvc : Spring MVC 활성화
 *   - DispatcherServlet, HandlerMapping, HandlerAdapter 등 자동 등록
 *   - Jackson이 클래스패스에 있으면 MappingJackson2HttpMessageConverter 자동 등록
 *     → @RestController의 JSON 직렬화/역직렬화가 별도 설정 없이 동작
 *
 * @ComponentScan : controller, exception 패키지의 빈 자동 등록
 *   - @RestController → ProductController
 *   - @RestControllerAdvice → GlobalExceptionHandler
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
        "kr.ac.hansung.cse.controller",
        "kr.ac.hansung.cse.exception"
})
public class WebConfig implements WebMvcConfigurer {
}
