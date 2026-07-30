package com.pet.domain

import java.math.BigDecimal
import java.util.UUID

class Order(
    val id : UUID,
    val price: BigDecimal,
    val discountPrice: BigDecimal? = null,
) {
}