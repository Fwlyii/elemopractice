package com.tju.elm_bk.config;

import com.tju.elm_bk.security.JWTFilter;
import com.tju.elm_bk.security.JwtAccessDeniedHandler;
import com.tju.elm_bk.security.JwtAuthenticationEntryPoint;
import com.tju.elm_bk.service.UserModelDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    @Autowired
    private JwtAuthenticationEntryPoint authenticationEntryPoint;
    @Autowired
    private JwtAccessDeniedHandler accessDeniedHandler;
    private final UserModelDetailsService userDetailsService;
    private final JWTFilter jwtFilter;

    public SecurityConfig(UserModelDetailsService userDetailsService, JWTFilter jwtFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String[] permitUrlArr = {
                "/api/auth",
                "/api/register",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/ws/**",
                "/api/ai/chat/health",
                "/api/businesses/search",
                "/api/businesses/carousel",
                "/uploads/**"
        };

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(permitUrlArr).permitAll()
                        // 游客可以浏览已上线商家、菜单和评价，也可以使用不保存个人历史的 AI 对话。
                        // 写购物车、下单、收藏以及读取私人会话仍然要求登录。
                        .requestMatchers(HttpMethod.GET,
                                "/api/businesses/{id}",
                                "/api/businesses/type",
                                "/api/foods/list",
                                "/api/v1/reviews/business/{businessId}",
                                "/api/ai/chat/recommendations").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/ai/chat").permitAll()
                        .anyRequest().authenticated()
                )
                .headers(httpSecurity -> httpSecurity  // 显式参数名
                        .frameOptions(options -> options.sameOrigin())
                )
                .sessionManagement(httpSecurity -> httpSecurity  // 显式参数名
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(httpSecurity -> httpSecurity.disable())  // 显式参数名
                .httpBasic(httpSecurity -> httpSecurity.disable())  // 显式参数名
                .rememberMe(httpSecurity -> httpSecurity.disable())  // 显式参数名
                .csrf(httpSecurity -> httpSecurity.disable())  // 显式参数名
                .cors(httpSecurity -> httpSecurity.configurationSource(corsConfigurationSource()))// 显式参数名
                .exceptionHandling(exception -> exception.accessDeniedHandler(accessDeniedHandler).authenticationEntryPoint(authenticationEntryPoint));
        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 只接受本机开发页和课程演示的临时 HTTPS 域名；不允许任意第三方网页跨域读取接口。
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.trycloudflare.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // 本地前端与后端分别运行在 18081/18080 时，下单请求会先发送预检；
        // 幂等键用于防止重复提交，必须显式加入 CORS 白名单，否则浏览器只会显示 Network Error。
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token", "idempotency-key"));
        configuration.setExposedHeaders(Arrays.asList("x-auth-token"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
