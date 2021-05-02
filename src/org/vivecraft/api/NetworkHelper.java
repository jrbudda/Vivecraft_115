package org.vivecraft.api;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.vivecraft.render.PlayerModelController;
import org.vivecraft.utils.lwjgl.Matrix4f;
import org.vivecraft.utils.math.Quaternion;

import com.google.common.base.Charsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CCustomPayloadPacket;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class NetworkHelper {

	public static Map<UUID, ServerVivePlayer> vivePlayers = new HashMap<UUID, ServerVivePlayer>();

	public enum PacketDiscriminators {
		VERSION,
		REQUESTDATA,
		HEADDATA,
		CONTROLLER0DATA,
		CONTROLLER1DATA,
		WORLDSCALE,
		DRAW,
		MOVEMODE,
		UBERPACKET,
		TELEPORT,
		CLIMBING,
		SETTING_OVERRIDE,
		HEIGHT,
		ACTIVEHAND
	}
	public final static ResourceLocation channel = new ResourceLocation("vivecraft:data");

	public static CCustomPayloadPacket getVivecraftClientPacket(PacketDiscriminators command, byte[] payload)
	{
		PacketBuffer pb = new PacketBuffer(Unpooled.buffer());
		pb.writeByte(command.ordinal());
		pb.writeBytes(payload);
		return  (new CCustomPayloadPacket(channel, pb));
	}

	public static SCustomPayloadPlayPacket getVivecraftServerPacket(PacketDiscriminators command, byte[] payload)
	{
		PacketBuffer pb = new PacketBuffer(Unpooled.buffer());
		pb.writeByte(command.ordinal());
		pb.writeBytes(payload);
		return (new SCustomPayloadPlayPacket(channel, pb));
	}

	public static SCustomPayloadPlayPacket getVivecraftServerPacket(PacketDiscriminators command, String payload)
	{
		PacketBuffer pb = new PacketBuffer(Unpooled.buffer());
		pb.writeByte(command.ordinal());
		pb.writeString(payload);
		return (new SCustomPayloadPlayPacket(channel, pb));
	}

	public static boolean serverWantsData = false;
	public static boolean serverAllowsClimbey = false;
	public static boolean serverSupportsDirectTeleport = false;

	private static float worldScallast = 0;
	private static float heightlast = 0;

	public static boolean isVive(ServerPlayerEntity p){
		if(p == null) return false;
		if(vivePlayers.containsKey(p.getGameProfile().getId())){
			return vivePlayers.get(p.getGameProfile().getId()).isVR();
		}
		return false;
	}

	public static void sendPosData(ServerPlayerEntity from) {

		ServerVivePlayer v = vivePlayers.get(from.getUniqueID());
		if (v==null || v.isVR() == false || v.player == null || v.player.hasDisconnected()) return;

		for (ServerVivePlayer sendTo : vivePlayers.values()) {

			if (sendTo == null || sendTo.player == null || sendTo.player.hasDisconnected())
				continue; // dunno y but just in case.

			if (v == sendTo || v.player.getEntityWorld() != sendTo.player.getEntityWorld() || v.hmdData == null || v.controller0data == null || v.controller1data == null){
				continue;
			}

			double d = sendTo.player.getPositionVector().squareDistanceTo(v.player.getPositionVector());

			if (d < 256 * 256) {
				SCustomPayloadPlayPacket pack  = getVivecraftServerPacket(PacketDiscriminators.UBERPACKET, v.getUberPacket());
				sendTo.player.connection.sendPacket(pack);
			}
		}
	}
	
	public static void sendVersionInfo() {
		byte[] version = Minecraft.getInstance().minecriftVerString.getBytes(Charsets.UTF_8);
		String s = NetworkHelper.channel.toString();
		PacketBuffer pb = new PacketBuffer(Unpooled.buffer());
		pb.writeBytes(s.getBytes());
		Minecraft.getInstance().getConnection().sendPacket(new CCustomPayloadPacket(new ResourceLocation("minecraft:register"), pb));
		Minecraft.getInstance().getConnection().sendPacket(NetworkHelper.getVivecraftClientPacket(PacketDiscriminators.VERSION, version));
	}
}
