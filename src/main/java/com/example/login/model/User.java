package com.example.login.model;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
    private long Id;
@Column(nullable = false, unique = true)
    private String email;
@Column(nullable = false)
    private String password;

private boolean enabled=false;


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles;

}
