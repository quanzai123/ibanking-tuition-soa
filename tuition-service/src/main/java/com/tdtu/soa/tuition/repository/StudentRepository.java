package com.tdtu.soa.tuition.repository;

import com.tdtu.soa.tuition.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, String> {
}

