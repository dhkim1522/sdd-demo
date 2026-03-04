package com.example.sdddemo.controller;

import com.example.sdddemo.domain.Stock;
import com.example.sdddemo.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockRepository stockRepository;

    @GetMapping
    public List<Stock> findAllStocks() {
        return stockRepository.findAll();
    }
}
