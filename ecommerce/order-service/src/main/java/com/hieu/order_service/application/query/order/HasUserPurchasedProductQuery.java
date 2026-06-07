package com.hieu.order_service.application.query.order;

import com.hieu.order_service.application.common.Query;

public record HasUserPurchasedProductQuery(String userId, String productId) implements Query<Boolean> {}
