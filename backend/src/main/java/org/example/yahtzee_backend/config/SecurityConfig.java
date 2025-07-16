package org.example.yahtzee_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disabilita CSRF (per ora, per permettere H2 console)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**")
                )

                // Permette frames per H2 console
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                )

                // Configurazione permessi
                .authorizeHttpRequests(auth -> auth
                        // Permetti accesso libero alla H2 console
                        .requestMatchers("/h2-console/**").permitAll()

                        // Permetti accesso alle API pubbliche (per test)
                        .requestMatchers("/api/public/**").permitAll()

                        // Permetti accesso a tutti gli endpoint per ora (SOLO PER SVILUPPO!)
                        .anyRequest().permitAll()
                )

                // Disabilita form login per ora
                .formLogin(AbstractHttpConfigurer::disable)

                // Disabilita http basic per ora
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
