package com.example.makeup.auth;

import com.example.makeup.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public User updateUser(Long id, UpdateUserRequest request) {
        User user = getUserById(id);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        User updated = userRepository.save(user);
        userDetailsService.evict(updated.getUsername());
        return updated;
    }
}
