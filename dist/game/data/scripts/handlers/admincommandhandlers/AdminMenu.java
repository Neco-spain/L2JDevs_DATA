/*
 * Copyright © 2004-2019 L2JDevs
 * 
 * This file is part of L2JDevs.
 * 
 * L2JDevs is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2JDevs is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.admincommandhandlers;

import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.data.xml.impl.AdminData;
import org.l2jdevs.gameserver.handler.AdminCommandHandler;
import org.l2jdevs.gameserver.handler.IAdminCommandHandler;
import org.l2jdevs.gameserver.model.L2Clan;
import org.l2jdevs.gameserver.model.L2Object;
import org.l2jdevs.gameserver.model.L2World;
import org.l2jdevs.gameserver.model.Location;
import org.l2jdevs.gameserver.model.actor.L2Character;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.network.SystemMessageId;

/**
 * This class handles following admin commands: - handles every admin menu command
 * @author Sacrifice
 */
public final class AdminMenu implements IAdminCommandHandler
{
	private static final Logger _log = Logger.getLogger(AdminMenu.class.getName());
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_char_manage",
		"admin_teleport_character_to_menu",
		"admin_recall_char_menu",
		"admin_recall_party_menu",
		"admin_recall_clan_menu",
		"admin_goto_char_menu",
		"admin_kick_menu",
		"admin_kill_menu",
		"admin_ban_menu",
		"admin_unban_menu"
	};
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.equals("admin_char_manage"))
		{
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_teleport_character_to_menu"))
		{
			final String[] data = command.split(" ");
			if (data.length == 5)
			{
				final String playerName = data[1];
				final L2PcInstance player = L2World.getInstance().getPlayer(playerName);
				if (player != null)
				{
					teleportCharacter(player, new Location(Integer.parseInt(data[2]), Integer.parseInt(data[3]), Integer.parseInt(data[4])), activeChar, "Admin is teleporting you.");
				}
			}
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_recall_char_menu"))
		{
			try
			{
				final String targetName = command.substring(23);
				final L2PcInstance player = L2World.getInstance().getPlayer(targetName);
				teleportCharacter(player, activeChar.getLocation(), activeChar, "Admin is teleporting you.");
			}
			catch (StringIndexOutOfBoundsException e)
			{
			}
		}
		else if (command.startsWith("admin_recall_party_menu"))
		{
			try
			{
				final String targetName = command.substring(24);
				final L2PcInstance player = L2World.getInstance().getPlayer(targetName);
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return true;
				}
				
				if (!player.isInParty())
				{
					activeChar.sendMessage("Player is not in party.");
					teleportCharacter(player, activeChar.getLocation(), activeChar, "Admin is teleporting you.");
					return true;
				}
				
				for (L2PcInstance pm : player.getParty().getMembers())
				{
					teleportCharacter(pm, activeChar.getLocation(), activeChar, "Your party is being teleported by an Admin.");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
			}
		}
		else if (command.startsWith("admin_recall_clan_menu"))
		{
			try
			{
				final String targetName = command.substring(23);
				final L2PcInstance player = L2World.getInstance().getPlayer(targetName);
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return true;
				}
				
				final L2Clan clan = player.getClan();
				if (clan == null)
				{
					activeChar.sendMessage("Player is not in a clan.");
					teleportCharacter(player, activeChar.getLocation(), activeChar, "Admin is teleporting you.");
					return true;
				}
				
				for (L2PcInstance member : clan.getOnlineMembers(0))
				{
					teleportCharacter(member, activeChar.getLocation(), activeChar, "Your clan is being teleported by an Admin.");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				
			}
		}
		else if (command.startsWith("admin_goto_char_menu"))
		{
			try
			{
				final String targetName = command.substring(21);
				final L2PcInstance player = L2World.getInstance().getPlayer(targetName);
				activeChar.setInstanceId(player.getInstanceId());
				teleportToCharacter(activeChar, player);
			}
			catch (StringIndexOutOfBoundsException e)
			{
			}
		}
		else if (command.equals("admin_kill_menu"))
		{
			handleKill(activeChar);
		}
		else if (command.startsWith("admin_kick_menu"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				st.nextToken();
				final String player = st.nextToken();
				final L2PcInstance plyr = L2World.getInstance().getPlayer(player);
				String text;
				if (plyr != null)
				{
					plyr.logout();
					text = "You kicked " + plyr.getName() + " from the game.";
				}
				else
				{
					text = "Player " + player + " was not found in the game.";
				}
				activeChar.sendMessage(text);
			}
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_ban_menu"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				final String subCommand = "admin_ban_char";
				if (!AdminData.getInstance().hasAccess(subCommand, activeChar.getAccessLevel()))
				{
					activeChar.sendMessage("You don't have the access right to use this command!");
					_log.warning("Character " + activeChar.getName() + " tryed to use admin command " + subCommand + ", but have no access to it!");
					return false;
				}
				IAdminCommandHandler ach = AdminCommandHandler.getInstance().getHandler(subCommand);
				ach.useAdminCommand(subCommand + command.substring(14), activeChar);
			}
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_unban_menu"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				final String subCommand = "admin_unban_char";
				if (!AdminData.getInstance().hasAccess(subCommand, activeChar.getAccessLevel()))
				{
					activeChar.sendMessage("You don't have the access right to use this command!");
					_log.warning("Character " + activeChar.getName() + " tryed to use admin command " + subCommand + ", but have no access to it!");
					return false;
				}
				IAdminCommandHandler ach = AdminCommandHandler.getInstance().getHandler(subCommand);
				ach.useAdminCommand(subCommand + command.substring(16), activeChar);
			}
			showMainPage(activeChar);
		}
		return true;
	}
	
	private void handleKill(L2PcInstance activeChar)
	{
		handleKill(activeChar, null);
	}
	
	private void handleKill(L2PcInstance activeChar, String player)
	{
		final L2Object obj = activeChar.getTarget();
		L2Character target = (L2Character) obj;
		String filename = "main_menu.htm";
		if (player != null)
		{
			final L2PcInstance plyr = L2World.getInstance().getPlayer(player);
			if (plyr != null)
			{
				target = plyr;
				activeChar.sendMessage("You killed " + plyr.getName());
			}
		}
		
		if (target != null)
		{
			if (target instanceof L2PcInstance)
			{
				target.reduceCurrentHp(target.getMaxHp() + target.getMaxCp() + 1, activeChar, null);
				filename = "charmanage.htm";
			}
			else if (Config.L2JMOD_CHAMPION_ENABLE && target.isChampion())
			{
				target.reduceCurrentHp((target.getMaxHp() * Config.L2JMOD_CHAMPION_HP) + 1, activeChar, null);
			}
			else
			{
				target.reduceCurrentHp(target.getMaxHp() + 1, activeChar, null);
			}
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		}
		AdminHtml.showAdminHtml(activeChar, filename);
	}
	
	/**
	 * @param activeChar
	 */
	private void showMainPage(L2PcInstance activeChar)
	{
		AdminHtml.showAdminHtml(activeChar, "charmanage.htm");
	}
	
	private void teleportCharacter(L2PcInstance player, Location loc, L2PcInstance activeChar, String message)
	{
		if (player != null)
		{
			player.sendMessage(message);
			player.teleToLocation(loc, true);
		}
		showMainPage(activeChar);
	}
	
	private void teleportToCharacter(L2PcInstance activeChar, L2Object target)
	{
		L2PcInstance player = null;
		if (target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		
		if (player.getObjectId() == activeChar.getObjectId())
		{
			player.sendPacket(SystemMessageId.CANNOT_USE_ON_YOURSELF);
		}
		else
		{
			activeChar.setInstanceId(player.getInstanceId());
			activeChar.teleToLocation(player.getLocation(), true);
			activeChar.sendMessage("You're teleporting yourself to character " + player.getName());
		}
		showMainPage(activeChar);
	}
}