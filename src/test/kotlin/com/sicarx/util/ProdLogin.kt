package org.example.util

import org.openqa.selenium.By
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebDriver

import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.devtools.v120.network.*
import org.openqa.selenium.devtools.v120.network.model.Headers
import java.util.*

class ProdLogin {
    companion object {
        fun loginProduction(email: String, password: String,companyName: String,branch: String?): String? {
            var cauth: String? = null
            val co = ChromeOptions()
            with(co) {
                addArguments("--remote-allow-origins=*")
                addArguments("--allowed-ips")
                addArguments("--headless")
            }
            val driver = ChromeDriver(co)
            val devTools = driver.devTools
            devTools.createSession()

            devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()))
            devTools.addListener(Network.responseReceived()) {
                val res = it.response
                if (res.url == "https://7ew5wkc4jsnbb6ph2bd2r4o5540hqlea.lambda-url.us-east-1.on.aws/login/v1/login") {
                    if (res.status != 200) {
                        println("fallo el login de company, code: ${res.status}")
                    }

                    val headers: Headers = res.headers
                    val head1 = headers.toJson()["cauth"]
                    if (head1 != null) {
                        cauth = head1.toString()
                        println("***tokenCompany, $companyName - caut: $cauth")
                    }
                }

            }

            val login = verifiedLogin(
                driver = driver,
                url ="https://app.sicarx.com/",
                email =email,
                password = password
            )
            if (login){
                val existsCompany = try {
                    driver.findElement(By.xpath("//div[contains(text(),'$companyName')]")).click()
                    true
                } catch (e: NoSuchElementException) {
                    println("Login error  $e")
                    false
                }

                println("Exist Company $existsCompany")
                if (existsCompany){
                    try {
                        driver.findElement(By.xpath("//div[contains(text(),'$companyName')]")).click()
                        Thread.sleep(1500L)
                    }catch (e: StaleElementReferenceException){
                        println("error al seleccionar company : $e")
                    }

                    if (branch!= null){
                        clickEnterOnBranch(driver, branch)
                        Thread.sleep(1500L)
                    }
                }
            }
            println(cauth)
            driver.quit()
            return cauth
        }

        fun verifiedLogin(driver: ChromeDriver,url: String,email:String,password:String):Boolean{
            driver[url]
            Thread.sleep(2000L)
            val emailField = driver.findElement(By.id("email"))
            val passwordField = driver.findElement(By.id("password"))
            val boton = driver.findElement(By.id("btnLogin"))
            emailField.sendKeys(email)
            passwordField.sendKeys(password)
            boton.click()
            Thread.sleep(2000L)
            return if (driver.currentUrl != "https://app.sicarx.com/") {
                println("Inicio de sesión exitoso.")
                true
            } else {
                println("Inicio de sesión fallido.")
                false
            }

        }



        fun clickEnterOnBranch(driver: WebDriver, branchName: String) {

            val branchElement = try {
                driver.findElement(By.xpath("//span[text()='$branchName']")).click()
                true
            }catch (e: StaleElementReferenceException){
                println(e)
                false
            }
            println("Exist Branch $branchElement")

            if (branchElement){
                try {
                    val branch = driver.findElement(By.xpath("//span[text()='$branchName']"))
                    branch.click()
                }catch (e: StaleElementReferenceException){
                    println(e)
                }
            }

        }
    }

}