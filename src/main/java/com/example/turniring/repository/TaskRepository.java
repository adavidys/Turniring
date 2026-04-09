package com.example.turniring.repository;

import com.example.turniring.model.TaskModel;
import com.example.turniring.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<TaskModel, Long> {
    List<TaskModel> findByStatus(TaskStatus status);
}