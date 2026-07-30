package com.pet.application.user

import java.util.UUID

interface UserService {

   fun getUserData(id: UUID) : UserDto

}