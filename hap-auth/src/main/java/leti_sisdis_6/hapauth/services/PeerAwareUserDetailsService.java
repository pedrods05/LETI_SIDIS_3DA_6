package leti_sisdis_6.hapauth.services;

import leti_sisdis_6.hapauth.usermanagement.User;
import leti_sisdis_6.hapauth.usermanagement.UserInMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PeerAwareUserDetailsService implements UserDetailsService {
    
    private final UserInMemoryRepository userInMemoryRepository;
    private final PeerService peerService;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        
        // First, try to find user in local repository
        Optional<User> localUser = userInMemoryRepository.findByUsername(username);
        if (localUser.isPresent()) {
            log.debug("Found user '{}' in local repository", username);
            return localUser.get();
        }
        
        // If not found locally, try to find in peers
        Optional<User> peerUser = peerService.findUserInPeers(username);
        if (peerUser.isPresent()) {
            log.info("Found user '{}' in peer repository", username);
            return peerUser.get();
        }
        
        log.debug("User '{}' not found in local or peer repositories", username);
        throw new UsernameNotFoundException("User not found: " + username);
    }
}
