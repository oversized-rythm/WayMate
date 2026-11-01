package com.waymate.backend.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.waymate.backend.repository.UserRepository;
import java.io.IOException;
import com.waymate.backend.service.JwtService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository){
        this.jwtService=jwtService;
        this.userRepository=userRepository;
    }

    //process each incoming request
    @Override
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException{

            //read the Authorization Header
            String authHeader=request.getHeader("Authorization");
            //Check for Bearer Token
            if(authHeader==null || !authHeader.startsWith("Bearer ")){
                filterChain.doFilter(request,response);
                return;
            }
            //Extract the jwt
            String token=authHeader.substring(7);

            //Validate the JWT
            if(!jwtService.validateToken(token)){
                filterChain.doFilter(request,response);
                return;

            }
            //Extract user's UUID
            String userId= jwtService.extractUserId(token);

            //Check weather the user is Active or not
            UUID id=UUID.fromString(userId);
            boolean isActive=userRepository.findById(id)
                    .map(user->"ACTIVE".equals(user.getAccountStatus()))
                    .orElse(false);
            if(!isActive){
                filterChain.doFilter(request,response);
                return;
            }

            //Create an Authentication for the user
            UsernamePasswordAuthenticationToken authentication =new UsernamePasswordAuthenticationToken(
                    id,
                    null,
                    List.of()
            );
            //Storing the Authentication in Spring Security
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request,response);
        }
    }

