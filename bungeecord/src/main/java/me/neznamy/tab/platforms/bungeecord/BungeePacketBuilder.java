package me.neznamy.tab.platforms.bungeecord;

import java.util.ArrayList;
import java.util.List;

import me.neznamy.tab.shared.ProtocolVersion;
import me.neznamy.tab.shared.packets.EnumChatFormat;
import me.neznamy.tab.shared.packets.IChatBaseComponent;
import me.neznamy.tab.shared.packets.PacketBuilder;
import me.neznamy.tab.shared.packets.PacketPlayOutBoss;
import me.neznamy.tab.shared.packets.PacketPlayOutChat;
import me.neznamy.tab.shared.packets.PacketPlayOutPlayerInfo;
import me.neznamy.tab.shared.packets.PacketPlayOutPlayerInfo.EnumGamemode;
import me.neznamy.tab.shared.packets.PacketPlayOutPlayerInfo.EnumPlayerInfoAction;
import me.neznamy.tab.shared.packets.PacketPlayOutPlayerInfo.PlayerInfoData;
import me.neznamy.tab.shared.packets.PacketPlayOutPlayerListHeaderFooter;
import me.neznamy.tab.shared.packets.PacketPlayOutScoreboardDisplayObjective;
import me.neznamy.tab.shared.packets.PacketPlayOutScoreboardObjective;
import me.neznamy.tab.shared.packets.PacketPlayOutScoreboardObjective.EnumScoreboardHealthDisplay;
import me.neznamy.tab.shared.packets.PacketPlayOutScoreboardScore;
import me.neznamy.tab.shared.packets.PacketPlayOutScoreboardTeam;
import net.md_5.bungee.protocol.packet.BossBar;
import net.md_5.bungee.protocol.packet.Chat;
import net.md_5.bungee.protocol.packet.PlayerListHeaderFooter;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import net.md_5.bungee.protocol.packet.PlayerListItem.Item;
import net.md_5.bungee.protocol.packet.ScoreboardDisplay;
import net.md_5.bungee.protocol.packet.ScoreboardObjective;
import net.md_5.bungee.protocol.packet.ScoreboardObjective.HealthDisplay;
import net.md_5.bungee.protocol.packet.ScoreboardScore;
import net.md_5.bungee.protocol.packet.Team;

/**
 * Packet builder for BungeeCord platform
 */
public class BungeePacketBuilder implements PacketBuilder {

	@Override
	public Object build(PacketPlayOutBoss packet, ProtocolVersion clientVersion) {
		if (clientVersion.getMinorVersion() < 9) return null;
		BossBar bungeePacket = new BossBar(packet.id, packet.operation.ordinal());
		bungeePacket.setHealth(packet.pct);
		bungeePacket.setTitle(packet.name == null ? null : IChatBaseComponent.optimizedComponent(packet.name).toString(clientVersion));
		bungeePacket.setColor(packet.color == null ? 0 : packet.color.ordinal());
		bungeePacket.setDivision(packet.overlay == null ? 0: packet.overlay.ordinal());
		bungeePacket.setFlags(packet.getFlags());
		return bungeePacket;
	}

	@Override
	public Object build(PacketPlayOutChat packet, ProtocolVersion clientVersion) {
		return new Chat(packet.message.toString(clientVersion), (byte) packet.type.ordinal());
	}

	@Override
	public Object build(PacketPlayOutPlayerInfo packet, ProtocolVersion clientVersion) {
		List<Item> items = new ArrayList<Item>();
		for (PlayerInfoData data : packet.entries) {
			Item item = new Item();
			if (data.displayName != null) {
				if (clientVersion.getNetworkId() >= ProtocolVersion.v1_8.getNetworkId()) {
					item.setDisplayName(data.displayName.toString(clientVersion));
				} else {
					item.setDisplayName(data.displayName.toLegacyText());
				}
			} else if (clientVersion.getNetworkId() < ProtocolVersion.v1_8.getNetworkId()) {
				item.setDisplayName(data.name); //avoiding NPE, 1.7 client requires this, 1.8 added a leading boolean
			}
			if (data.gameMode != null) item.setGamemode(data.gameMode.ordinal()-1);
			item.setPing(data.latency);
			if (data.skin != null) {
				item.setProperties((String[][]) data.skin);
			} else {
				item.setProperties(new String[0][0]);
			}
			item.setUsername(data.name);
			item.setUuid(data.uniqueId);
			items.add(item);
		}
		PlayerListItem bungeePacket = new PlayerListItem();
		bungeePacket.setAction(PlayerListItem.Action.valueOf(packet.action.toString().replace("GAME_MODE", "GAMEMODE")));
		bungeePacket.setItems(items.toArray(new Item[0]));
		return bungeePacket;
	}

