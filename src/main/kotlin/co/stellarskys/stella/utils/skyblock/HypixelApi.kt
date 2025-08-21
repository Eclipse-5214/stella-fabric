package co.stellarskys.stella.utils.skyblock

import co.stellarskys.stella.utils.NetworkUtils
import kotlinx.serialization.json.*

object HypixelApi {
    fun fetchElectionData(
        apiUrl: String = "https://api.hypixel.net/resources/skyblock/election",
        onResult: (ElectionData?) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        NetworkUtils.getJson(
            url = apiUrl,
            onSuccess = { json ->
                try {
                    val mayor = json["mayor"]?.jsonObject
                    val currentYear = json["current"]?.jsonObject?.get("year")?.jsonPrimitive?.intOrNull

                    val mayorName = mayor?.get("name")?.jsonPrimitive?.contentOrNull
                    val perks = mayor?.get("perks")?.jsonArray?.mapNotNull { perk ->
                        val name = perk.jsonObject["name"]?.jsonPrimitive?.contentOrNull
                        val desc = perk.jsonObject["description"]?.jsonPrimitive?.contentOrNull
                        if (name != null && desc != null) name to desc else null
                    }

                    val ministerName = mayor?.get("minister")?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
                    val ministerPerk = mayor?.get("minister")?.jsonObject?.get("perk")?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull

                    onResult(
                        ElectionData(
                            mayorName = mayorName ?: "Unknown",
                            mayorPerks = perks ?: emptyList(),
                            ministerName = ministerName ?: "None",
                            ministerPerk = ministerPerk ?: "None",
                            currentYear = currentYear ?: -1
                        )
                    )
                } catch (e: Exception) {
                    onError(e)
                }
            },
            onError = onError
        )
    }

    data class ElectionData(
        val mayorName: String,
        val mayorPerks: List<Pair<String, String>>,
        val ministerName: String,
        val ministerPerk: String,
        val currentYear: Int
    )
}