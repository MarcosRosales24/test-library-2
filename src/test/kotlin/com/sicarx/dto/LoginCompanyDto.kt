package dto

data class LoginCompanyDto(
    val branchId: Long,
    val deviceId: String = "Product Testing",
    val deviceAlias: String = "Product Testing",
    val deviceType: String = "Product Testing",
    val jwt: String
)