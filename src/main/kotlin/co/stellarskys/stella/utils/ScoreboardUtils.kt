package co.stellarskys.stella.utils

import co.stellarskys.stella.Stella
import net.minecraft.scoreboard.ScoreboardDisplaySlot
import net.minecraft.scoreboard.ScoreboardEntry
import net.minecraft.scoreboard.Team

object ScoreboardUtils {
    @JvmStatic
    fun cleanSB(scoreboard: String): String {
        return scoreboard.stripControlCodes().toCharArray().filter { it.code in 32..126 }.joinToString(separator = "")
    }

    var sidebarLines: List<String> = emptyList()

    private val SCOREBOARD_ENTRY_COMPARATOR: Comparator<ScoreboardEntry> = Comparator.comparing { obj: ScoreboardEntry -> obj.value() }
        .reversed()
        .thenComparing({ obj: ScoreboardEntry -> obj.owner() }, java.lang.String.CASE_INSENSITIVE_ORDER);

    fun fetchScoreboardLines(): List<String> {
        val scoreboard = Stella.Companion.mc.world?.scoreboard ?: return emptyList()
        val objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR) ?: return emptyList()

        val scores = scoreboard.getScoreboardEntries(objective).filter { input ->
            input?.owner != null && !input.hidden()
        }.sortedWith(SCOREBOARD_ENTRY_COMPARATOR).take(15)

        return scores.map { e ->
            Team.decorateName(scoreboard.getScoreHolderTeam(e.owner()), e.name()).string
        }.asReversed()
    }
}