package com.mdev.bukkit.mpasswordprotector;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class WhitelistCommand implements CommandExecutor {

private final mPasswordProtector plugin;
	
	public WhitelistCommand(mPasswordProtector plugin){
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args) {
		if(args.length < 1)
			return false;
		
		if(sender.isOp()){
			if(args[0].equalsIgnoreCase("list")){
				sender.sendMessage(ChatColor.GREEN + "Players whitelisted: ");
				for(String name : plugin.whitelistPlayerNames)
					sender.sendMessage(ChatColor.YELLOW + name);
						
				return true;
			} else if(args[0].equalsIgnoreCase("add")){
				//code for add
				if(plugin.whitelistPlayerNames.contains(args[1])) {
					sender.sendMessage(ChatColor.GREEN + "Player already whitelisted!");
					return true;
				}				
				plugin.whitelistPlayerNames.add(args[1]);
				plugin.setWhitelist();
				
				sender.sendMessage(ChatColor.GREEN + "Player '" + args[1] + "' is now whitelisted!");
				
				return true;
			} else if (args[0].equalsIgnoreCase("del")){
				if(!plugin.whitelistPlayerNames.contains(args[1])) {
					sender.sendMessage(ChatColor.GREEN + "Player is not whitelisted!");
					return true;
				}				
				plugin.whitelistPlayerNames.remove(args[1]);
				plugin.setWhitelist();
				
				sender.sendMessage(ChatColor.GREEN + "Player '" + args[1] + "' succssesfully removed from whitelist!");
				
				return true;
			}
		} else if(sender instanceof Player){
			sender.sendMessage(ChatColor.RED + "Buhh... Only for OPs!");
			return true;
		}
		
		return false;
	}
}
