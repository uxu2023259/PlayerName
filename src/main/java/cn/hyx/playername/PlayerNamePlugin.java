package cn.hyx.playername;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlayerNamePlugin extends JavaPlugin implements Listener {
    private static final String MANAGED_TEAM_PREFIX = "pnm_";
    private static final String LUCKPERMS_PREFIX_PLACEHOLDER = "%luckperms_prefix%";
    private static final String LUCKPERMS_SUFFIX_PLACEHOLDER = "%luckperms_suffix%";
    private static final int TEAM_PART_LIMIT = 64;
    private static final long REFRESH_INTERVAL_TICKS = 40L;
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("(?i)&\\#([0-9a-f]{6})");

    private BukkitTask refreshTask;
    private Method placeholderMethod;
    private Object luckPermsApi;
    private Object luckPermsSubscription;
    private boolean placeholderFailureLogged;
    private boolean luckPermsFailureLogged;
    private boolean luckPermsEventFailureLogged;

    @Override
    public void onEnable() {
        refreshIntegrations(true);
        clearManagedTeams();

        getServer().getPluginManager().registerEvents(this, this);

        applyAllScoreboards();
        Bukkit.getScheduler().runTask(this, this::applyAllScoreboards);
        refreshTask = Bukkit.getScheduler().runTaskTimer(this, this::applyAllScoreboards,
                REFRESH_INTERVAL_TICKS, REFRESH_INTERVAL_TICKS);

        getLogger().info("已启用，玩家头顶名称会立即同步为 LuckPerms 前缀、玩家名和后缀。");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll((Listener) this);
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        closeLuckPermsSubscription();
        clearManagedTeams();
        getLogger().info("已禁用，已清理本插件创建的头顶名称数据。");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        scheduleFullRefresh();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeManagedTeam(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        scheduleFullRefresh();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (isIntegrationPlugin(event.getPlugin())) {
            refreshIntegrations(false);
            scheduleFullRefresh();
            getLogger().info(event.getPlugin().getName() + " 已启用，正在重新同步玩家头顶名称。");
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (isIntegrationPlugin(event.getPlugin())) {
            refreshIntegrations(false);
            scheduleFullRefresh();
            getLogger().info(event.getPlugin().getName() + " 已禁用，正在重新同步玩家头顶名称。");
        }
    }

    private void scheduleFullRefresh() {
        if (isEnabled()) {
            Bukkit.getScheduler().runTask(this, this::applyAllScoreboards);
        }
    }

    private void refreshIntegrations(boolean logResult) {
        closeLuckPermsSubscription();
        placeholderMethod = findPlaceholderMethod();
        luckPermsApi = findLuckPermsApi();
        subscribeLuckPermsEvents();

        if (!logResult) {
            return;
        }

        if (luckPermsApi == null) {
            getLogger().warning("未检测到 LuckPerms，前后缀将暂时为空；启用 LuckPerms 后会自动重新同步。");
        } else {
            getLogger().info("已检测到 LuckPerms，可读取玩家前后缀。");
        }

        if (placeholderMethod == null) {
            getLogger().info("未检测到可用的 PlaceholderAPI，将直接读取 LuckPerms 前后缀。");
        } else {
            getLogger().info("已检测到 PlaceholderAPI，将优先解析 %luckperms_prefix% 与 %luckperms_suffix%。");
        }
    }

    private Method findPlaceholderMethod() {
        Plugin plugin = getServer().getPluginManager().getPlugin("PlaceholderAPI");
        if (plugin == null || !plugin.isEnabled()) {
            return null;
        }

        try {
            Class<?> placeholderApiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return placeholderApiClass.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            getLogger().warning("检测到 PlaceholderAPI，但无法访问变量解析接口，将改用 LuckPerms 数据。");
            return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object findLuckPermsApi() {
        Plugin plugin = getServer().getPluginManager().getPlugin("LuckPerms");
        if (plugin == null || !plugin.isEnabled()) {
            return null;
        }

        try {
            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");
            RegisteredServiceProvider provider = Bukkit.getServicesManager().getRegistration((Class) luckPermsClass);
            return provider == null ? null : provider.getProvider();
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }

    private void subscribeLuckPermsEvents() {
        if (luckPermsApi == null) {
            return;
        }

        try {
            Object eventBus = luckPermsApi.getClass().getMethod("getEventBus").invoke(luckPermsApi);
            Class<?> eventClass = Class.forName("net.luckperms.api.event.user.UserDataRecalculateEvent");
            Method subscribeMethod = findLuckPermsSubscribeMethod(eventBus.getClass());
            if (subscribeMethod == null) {
                return;
            }

            Consumer<Object> consumer = event -> runOnServerThread(() -> refreshLuckPermsPlayer(event));
            if (subscribeMethod.getParameterCount() == 3) {
                luckPermsSubscription = subscribeMethod.invoke(eventBus, this, eventClass, consumer);
            } else {
                luckPermsSubscription = subscribeMethod.invoke(eventBus, eventClass, consumer);
            }
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            if (!luckPermsEventFailureLogged) {
                luckPermsEventFailureLogged = true;
                getLogger().warning("无法订阅 LuckPerms 数据更新事件，将通过定时刷新保持头顶名称同步。");
            }
        }
    }

    private Method findLuckPermsSubscribeMethod(Class<?> eventBusClass) {
        Method twoArgumentMethod = null;
        for (Method method : eventBusClass.getMethods()) {
            if (!method.getName().equals("subscribe")) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 3
                    && Class.class.equals(parameterTypes[1])
                    && Consumer.class.isAssignableFrom(parameterTypes[2])) {
                return method;
            }
            if (parameterTypes.length == 2
                    && Class.class.equals(parameterTypes[0])
                    && Consumer.class.isAssignableFrom(parameterTypes[1])) {
                twoArgumentMethod = method;
            }
        }
        return twoArgumentMethod;
    }

    private void refreshLuckPermsPlayer(Object event) {
        UUID uniqueId = readLuckPermsEventPlayerId(event);
        if (uniqueId == null) {
            applyAllScoreboards();
            return;
        }

        Player target = Bukkit.getPlayer(uniqueId);
        if (target == null) {
            removeManagedTeam(uniqueId);
            return;
        }

        applyTargetToAllScoreboards(target, resolveNameParts(target));
    }

    private UUID readLuckPermsEventPlayerId(Object event) {
        try {
            Object user = event.getClass().getMethod("getUser").invoke(event);
            Object uniqueId = user.getClass().getMethod("getUniqueId").invoke(user);
            return uniqueId instanceof UUID uuid ? uuid : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private void closeLuckPermsSubscription() {
        Object subscription = luckPermsSubscription;
        luckPermsSubscription = null;
        if (subscription == null) {
            return;
        }

        try {
            subscription.getClass().getMethod("close").invoke(subscription);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
            // LuckPerms 会按插件对象管理订阅；这里无需向控制台输出无意义信息。
        }
    }

    private void runOnServerThread(Runnable runnable) {
        if (!isEnabled()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }
        Bukkit.getScheduler().runTask(this, runnable);
    }

    private void applyAllScoreboards() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        Set<String> expectedTeamNames = new HashSet<>();
        Map<UUID, NameParts> namePartsByPlayer = new HashMap<>();

        for (Player player : players) {
            expectedTeamNames.add(teamName(player.getUniqueId()));
            namePartsByPlayer.put(player.getUniqueId(), resolveNameParts(player));
        }

        for (Scoreboard scoreboard : collectActiveScoreboards()) {
            removeUnexpectedManagedTeams(scoreboard, expectedTeamNames);
            for (Player target : players) {
                applyTargetToScoreboard(scoreboard, target, namePartsByPlayer.get(target.getUniqueId()));
            }
        }
    }

    private void applyTargetToAllScoreboards(Player target, NameParts nameParts) {
        for (Scoreboard scoreboard : collectActiveScoreboards()) {
            applyTargetToScoreboard(scoreboard, target, nameParts);
        }
    }

    private void applyTargetToScoreboard(Scoreboard scoreboard, Player target, NameParts nameParts) {
        Team team = scoreboard.getTeam(teamName(target.getUniqueId()));
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName(target.getUniqueId()));
        }

        String entry = target.getName();
        for (String existingEntry : new ArrayList<>(team.getEntries())) {
            if (!existingEntry.equals(entry)) {
                team.removeEntry(existingEntry);
            }
        }
        if (!team.hasEntry(entry)) {
            team.addEntry(entry);
        }

        if (!Objects.equals(team.getPrefix(), nameParts.prefix())) {
            team.setPrefix(nameParts.prefix());
        }
        if (!Objects.equals(team.getSuffix(), nameParts.suffix())) {
            team.setSuffix(nameParts.suffix());
        }
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
    }

    private NameParts resolveNameParts(Player player) {
        String prefix = expandPlaceholder(player, LUCKPERMS_PREFIX_PLACEHOLDER);
        String suffix = expandPlaceholder(player, LUCKPERMS_SUFFIX_PLACEHOLDER);

        if (isMissingPlaceholder(prefix, LUCKPERMS_PREFIX_PLACEHOLDER)
                || isMissingPlaceholder(suffix, LUCKPERMS_SUFFIX_PLACEHOLDER)) {
            NameParts luckPermsNameParts = readLuckPermsNameParts(player);
            if (isMissingPlaceholder(prefix, LUCKPERMS_PREFIX_PLACEHOLDER)) {
                prefix = luckPermsNameParts.prefix();
            }
            if (isMissingPlaceholder(suffix, LUCKPERMS_SUFFIX_PLACEHOLDER)) {
                suffix = luckPermsNameParts.suffix();
            }
        }

        prefix = normalizeText(prefix);
        suffix = normalizeText(suffix);

        return new NameParts(
                limitTeamPart(appendSeparator(prefix)),
                limitTeamPart(prependSeparator(suffix))
        );
    }

    private String expandPlaceholder(Player player, String placeholder) {
        Method method = placeholderMethod;
        if (method == null) {
            return null;
        }

        try {
            Object result = method.invoke(null, player, placeholder);
            return result instanceof String text ? text : null;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            if (!placeholderFailureLogged) {
                placeholderFailureLogged = true;
                getLogger().warning("PlaceholderAPI 变量解析失败，已临时回退到 LuckPerms 数据。");
            }
            return null;
        }
    }

    private boolean isMissingPlaceholder(String value, String placeholder) {
        return value == null || value.equals(placeholder) || value.contains(placeholder);
    }

    private NameParts readLuckPermsNameParts(Player player) {
        Object api = luckPermsApi;
        if (api == null) {
            return NameParts.empty();
        }

        try {
            Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, player.getUniqueId());
            if (user == null) {
                return NameParts.empty();
            }

            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            Object metaData = cachedData.getClass().getMethod("getMetaData").invoke(cachedData);
            String prefix = readStringMethod(metaData, "getPrefix");
            String suffix = readStringMethod(metaData, "getSuffix");
            return new NameParts(prefix, suffix);
        } catch (ReflectiveOperationException exception) {
            if (!luckPermsFailureLogged) {
                luckPermsFailureLogged = true;
                getLogger().warning("读取 LuckPerms 玩家前后缀失败，已暂时显示原名。");
            }
            return NameParts.empty();
        }
    }

    private String readStringMethod(Object object, String methodName) throws ReflectiveOperationException {
        Object value = object.getClass().getMethod(methodName).invoke(object);
        return value instanceof String text ? text : "";
    }

    private String normalizeText(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return translateColorCodes(value.replace('\n', ' ').replace('\r', ' '));
    }

    private String translateColorCodes(String value) {
        Matcher matcher = HEX_COLOR_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1).toLowerCase(Locale.ROOT);
            StringBuilder replacement = new StringBuilder().append(ChatColor.COLOR_CHAR).append('x');
            for (char character : hex.toCharArray()) {
                replacement.append(ChatColor.COLOR_CHAR).append(character);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    private String appendSeparator(String value) {
        if (value == null || value.isEmpty() || value.endsWith(" ")) {
            return value == null ? "" : value;
        }
        return value + " ";
    }

    private String prependSeparator(String value) {
        if (value == null || value.isEmpty() || value.startsWith(" ")) {
            return value == null ? "" : value;
        }
        return " " + value;
    }

    private String limitTeamPart(String value) {
        if (value.length() <= TEAM_PART_LIMIT) {
            return value;
        }
        String limited = value.substring(0, TEAM_PART_LIMIT);
        if (limited.endsWith(String.valueOf(ChatColor.COLOR_CHAR))) {
            return limited.substring(0, limited.length() - 1);
        }
        return limited;
    }

    private void removeUnexpectedManagedTeams(Scoreboard scoreboard, Set<String> expectedTeamNames) {
        for (Team team : new ArrayList<>(scoreboard.getTeams())) {
            if (team.getName().startsWith(MANAGED_TEAM_PREFIX) && !expectedTeamNames.contains(team.getName())) {
                team.unregister();
            }
        }
    }

    private void clearManagedTeams() {
        for (Scoreboard scoreboard : collectActiveScoreboards()) {
            for (Team team : new ArrayList<>(scoreboard.getTeams())) {
                if (team.getName().startsWith(MANAGED_TEAM_PREFIX)) {
                    team.unregister();
                }
            }
        }
    }

    private void removeManagedTeam(UUID uniqueId) {
        String teamName = teamName(uniqueId);
        for (Scoreboard scoreboard : collectActiveScoreboards()) {
            Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                team.unregister();
            }
        }
    }

    private Set<Scoreboard> collectActiveScoreboards() {
        Set<Scoreboard> scoreboards = new LinkedHashSet<>();
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager != null) {
            scoreboards.add(scoreboardManager.getMainScoreboard());
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            scoreboards.add(player.getScoreboard());
        }
        return scoreboards;
    }

    private boolean isIntegrationPlugin(Plugin plugin) {
        String pluginName = plugin.getName();
        return pluginName.equalsIgnoreCase("LuckPerms") || pluginName.equalsIgnoreCase("PlaceholderAPI");
    }

    private String teamName(UUID uniqueId) {
        return MANAGED_TEAM_PREFIX + uniqueId.toString().replace("-", "").substring(0, 12);
    }

    private record NameParts(String prefix, String suffix) {
        private static NameParts empty() {
            return new NameParts("", "");
        }
    }
}
