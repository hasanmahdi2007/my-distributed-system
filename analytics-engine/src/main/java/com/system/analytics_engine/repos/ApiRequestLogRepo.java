package com.system.analytics_engine.repos;


import com.system.analytics_engine.entities.ApiRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiRequestLogRepo extends JpaRepository<ApiRequestLog, Long> {
    
}