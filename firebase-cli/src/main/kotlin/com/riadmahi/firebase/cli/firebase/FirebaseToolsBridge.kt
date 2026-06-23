package com.riadmahi.firebase.cli.firebase

import com.riadmahi.firebase.cli.utils.ProcessRunner
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URI

class FirebaseToolsBridge {
    private val runner = ProcessRunner()

    // Firebase CLI OAuth credentials (public, embedded in firebase-tools)
    private val clientId = "563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com"
    private val clientSecret = "j9iVZfS8kkCEFUPaAeJV0sAi"

    fun isInstalled(): Boolean {
        return try {
            val result = runner.run("firebase", "--version")
            result.exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    fun isLoggedIn(): Boolean {
        return try {
            val result = runner.run("firebase", "login:list", "--json")
            if (result.exitCode != 0) return false

            val json = Json.parseToJsonElement(result.output)
            val users = json.jsonObject["result"]?.jsonArray
            users != null && users.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    fun login() {
        runner.runInteractive("firebase", "login")
    }

    fun getAccessToken(): String? {
        // Get refresh token from config
        val refreshToken = readRefreshToken() ?: return null

        // Exchange refresh token for access token
        return exchangeRefreshToken(refreshToken)
    }

    private fun readRefreshToken(): String? {
        return readTokenFromConfig() ?: readTokenFromConfigstore()
    }

    private fun exchangeRefreshToken(refreshToken: String): String? {
        return try {
            val url = URI("https://oauth2.googleapis.com/token").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val postData = buildString {
                append("client_id=").append(clientId)
                append("&client_secret=").append(clientSecret)
                append("&refresh_token=").append(refreshToken)
                append("&grant_type=refresh_token")
            }

            connection.outputStream.use { os ->
                os.write(postData.toByteArray())
            }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = Json.parseToJsonElement(response)
                json.jsonObject["access_token"]?.jsonPrimitive?.content
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTokenFromOutput(output: String): String? {
        // Firebase CLI outputs token in various formats
        val patterns = listOf(
            Regex("1//[a-zA-Z0-9_-]+"),
            Regex("ya29\\.[a-zA-Z0-9_-]+")
        )

        for (pattern in patterns) {
            pattern.find(output)?.value?.let { return it }
        }
        return null
    }

    private fun readTokenFromConfig(): String? {
        return try {
            val homeDir = System.getProperty("user.home")
            val configPath = "$homeDir/.config/firebase/config.json"
            val configFile = java.io.File(configPath)

            if (!configFile.exists()) return null

            val json = Json.parseToJsonElement(configFile.readText())
            json.jsonObject["tokens"]?.jsonObject?.get("refresh_token")?.jsonPrimitive?.content
        } catch (e: Exception) {
            null
        }
    }

    private fun readTokenFromConfigstore(): String? {
        return try {
            val homeDir = System.getProperty("user.home")
            // Firebase CLI stores credentials in configstore on macOS/Linux
            val configPath = "$homeDir/.config/configstore/firebase-tools.json"
            val configFile = java.io.File(configPath)

            if (!configFile.exists()) return null

            val json = Json.parseToJsonElement(configFile.readText())
            // The token structure in firebase-tools.json
            json.jsonObject["tokens"]?.jsonObject?.get("refresh_token")?.jsonPrimitive?.content
                ?: json.jsonObject["user"]?.jsonObject?.get("tokens")?.jsonObject?.get("refresh_token")?.jsonPrimitive?.content
        } catch (e: Exception) {
            null
        }
    }
}
