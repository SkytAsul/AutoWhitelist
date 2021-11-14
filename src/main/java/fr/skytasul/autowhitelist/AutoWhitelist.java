package fr.skytasul.autowhitelist;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.types.PermissionNode;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import io.netty.util.internal.StringUtil;

public class AutoWhitelist extends Plugin {
	
	private static final Gson GSON = new Gson();
	
	private File debugFile = /*new File(getDataFolder(), "untitled.csv")*/ null;
	
	private UserManager luckpermsUsers;
	private Cache<String, UUID> uuidCache;
	
	private URL csvURL = null;
	private int syncTime = 60;
	private int cacheDuration;
	private List<String> servers;
	
	private ScheduledTask task;
	private Cache<String, Boolean> usernameCache;
	
	@Override
	public void onEnable() {
		luckpermsUsers = LuckPermsProvider.get().getUserManager();
		uuidCache = CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.HOURS).build();
		
		loadConfig();
		
		getProxy().getPluginManager().registerCommand(this, new AWManageCommand(this));
	}
	
	protected void loadConfig() {
		if (!getDataFolder().exists())
			getDataFolder().mkdir();
		
		File file = new File(getDataFolder(), "config.yml");
		
		if (!file.exists()) {
			try (InputStream in = getResourceAsStream("config.yml")) {
				Files.copy(in, file.toPath());
			}catch (Exception e) {
				getLogger().severe("An error occurred while trying to copy default configuration.");
				e.printStackTrace();
			}
		}
		
		try {
			Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
			syncTime = configuration.getInt("syncTime");
			cacheDuration = configuration.getInt("cacheDuration");
			servers = configuration.getStringList("servers");
			csvURL = new URL(configuration.getString("csvURL"));
		}catch (Exception e) {
			getLogger().severe("An error occurred while loading configuration.");
			e.printStackTrace();
		}
		
		startCache();
		startTask();
	}
	
	protected void stopTask() {
		if (task != null) task.cancel();
	}
	
	protected void startTask() {
		stopTask();
		task = getProxy().getScheduler().schedule(this, this::syncWhitelist, 1, syncTime, TimeUnit.SECONDS);
	}
	
	protected void startCache() {
		usernameCache = CacheBuilder.newBuilder().expireAfterWrite(cacheDuration, TimeUnit.SECONDS).build();
	}
	
	protected void syncWhitelist() {
		if (csvURL == null && debugFile == null) {
			getLogger().warning("Cannot sync whitelist: URL has not been defined.");
			return;
		}
		
		try (InputStreamReader reader = debugFile == null ? new InputStreamReader(csvURL.openStream()) : new FileReader(new File(getDataFolder(), "untitled.csv"))) {
			CsvParserSimple parser = new CsvParserSimple();
			List<String[]> lines = parser.read(reader, 0);
			lines.stream()
				.map(x -> x[0])
				.filter(username -> !StringUtil.isNullOrEmpty(username))
				.filter(username -> !usernameCache.asMap().containsKey(username))
				.forEach(username -> {
						usernameCache.put(username, false);
						User user = luckpermsUsers.getUser(username);
						if (user == null) {
							UUID uuid = uuidCache.asMap().computeIfAbsent(username, this::getUUID);
							if (uuid == null) {
								getLogger().severe("An error occurred while retrieving UUID for player " + username);
								return;
							}
							luckpermsUsers.modifyUser(uuid, this::addUserPermissions);
						}else {
							addUserPermissions(user);
							luckpermsUsers.saveUser(user);
						}
					});
		}catch (Exception e) {
			getLogger().severe("An error occurred while syncing whitelist.");
			e.printStackTrace();
		}
	}

	private void addUserPermissions(User user) {
		boolean result = false;
		for (String server : servers) {
			result |= user.data().add(PermissionNode.builder("bungeecord.server." + server).build()).wasSuccessful();
		}
		if (result) getLogger().info(user.getFriendlyName() + " has got new server permissions");
	}
	
	private UUID getUUID(String name) {
		try (InputStream stream = new URL("https://api.mojang.com/users/profiles/minecraft/" + name).openStream()) {
			String UUIDJson = new String(stream.readAllBytes());
			if (UUIDJson.isEmpty()) return null;
			JsonObject obj = GSON.fromJson(UUIDJson, JsonObject.class);
			if (obj == null || !obj.has("id")) return null;
			String uuid = obj
					.get("id")
					.getAsString()
					.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"); // add hyphens to UUID for parsing
			return UUID.fromString(uuid);
		}catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
