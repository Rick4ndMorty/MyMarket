package com.tradestation.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.exception.BusinessException;
import com.tradestation.common.result.Result;
import com.tradestation.product.entity.ProductReview;
import com.tradestation.product.mapper.ProductReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewMapper reviewMapper;

    /**
     * Create a review / question / follow-up / answer
     */
    @PostMapping("/review")
    public Result<ProductReview> create(@RequestHeader("X-User-Id") Long userId,
                                        @RequestBody Map<String, Object> body) {
        Long productId = Long.valueOf(body.get("productId").toString());
        String type = body.containsKey("type") ? (String) body.get("type") : "REVIEW";
        String content = (String) body.get("content");
        Long parentId = body.containsKey("parentId") ? Long.valueOf(body.get("parentId").toString()) : null;
        Long orderId = body.containsKey("orderId") ? Long.valueOf(body.get("orderId").toString()) : null;
        Integer rating = body.containsKey("rating") ? ((Number) body.get("rating")).intValue() : null;

        if ("REVIEW".equals(type)) {
            if (rating == null || rating < 1 || rating > 5) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "rating must be 1-5");
            }
        }

        ProductReview review = new ProductReview();
        review.setProductId(productId);
        review.setUserId(userId);
        review.setOrderId(orderId);
        review.setType(type);
        review.setParentId(parentId);
        review.setRating(rating);
        review.setContent(content);
        review.setCreateTime(LocalDateTime.now());
        review.setUpdateTime(LocalDateTime.now());
        reviewMapper.insert(review);

        log.info("Review created: type={}, productId={}, userId={}", type, productId, userId);
        return Result.ok(review);
    }

    /**
     * List reviews for a product (with nested followups)
     */
    @GetMapping("/{productId}/reviews")
    public Result<Page<ProductReview>> listReviews(@PathVariable Long productId,
                                                   @RequestParam(defaultValue = "REVIEW") String type,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        LambdaQueryWrapper<ProductReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductReview::getProductId, productId)
               .eq(ProductReview::getType, type)
               .orderByDesc(ProductReview::getCreateTime);
        Page<ProductReview> reviewPage = reviewMapper.selectPage(new Page<>(page, size), wrapper);

        for (ProductReview review : reviewPage.getRecords()) {
            LambdaQueryWrapper<ProductReview> fWrapper = new LambdaQueryWrapper<>();
            fWrapper.eq(ProductReview::getParentId, review.getId())
                    .eq(ProductReview::getType, "FOLLOWUP")
                    .orderByAsc(ProductReview::getCreateTime);
            review.setFollowups(reviewMapper.selectList(fWrapper));
        }

        return Result.ok(reviewPage);
    }

    /**
     * List questions for a product (with nested answers)
     */
    @GetMapping("/{productId}/questions")
    public Result<Map<String, Object>> listQuestions(@PathVariable Long productId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        LambdaQueryWrapper<ProductReview> qWrapper = new LambdaQueryWrapper<>();
        qWrapper.eq(ProductReview::getProductId, productId)
                .eq(ProductReview::getType, "QUESTION")
                .orderByDesc(ProductReview::getCreateTime);
        Page<ProductReview> questionPage = reviewMapper.selectPage(new Page<>(page, size), qWrapper);

        for (ProductReview question : questionPage.getRecords()) {
            LambdaQueryWrapper<ProductReview> aWrapper = new LambdaQueryWrapper<>();
            aWrapper.eq(ProductReview::getParentId, question.getId())
                    .eq(ProductReview::getType, "ANSWER")
                    .orderByAsc(ProductReview::getCreateTime);
            question.setAnswers(reviewMapper.selectList(aWrapper));
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("records", questionPage.getRecords());
        result.put("total", questionPage.getTotal());
        return Result.ok(result);
    }
}