	@Override
	public Object build(PacketPlayOutPlayerListHeaderFooter packet, ProtocolVersion clientVersion) {
		return new PlayerListHeaderFooter(packet.header.toString(clientVersion, true), packet.footer.toString(clientVersion, true));
	}

	@Override
	public Object build(PacketPlayOutScoreboardDisplayObjective packet, ProtocolVersion clientVersion) {
		return new ScoreboardDisplay((byte)packet.slot, packet.objectiveName);
	}

	@Override
	public Object build(PacketPlayOutScoreboardObjective packet, ProtocolVersion clientVersion) {
		return new ScoreboardObjective(packet.objectiveName, jsonOrCut(packet.displayName, clientVersion, 32), packet.renderType == null ? null : HealthDisplay.valueOf(packet.renderType.toString()), (byte)packet.method);
	}

	@Override
	public Object build(PacketPlayOutScoreboardScore packet, ProtocolVersion clientVersion) {
		return new ScoreboardScore(packet.player, (byte) packet.action.ordinal(), packet.objectiveName, packet.score);
	}

	@Override
	public Object build(PacketPlayOutScoreboardTeam packet, ProtocolVersion clientVersion) {
		int color = 0;
		if (clientVersion.getMinorVersion() >= 13) {
			color = (packet.color != null ? packet.color : EnumChatFormat.lastColorsOf(packet.playerPrefix)).getNetworkId();
		}
		return new Team(packet.name, (byte)packet.method, jsonOrCut(packet.name, clientVersion, 16), jsonOrCut(packet.playerPrefix, clientVersion, 16), jsonOrCut(packet.playerSuffix, clientVersion, 16), 
				packet.nametagVisibility, packet.collisionRule, color, (byte)packet.options, packet.players.toArray(new String[0]));
	}
	
	@Override
	public PacketPlayOutPlayerInfo readPlayerInfo(Object bungeePacket, ProtocolVersion clientVersion) {
		PlayerListItem item = (PlayerListItem) bungeePacket;
		List<PlayerInfoData> listData = new ArrayList<PlayerInfoData>();
		for (Item i : item.getItems()) {
			listData.add(new PlayerInfoData(i.getUsername(), i.getUuid(), i.getProperties(), i.getPing(), EnumGamemode.values()[i.getGamemode()+1], IChatBaseComponent.fromString(i.getDisplayName())));
		}
		return new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.valueOf(item.getAction().toString().replace("GAMEMODE", "GAME_MODE")), listData);
	}

	@Override
	public PacketPlayOutScoreboardObjective readObjective(Object bungeePacket, ProtocolVersion clientVersion) {
		ScoreboardObjective packet = (ScoreboardObjective) bungeePacket;
		String title;
		if (clientVersion.getMinorVersion() >= 13) {
			title = packet.getValue() == null ? null : IChatBaseComponent.fromString(packet.getValue()).toLegacyText();
		} else {
			title = packet.getValue();
		}
		EnumScoreboardHealthDisplay renderType = (packet.getType() == null ? null : EnumScoreboardHealthDisplay.valueOf(packet.getType().toString().toUpperCase()));
		return new PacketPlayOutScoreboardObjective(packet.getAction(), packet.getName(), title, renderType);
	}

	@Override
	public PacketPlayOutScoreboardDisplayObjective readDisplayObjective(Object bungeePacket, ProtocolVersion clientVersion){
		return new PacketPlayOutScoreboardDisplayObjective(((ScoreboardDisplay) bungeePacket).getPosition(), ((ScoreboardDisplay) bungeePacket).getName());
	}
}