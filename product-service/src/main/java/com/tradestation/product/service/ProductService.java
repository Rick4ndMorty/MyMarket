package com.tradestation.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tradestation.common.result.Result;
import com.tradestation.product.dto.ProductDetailVO;
import com.tradestation.product.dto.ProductSaveReq;
import com.tradestation.product.dto.ProductVO;

public interface ProductService {

    Result<Void> publish(Long shopId, ProductSaveReq req);

    Result<Page<ProductVO>> search(String keyword, Long shopId, Long categoryId, String status, int page, int size);

    Result<ProductDetailVO> getDetail(Long id);

    Result<Void> update(Long shopId, Long productId, ProductSaveReq req);

    Result<Void> updateStatus(Long shopId, Long productId, String status);

    Result<Void> delete(Long shopId, Long productId);
}
