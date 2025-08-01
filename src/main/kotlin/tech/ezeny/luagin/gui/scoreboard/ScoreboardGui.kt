package tech.ezeny.luagin.gui.scoreboard

import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.utils.ColorUtils

class ScoreboardGui(
    private var title: String,
    val guiId: String
) {
    private var scoreboard: Scoreboard = Bukkit.getScoreboardManager()!!.newScoreboard
    private var objective: Objective? = null
    private val lines = mutableMapOf<Int, String>()
    private val teams = mutableMapOf<Int, Team>() // 存储每行对应的Team
    private val maxLines = 15

    // 回调函数
    private val showCallbacks = mutableListOf<(player: String) -> Unit>()
    private val hideCallbacks = mutableListOf<(player: String) -> Unit>()

    // 动画任务
    private var animationTask: BukkitRunnable? = null

    init {
        createObjective()
    }

    private fun createObjective() {
        // 清理旧的 Teams
        teams.values.forEach { team ->
            team.unregister()
        }
        teams.clear()

        // 清理旧的 objective
        objective?.unregister()

        // 创建新的 objective
        objective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY, ColorUtils.formatString(title))
        objective?.displaySlot = DisplaySlot.SIDEBAR
    }

    fun show(playerName: String) {
        val player = Bukkit.getPlayer(playerName) ?: return
        player.scoreboard = scoreboard
        showCallbacks.forEach { it(playerName) }
    }

    fun hide(playerName: String) {
        val player = Bukkit.getPlayer(playerName) ?: return
        player.scoreboard = Bukkit.getScoreboardManager()!!.mainScoreboard
        hideCallbacks.forEach { it(playerName) }
    }

    fun setTitle(newTitle: String) {
        title = newTitle
        objective?.displayName = ColorUtils.formatString(title)
    }

    /**
     * 设置指定行的内容
     * @param line 行号 (1-15)
     * @param text 文本内容
     */
    fun setLine(line: Int, text: String) {
        if (line < 1 || line > maxLines) {
            throw IllegalArgumentException("Line number must be between 1 and $maxLines")
        }

        val score = maxLines - line + 1 // 反转行号，使第1行显示在最上方

        // 清理旧的 Team 和 entry
        teams[line]?.let { oldTeam ->
            oldTeam.entries.forEach { entry ->
                scoreboard.resetScores(entry)
            }
            oldTeam.unregister()
        }
        teams.remove(line)

        if (text.isEmpty()) {
            lines.remove(line)
            return
        }

        lines[line] = text

        // 创建唯一的 entry 标识符（不可见字符）
        val entryId = generateUniqueEntry(line)

        val teamName = "sb_${guiId}_line_$line"
        val team = scoreboard.registerNewTeam(teamName)
        teams[line] = team

        val colorizedText = ColorUtils.formatString(text)
        team.prefix = colorizedText

        team.addEntry(entryId)

        objective?.getScore(entryId)?.score = score
    }

    /**
     * 设置多行内容
     * @param textLines 文本行列表
     */
    fun setLines(textLines: List<String>) {
        clearAllLines()
        textLines.forEachIndexed { index, text ->
            if (index < maxLines) {
                setLine(index + 1, text)
            }
        }
    }

    fun clearLine(line: Int) {
        setLine(line, "")
    }

    fun clearAllLines() {
        for (i in 1..maxLines) {
            clearLine(i)
        }
    }

    fun onShow(callback: (String) -> Unit) {
        showCallbacks.add(callback)
    }

    fun onHide(callback: (String) -> Unit) {
        hideCallbacks.add(callback)
    }

    fun getViewers(): List<String> {
        return Bukkit.getOnlinePlayers()
            .filter { it.scoreboard == scoreboard }
            .map { it.name }
    }

    fun animate(duration: Int, interval: Int, animation: (ScoreboardGui, Int) -> Unit) {
        animationTask?.cancel()
        val plugin = Bukkit.getPluginManager().getPlugin("Luagin") as? Luagin ?: return
        animationTask = object : BukkitRunnable() {
            var tick = 0
            override fun run() {
                if (tick >= duration) {
                    cancel()
                    return
                }
                animation(this@ScoreboardGui, tick)
                tick += interval
            }
        }
        animationTask?.runTaskTimer(plugin, 0L, interval.toLong())
    }

    fun destroy() {
        // 取消动画任务
        animationTask?.cancel()
        animationTask = null

        // 将所有查看此记分板的玩家恢复到主记分板
        getViewers().forEach { playerName ->
            hide(playerName)
        }

        teams.values.forEach { team ->
            team.unregister()
        }
        teams.clear()

        objective?.unregister()
        objective = null

        showCallbacks.clear()
        hideCallbacks.clear()
        lines.clear()
    }

    /**
     * 生成唯一的 entry 标识符
     */
    private fun generateUniqueEntry(line: Int): String {
        val chars =
            listOf("§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f")
        val charIndex = line % chars.size
        return chars[charIndex] + "§r"
    }
}
