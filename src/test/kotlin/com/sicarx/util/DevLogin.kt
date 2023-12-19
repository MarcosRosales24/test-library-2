package util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import dto.LoginAccountDto
import dto.LoginCompanyDto
import org.example.dto.BranchesListDto
import org.example.dto.CompanyListDto
import org.example.request.getLog
import org.example.request.postLog
import org.example.util.GlobalVariables.Companion.ACCOUNT
import org.example.util.GlobalVariables.Companion.COMPANY
import org.example.util.GlobalVariables.Companion.LIST_BRANCH
import org.example.util.GlobalVariables.Companion.LIST_COMPANY
import org.springframework.http.ResponseEntity
import utils.FirmadorAWSGeneral
import java.net.URI
import java.security.MessageDigest

class DevLogin {

    companion object {
        private val aws = FirmadorAWSGeneral(
            secretKey = System.getenv("secretKey"),
            accessKey = System.getenv("accessKey")
        )
        fun loginAccount(email: String, password: String): String {
            val urlAcc = URI(ACCOUNT)
            val httpMethodAcc = "POST"
            val encrypt = password.toSHA512()
            val dtoAcc = LoginAccountDto(email = email, password = encrypt)
            val bodyRequestAcc = GsonBuilder().serializeNulls().setLenient().create().toJson(dtoAcc)
            val getSignHeaderAcc = aws.signBody(bodyRequestAcc, urlAcc, httpMethodAcc)
            val response = ACCOUNT.postLog(bodyRequestAcc, getSignHeaderAcc)
            return getJwt(response)
        }
        fun loginCompany(email: String, password: String, branchId: Long): String {
            val jwt = loginAccount(email, password)
            val urlCom = URI(COMPANY)
            val httpMethodCom = "POST"
            val dtoCom = LoginCompanyDto(jwt = jwt, branchId = branchId)
            val bodyRequestCom = GsonBuilder().serializeNulls().setLenient().create().toJson(dtoCom)
            val getSignHeaderCom = aws.signBody(bodyRequestCom, urlCom, httpMethodCom)
            val response = COMPANY.postLog(bodyRequestCom, getSignHeaderCom)
            return getCauth(response)
        }
        fun listMyCompanies(email: String, password: String) {
            val jwt = loginAccount(email, password)
            val auth = mapOf("Authorization" to jwt)
            val response = LIST_COMPANY.getLog(auth)
            val companiesType = object : TypeToken<List<CompanyListDto>>() {}.type
            val companies: List<CompanyListDto> = Gson().fromJson(response.body, companiesType)
            val list = companies.joinToString(separator = "\n") { "${it.uuid} - ${it.name} - ${it.contentId}" }
            println(list)
        }
        fun listBranches(email: String, password: String, uuid: String) {
            val jwt = loginAccount(email, password)
            val auth = mapOf("Authorization" to jwt)
            val response = "${LIST_BRANCH}$uuid".getLog(auth)

            val companiesType = object : TypeToken<List<BranchesListDto>>() {}.type
            val companies: List<BranchesListDto> = Gson().fromJson(response.body, companiesType)

            val list = companies.joinToString(separator = "\n") { "${it.id} - ${it.name} - ${it.alias}" }
            println(list)
        }
        private fun getJwt(response: ResponseEntity<String>): String {
            val responseBody = response.body ?: throw IllegalStateException("Void body")

            val jsonElement = JsonParser.parseString(responseBody)
            if (jsonElement.isJsonObject) {
                val jsonObject = jsonElement.asJsonObject

                val jwt = jsonObject.get("jwt")?.asString
                println("Account Jwt: $jwt")
                if (jwt != null) {
                    return jwt
                } else {
                    throw IllegalStateException("Jwt wasn't found")
                }
            } else {
                throw IllegalStateException("Response wasn't a valid json")
            }
        }
        private fun getCauth(response: ResponseEntity<String>): String {
            val cauthHeader = response.headers["cauth"]?.firstOrNull()
            println("Company Jwt: $cauthHeader")
            return cauthHeader ?: throw IllegalStateException("\"cauth\" not found")
        }
        private fun String.toSHA512(): String {
            val md = MessageDigest.getInstance("SHA-512")
            val digest = md.digest(this.toByteArray(Charsets.UTF_8))
            return digest.fold("") { str, it -> str + "%02x".format(it) }
        }
    }
}