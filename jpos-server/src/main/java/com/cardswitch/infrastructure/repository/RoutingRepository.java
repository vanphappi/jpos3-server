package com.cardswitch.infrastructure.repository;

import com.cardswitch.domain.routing.RoutingRule;
import java.util.List;

public interface RoutingRepository {
    List<RoutingRule> findActiveRules();
    RoutingRule findByPriority(int priority);
    void save(RoutingRule rule);
    void update(RoutingRule rule);
}