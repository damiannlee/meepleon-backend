package com.meepleday.user

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Forces resolution of the deferred CsrfToken so CookieCsrfTokenRepository actually writes the
 * XSRF-TOKEN cookie on every request — without this, Spring Security 6's lazy token loading
 * means the cookie never appears until something else happens to read it first.
 */
class CsrfCookieFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        (request.getAttribute(CsrfToken::class.java.name) as? CsrfToken)?.token
        filterChain.doFilter(request, response)
    }
}
