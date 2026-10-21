package com.placementhub.backend.repository;

import com.placementhub.backend.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    // Responses include the user's email, so fetch the user row with the student.
    @EntityGraph(attributePaths = "user")
    Optional<Student> findByUser_Email(String email);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<Student> findAll(Pageable pageable);
}
