package com.zosh.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zosh.model.Seller;
import com.zosh.model.SellerReport;
import com.zosh.repository.SellerReportRepository;
import com.zosh.service.SellerReportService;

@Service
public class SellerReportServiceImpl implements SellerReportService {

	@Autowired
	private SellerReportRepository reportRepository;
	
	@Override
	public SellerReport getSellerReport(Seller seller) {
		
		SellerReport sellerReport = reportRepository.findBySellerId(seller.getId());
		
		if(sellerReport==null) {
			SellerReport newReport = new SellerReport();
			newReport.setSeller(seller);
			
			return reportRepository.save(newReport);
		}
		
		return sellerReport;
	}

	@Override
	public SellerReport updateSellerReport(SellerReport sellerReport) {
		
		return reportRepository.save(sellerReport);
	}

}
