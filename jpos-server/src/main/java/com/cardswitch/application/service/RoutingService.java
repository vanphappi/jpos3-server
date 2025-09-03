package com.cardswitch.application.service;

import com.cardswitch.domain.transaction.Transaction;

public interface RoutingService {
    String route(Transaction transaction);
}