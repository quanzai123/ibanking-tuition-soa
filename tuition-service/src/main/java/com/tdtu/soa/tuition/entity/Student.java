package com.tdtu.soa.tuition.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "students")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    @Id
    @Column(length = 20, nullable = false)
    private String mssv;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(length = 100)
    private String faculty;

    @Column(length = 100)
    private String major;
}

