package com.example.turniring.repository;

import com.example.turniring.entity.TaskEntity;
import com.example.turniring.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    List<TaskEntity> findByStatus(TaskStatus status);
}