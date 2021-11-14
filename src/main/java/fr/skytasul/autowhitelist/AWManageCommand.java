package fr.skytasul.autowhitelist;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class AWManageCommand extends Command implements TabExecutor {
	
	private static final List<String> COMMANDS = Arrays.asList("sync", "reload", "stop", "refreshCache");
	
	private AutoWhitelist plugin;
	
	public AWManageCommand(AutoWhitelist plugin) {
		super("autowhitelist", "autowhitelist.manage", "aw");
		this.plugin = plugin;
	}
	
	@Override
	public void execute(CommandSender sender, String[] args) {
		if (args.length != 1) {
			sendSyntax(sender);
			return;
		}
		switch (args[0]) {
		case "sync":
			plugin.syncWhitelist();
			sender.sendMessage("§aWhitelist has been synced.");
			break;
		case "reload":
			plugin.loadConfig();
			sender.sendMessage("§aConfiguration has been reloaded.");
			break;
		case "stop":
			plugin.stopTask();
			sender.sendMessage("§aWhitelist syncing has been stopped. Run \"/aw reload\" to resume it.");
			break;
		case "refreshCache":
			plugin.startCache();
			sender.sendMessage("§aUsername cache has been reset. You should run \"/aw sync\" to sync again the whitelist.");
			break;
		default:
			sendSyntax(sender);
			break;
		}
	}

	private void sendSyntax(CommandSender sender) {
		sender.sendMessage("§cInvalid syntax. Expected: §l/aw " + COMMANDS.stream().collect(Collectors.joining("|", "<", ">")));
	}

	@Override
	public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
		if (args.length > 1) return Collections.emptyList();
		return COMMANDS.stream().filter(x -> x.startsWith(args[0])).toList();
	}
	
}
