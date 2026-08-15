package com.placementhub.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "recruiters")
@Data

public class Recruiter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "full_name")
    private String fullName;

    private String phone;
    private String designation;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;
}
