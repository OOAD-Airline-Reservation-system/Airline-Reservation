package com.airline.reservation.entity;

import java.util.HashSet;
import java.util.Set;

/**
 * User — stored in Firestore collection "users", document ID = email.
 */
public class User {

    private String email;
    private String fullName;
    private String password;
    private Set<Role> roles = new HashSet<>();

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
}
