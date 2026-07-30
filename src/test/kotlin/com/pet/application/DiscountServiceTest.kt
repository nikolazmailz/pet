package com.pet.application

import com.pet.application.user.DiscountType
import com.pet.application.user.UserDto
import com.pet.application.user.UserService
import com.pet.domain.Basket
import com.pet.domain.Order
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.util.UUID


class DiscountServiceTest : ShouldSpec() {

    val userService = mockk<UserService>()

    var discountService = DiscountService(userService)

    init {


        should("calculate discount with DiscountType.PERCENT") {

            every { userService.getUserData(any()) } returns UserDto(
                id = UUID.randomUUID(),
                discountType = DiscountType.PERCENT,
                fixedDiscount = null,
                percentDiscount = BigDecimal.valueOf(50),
            )

            val order1 = Order(
                id = UUID.randomUUID(),
                price = BigDecimal.valueOf(50)
            )

            val order2 = Order(
                id = UUID.randomUUID(),
                price = BigDecimal.valueOf(100)
            )

            val original = Basket(
                listOf(order1, order2)
            )

            val newBasket =  discountService.calculateDiscount(
                UUID.randomUUID(),
                original
                )


            newBasket.orders.first { it.discountPrice!!.compareTo(BigDecimal.valueOf(25)) == 0 } shouldNotBe  null
            newBasket.orders.first { it.discountPrice!!.compareTo(BigDecimal.valueOf(50)) == 0 }  shouldNotBe  null

        }


        should("calculate discount with DiscountType.FIXED") {

            every { userService.getUserData(any()) } returns UserDto(
                id = UUID.randomUUID(),
                discountType = DiscountType.FIXED,
                fixedDiscount = BigDecimal.valueOf(10),
                percentDiscount = null,
            )

            val order1 = Order(
                id = UUID.randomUUID(),
                price = BigDecimal.valueOf(20)
            )

            val order2 = Order(
                id = UUID.randomUUID(),
                price = BigDecimal.valueOf(30)
            )

            val original = Basket(
                listOf(order1, order2)
            )

            val newBasket =  discountService.calculateDiscount(
                UUID.randomUUID(),
                original
            )


            newBasket.orders.first { it.discountPrice!!.compareTo(BigDecimal.valueOf(16)) == 0 } shouldNotBe  null
            newBasket.orders.first { it.discountPrice!!.compareTo(BigDecimal.valueOf(24)) == 0 }  shouldNotBe  null


        }


    }
}