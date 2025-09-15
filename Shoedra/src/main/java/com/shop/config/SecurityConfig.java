package com.shop.config;

import com.shop.constant.Role;
import com.shop.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import com.shop.entity.Member;

import com.shop.repository.MemberRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService(MemberRepository memberRepository) {
        return userRequest -> {
            DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
            OAuth2User oAuth2User = delegate.loadUser(userRequest);

            String email = null;
            String name = null;
            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            if (registrationId.equals("google")) {
                email = oAuth2User.getAttribute("email");
                name = oAuth2User.getAttribute("name");
            } else if (registrationId.equals("naver")) {
                var response = (java.util.Map<String, Object>) oAuth2User.getAttribute("response");
                email = (String) response.get("email");
                name = (String) response.get("name");
            } else if (registrationId.equals("kakao")) {
                var kakaoAccount = (java.util.Map<String, Object>) oAuth2User.getAttribute("kakao_account");
                if (kakaoAccount != null) {
                    email = (String) kakaoAccount.get("email");
                    var profile = (java.util.Map<String, Object>) kakaoAccount.get("profile");
                    name = profile != null ? (String) profile.get("nickname") : null;
                }
                // email이 없으면 카카오 id로 임시 이메일 생성
                if (email == null) {
                    Object kakaoId = oAuth2User.getAttribute("id");
                    if (kakaoId != null) {
                        email = "kakao_" + kakaoId + "@kakao.com";
                    }
                }
            }
            // email이 null이 아니도록 강제 보장
            if (email == null) {
                throw new RuntimeException("이메일 정보를 가져올 수 없습니다.");
            }
            // attributes에 email이 없으면 강제로 추가
            java.util.Map<String, Object> attributes = new java.util.HashMap<>(oAuth2User.getAttributes());
            attributes.put("email", email);
            Member member = memberRepository.findByEmail(email).orElse(null);
            if (member == null) {
                member = new Member();
                member.setEmail(email);
                member.setName(name != null ? name : email);
                member.setRole(Role.USER);
                memberRepository.save(member);
            }
            return new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_" + member.getRole().name())),
                    attributes,
                    "email"
            );
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, MemberService memberService, OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService) throws Exception {
//            http.csrf(csrf -> csrf.disable())
                http
                .userDetailsService(memberService)
                .formLogin(form -> form
                        .loginPage("/members/login")
                        .defaultSuccessUrl("/")
                        .usernameParameter("email")
                        .failureUrl("/members/login/error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/members/logout"))
                        .logoutSuccessUrl("/")
                        .permitAll()
                ).csrf(csrf -> csrf
                                .ignoringRequestMatchers(
                                        "/cash/success",
                                        "/cash/fail",
                                        "/cash/refund",
                                        "/api/cash/**" // ✅ API 캐시 경로에 대해 CSRF 예외 추가
                                )
                        )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/static/js/**", "/img/**", "/images/**", "/upload/**", "/favicon.ico", "/static/**").permitAll()
                        .requestMatchers("/", "/members/**", "/item/**", "/chat/**", "/error", "/auction/**","/calendar/**","/cash/**","/mypage/**","/sse/**","/order/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN").anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/members/login")
                        .defaultSuccessUrl("/")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                );

        return http.build();
    }

}