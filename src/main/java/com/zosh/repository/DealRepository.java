package com.zosh.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zosh.model.Deal;

public interface DealRepository extends JpaRepository<Deal, Long>{

	
}
