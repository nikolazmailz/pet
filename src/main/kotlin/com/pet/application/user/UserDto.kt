package com.pet.application.user

import java.math.BigDecimal
import java.util.UUID

data class UserDto(
    val id: UUID,
    val discountType: DiscountType,
    val fixedDiscount: BigDecimal? = null,
    val percentDiscount: BigDecimal? = null,
)

enum class DiscountType{
    FIXED,
    PERCENT
}

