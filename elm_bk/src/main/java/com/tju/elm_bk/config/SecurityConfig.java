package com.tju.elm_bk.config;

import com.tju.elm_bk.security.JWTFilter;
import com.tju.elm_bk.security.JwtAccessDeniedHandler;
import com.tju.elm_bk.security.JwtAuthenticationEntryPoint;
import com.tju.elm_bk.service.UserModelDetailsService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final UserModelDetailsService userDetailsService;
    private final JWTFilter jwtFilter;

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
                        // 顾客、商家、骑手与管理员会话使用单一端权限。把主要的顾客私有
                        // 接口集中声明为 USER，避免运营账号通过手写请求调用顾客流程。
                        .requestMatchers("/api/carts/**", "/api/addresses/**",
                                "/api/v1/assets/**", "/api/v1/preferences/**")
                                .hasAuthority("USER")
                        // 订单详情仍由服务层核对“本人订单/本人店铺/管理员”，允许三端按职责读取。
                        .requestMatchers(HttpMethod.GET, "/api/orders/{id}", "/api/orders/detail")
                                .hasAnyAuthority("USER", "BUSINESS", "ADMIN")
                        .requestMatchers("/api/orders/list/business")
                                .hasAnyAuthority("BUSINESS", "ADMIN")
                        .requestMatchers("/api/orders/**").hasAuthority("USER")
                        .requestMatchers("/api/merchant/interaction/collections/**",
                                "/api/merchant/interaction/status/**",
                                "/api/merchant/interaction/update").hasAuthority("USER")
                        .anyRequest().authenticated()
                )
                .headers(httpSecurity -> httpSecurity
                        .frameOptions(options -> options.sameOrigin())
                )
                .sessionManagement(httpSecurity -> httpSecurity
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(httpSecurity -> httpSecurity.disable())
                .httpBasic(httpSecurity -> httpSecurity.disable())
                .rememberMe(httpSecurity -> httpSecurity.disable())
                .csrf(httpSecurity -> httpSecurity.disable())
                .cors(httpSecurity -> httpSecurity.configurationSource(corsConfigurationSource()))
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint));
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
