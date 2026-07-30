package com.pet.application

import com.pet.application.user.DiscountType
import com.pet.application.user.UserService
import com.pet.domain.Basket
import com.pet.domain.Order
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Service
class DiscountService(
    private val userService: UserService
) {

    fun calculateDiscount(userId: UUID, basket: Basket): Basket {

        val user = userService.getUserData(userId);
        return when (user.discountType) {
            DiscountType.FIXED -> {
                calculateFixedDiscount(basket, user.fixedDiscount)
            }

            DiscountType.PERCENT -> {
                calculatePercentDiscount(basket, user.percentDiscount)
            }
        }
    }

    private fun calculateFixedDiscount(basket: Basket, fixedDiscount: BigDecimal?): Basket {

        val totalPrice = basket.orders.sumOf { it -> it.price }

        return Basket(basket.orders.map { it ->
            Order(
                id = it.id,
                price = it.price,
                discountPrice = calculatePrice(it.price, totalPrice, fixedDiscount)
            )
        })
    }

    private fun calculatePrice(
        price: BigDecimal,
        totalPrice: BigDecimal,
        fixedDiscount: BigDecimal?
    ): BigDecimal {
        val d=  price.divide(totalPrice).setScale(2, RoundingMode.CEILING).multiply(fixedDiscount)
        return price.minus(d)
    }

    private fun calculatePercentDiscount(basket: Basket, percentDiscount: BigDecimal?): Basket {
        return Basket(basket.orders.map { it ->
            Order(
                id = it.id,
                price = it.price,
                discountPrice = calculatePrice(it.price, percentDiscount),
            )
        }.toList())
    }

    private fun calculatePrice(
        price: BigDecimal,
        percentDiscount: BigDecimal?
    ): BigDecimal {
        val onePercent = price.divide(BigDecimal.valueOf(100))
        val pricePercent = onePercent.multiply(percentDiscount)
        return price.minus(pricePercent)
    }

}