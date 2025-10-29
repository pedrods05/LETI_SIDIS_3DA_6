package leti_sisdis_6.hapauth.usermanagement.repository;

import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import leti_sisdis_6.hapauth.usermanagement.model.User;   // <- IMPORT NECESSÁRIO

@Repository
public class UserInMemoryRepository {

    // Thread-safe map for storing users
    private final Map<String, User> userStore = new ConcurrentHashMap<>();
    private final Map<String, User> usernameIndex = new ConcurrentHashMap<>();

    public Optional<User> findById(String id) {
        return Optional.ofNullable(userStore.get(id));
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usernameIndex.get(username));
    }

    public boolean existsByUsername(String username) {
        return usernameIndex.containsKey(username);
    }

    public User save(User user) {
        if (user.getId() == null || user.getId().isBlank()) {
            user.setId(generateId());
        }
        // Update both indexes
        userStore.put(user.getId(), user);
        usernameIndex.put(user.getUsername(), user);
        return user;
    }

    public List<User> findAll() {
        return new ArrayList<>(userStore.values());
    }

    public void deleteById(String id) {
        User user = userStore.remove(id);
        if (user != null) {
            usernameIndex.remove(user.getUsername());
        }
    }

    public void deleteByUsername(String username) {
        User user = usernameIndex.remove(username);
        if (user != null) {
            userStore.remove(user.getId());
        }
    }

    public long count() {
        return userStore.size();
    }

    public boolean existsById(String id) {
        return userStore.containsKey(id);
    }

    private String generateId() {
        return UUID.randomUUID().toString().substring(0, 8); // um pouco maior para reduzir colisões
    }

    /** Clear all data (useful for testing) */
    public void clear() {
        userStore.clear();
        usernameIndex.clear();
    }

    /** Get instance info for debugging */
    public Map<String, Object> getInstanceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("totalUsers", userStore.size());
        info.put("usernames", new ArrayList<>(usernameIndex.keySet()));
        info.put("instanceId", System.identityHashCode(this));
        return info;
    }
}
