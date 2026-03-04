package com.example.sdddemo.config;

import com.example.sdddemo.domain.Stock;
import com.example.sdddemo.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final StockRepository stockRepository;

    @Override
    public void run(String... args) {
        stockRepository.save(new Stock("MacBook Pro", 5));
        stockRepository.save(new Stock("iPad Air", 3));
        stockRepository.save(new Stock("AirPods Pro", 10));

        log.info("[초기 데이터] 재고 데이터 초기화 완료");
        stockRepository.findAll().forEach(stock ->
                log.info("  - {}: {}개", stock.getProductName(), stock.getQuantity()));
    }
}
