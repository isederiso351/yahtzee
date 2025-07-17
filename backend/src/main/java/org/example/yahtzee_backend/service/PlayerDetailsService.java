package org.example.yahtzee_backend.service;

import org.example.yahtzee_backend.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class PlayerDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerDetailsService.class);

    @Autowired
    private PlayerService playerService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user by username: {}", username);

        Player player = playerService.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("Player not found with username: {}", username);
                    return new UsernameNotFoundException("Player not found: " + username);
                });

        logger.debug("Found player: {} with ID: {}", player.getUsername(), player.getId());

        // Crea le autorità per l'utente
        Collection<GrantedAuthority> authorities = getAuthorities(player);

        // Crea e ritorna UserDetails
        UserDetails userDetails = User.builder()
                .username(player.getUsername())
                .password(player.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(!player.getIsActive())  // Account bloccato se non attivo
                .credentialsExpired(false)
                .disabled(!player.getIsActive())       // Account disabilitato se non attivo
                .build();

        logger.debug("Created UserDetails for player: {} with authorities: {}",
                player.getUsername(), authorities);

        return userDetails;
    }

    /**
     * Determina le autorità/ruoli per un giocatore
     */
    private Collection<GrantedAuthority> getAuthorities(Player player) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Tutti i giocatori hanno il ruolo PLAYER
        authorities.add(new SimpleGrantedAuthority("ROLE_PLAYER"));

        // if (player.isAdmin()) {
        //     authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        // }
        // if (player.isPremium()) {
        //     authorities.add(new SimpleGrantedAuthority("ROLE_PREMIUM"));
        // }

        return authorities;
    }

    /**
     * Metodo di utility per refresh dei dati utente
     */
    public UserDetails refreshUserDetails(String username) {
        return loadUserByUsername(username);
    }
}