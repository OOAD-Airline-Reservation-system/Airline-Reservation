package com.airline.reservation.repository;

import com.airline.reservation.entity.Role;
import com.airline.reservation.entity.User;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Firestore-backed repository for User.
 * Collection: "users" — document ID = email.
 * Pure Fabrication (GRASP): no domain logic, only persistence.
 */
@Repository
public class UserRepository {

    private static final String COLLECTION = "users";
    private final Firestore firestore;

    public UserRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Optional<User> findByEmail(String email) {
        try {
            DocumentSnapshot doc = firestore.collection(COLLECTION)
                    .document(email.toLowerCase()).get().get();
            if (!doc.exists()) return Optional.empty();
            return Optional.of(fromDoc(doc));
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore read failed for user: " + email, e);
        }
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    public User save(User user) {
        try {
            firestore.collection(COLLECTION)
                    .document(user.getEmail().toLowerCase())
                    .set(toMap(user)).get();
            return user;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore write failed for user: " + user.getEmail(), e);
        }
    }

    private java.util.Map<String, Object> toMap(User u) {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("email", u.getEmail());
        m.put("fullName", u.getFullName());
        m.put("password", u.getPassword());
        m.put("roles", u.getRoles().stream().map(Enum::name).toList());
        return m;
    }

    @SuppressWarnings("unchecked")
    private User fromDoc(DocumentSnapshot doc) {
        User u = new User();
        u.setEmail(doc.getId());
        u.setFullName(doc.getString("fullName"));
        u.setPassword(doc.getString("password"));
        Set<Role> roles = new HashSet<>();
        Object rolesObj = doc.get("roles");
        if (rolesObj instanceof java.util.List<?> list) {
            list.forEach(r -> roles.add(Role.valueOf(r.toString())));
        }
        u.setRoles(roles);
        return u;
    }
}
