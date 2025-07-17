package com.example.login.model;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
@Column(nullable = false)
    private String name;
@Column(nullable = false, unique = true)
    private String email;
@Column(nullable = false)
    private String password;
@Column
    private String gender;

@Column
    private String phone;
@Column
    private LocalDate dob;  // Use java.time.LocalDate for date of birth
@Column
    private Double height;

    private boolean enabled=false;
@Column
    private LocalDateTime lastProfileUpdate;

    public boolean canUpdateProfile() {
        return lastProfileUpdate == null
                || lastProfileUpdate.isBefore(LocalDateTime.now().minusMonths(1));
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles;

}
