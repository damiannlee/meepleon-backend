package com.meepleday.user

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.util.matcher.AntPathRequestMatcher

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/me").authenticated()
                    .anyRequest().permitAll()
            }
            .oauth2Login { oauth2 ->
                oauth2.userInfoEndpoint { it.userService(customOAuth2UserService) }
                oauth2.defaultSuccessUrl("/", true)
            }
            .logout { logout ->
                logout.logoutUrl("/api/auth/logout")
                logout.logoutSuccessHandler { _, response, _ -> response.status = HttpStatus.NO_CONTENT.value() }
            }
            .csrf { csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // The default XorCsrfTokenRequestAttributeHandler masks the token for BREACH protection,
                // but CookieCsrfTokenRepository hands JS the raw token — the plain handler keeps both sides
                // in sync, which is Spring Security's documented pattern for cookie/header SPA auth.
                csrf.csrfTokenRequestHandler(CsrfTokenRequestAttributeHandler())
            }
            .addFilterAfter(CsrfCookieFilter(), BasicAuthenticationFilter::class.java)
            // API clients (the SPA) need a JSON-friendly 401 instead of a redirect to the OAuth login page.
            .exceptionHandling { exceptions ->
                exceptions.defaultAuthenticationEntryPointFor(
                    HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    AntPathRequestMatcher("/api/**"),
                )
            }

        return http.build()
    }
}
