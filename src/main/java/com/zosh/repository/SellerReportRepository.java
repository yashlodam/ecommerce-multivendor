package com.zosh.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zosh.model.SellerReport;

public interface SellerReportRepository extends JpaRepository<SellerReport, Long> {

	SellerReport findBySellerId(Long long1);
}
