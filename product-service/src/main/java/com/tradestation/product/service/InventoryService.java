package com.tradestation.product.service;

import com.tradestation.common.result.Result;

import java.util.List;
import java.util.Map;

public interface InventoryService {

    Result<Void> deduct(List<Map<String, Object>> items);

    Result<Void> restore(List<Map<String, Object>> items);
}
