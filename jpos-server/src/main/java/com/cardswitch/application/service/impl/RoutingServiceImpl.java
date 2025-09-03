package com.cardswitch.application.service.impl;

import com.cardswitch.application.service.RoutingService;
import com.cardswitch.domain.transaction.Transaction;
import com.cardswitch.domain.routing.RoutingRule;
import com.cardswitch.infrastructure.repository.RoutingRepository;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import java.util.List;

@Service
public class RoutingServiceImpl implements RoutingService {
    
    private final RoutingRepository routingRepository;
    
    public RoutingServiceImpl(RoutingRepository routingRepository) {
        this.routingRepository = routingRepository;
    }
    
    @Override
    @Cacheable(value = "routing", key = "#transaction.mti + '_' + #transaction.stan")
    public String route(Transaction transaction) {
        List<RoutingRule> rules = routingRepository.findActiveRules();
        
        return rules.stream()
            .filter(rule -> rule.matches(
                transaction.getMti(), 
                getProcessingCode(transaction),
                transaction.getAcquirerId()
            ))
            .sorted((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()))
            .map(RoutingRule::getDestination)
            .findFirst()
            .orElse(null);
    }
    
    private String getProcessingCode(Transaction transaction) {
        // Extract processing code based on transaction type
        // This would come from the original ISO message
        return "000000"; // Default
    }
}