package dto

data class LoginAccountDto(
    val deviceType: String = "Product Testing",
    val deviceId: String = "Product Testing",
    val deviceAlias: String = "Product Testing",
    val email: String,
    val password: String
)
