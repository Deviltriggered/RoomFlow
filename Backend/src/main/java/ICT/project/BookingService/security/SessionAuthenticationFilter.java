package ICT.project.BookingService.security;

import ICT.project.BookingService.entity.UserEntity;
import ICT.project.BookingService.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_USER_ID = "AUTH_USER_ID";

    private final UserRepository userRepository;

    public SessionAuthenticationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null && request.getSession(false) != null) {
            Object rawUserId = request.getSession(false).getAttribute(AUTH_USER_ID);
            if (rawUserId instanceof Long userId) {
                userRepository.findById(userId).ifPresent(this::authenticate);
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(UserEntity user) {
        String role = user.getUserRole() == null ? "CLIENT" : user.getUserRole().toUpperCase();
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getUserId(),
                user.getUserEmail(),
                user.getUserLegalName(),
                user.getUserPhone(),
                user.getUserRole()
        );
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
