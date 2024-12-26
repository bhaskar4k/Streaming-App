package com.app.authentication.repository;


import com.app.authentication.entity.TLogExceptions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TLogExceptionsRepository extends JpaRepository<TLogExceptions, Long> { }
