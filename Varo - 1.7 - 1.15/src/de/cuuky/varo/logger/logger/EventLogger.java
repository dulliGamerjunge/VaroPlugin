package de.cuuky.varo.logger.logger;

import java.awt.Color;
import java.util.ArrayList;

import org.bukkit.Bukkit;

import de.cuuky.cfw.utils.JavaUtils;
import de.cuuky.varo.Main;
import de.cuuky.varo.bot.telegram.VaroTelegramBot;
import de.cuuky.varo.configuration.configurations.config.ConfigSetting;
import de.cuuky.varo.logger.VaroLogger;

public class EventLogger extends VaroLogger {

	public enum LogType {

		ALERT("ALERT", Color.RED, ConfigSetting.DISCORDBOT_EVENT_ALERT),
		BORDER("BORDER", Color.GREEN, ConfigSetting.DISCORDBOT_EVENT_BORDER),
		DEATH("DEATH", Color.BLACK, ConfigSetting.DISCORDBOT_EVENT_DEATH),
		JOIN_LEAVE("JOIN/LEAVE", Color.CYAN, ConfigSetting.DISCORDBOT_EVENT_JOIN_LEAVE),
		KILL("KILL", Color.BLACK, ConfigSetting.DISCORDBOT_EVENT_KILL),
		LOG("LOG", Color.RED, null),
		STRIKE("STRIKE", Color.YELLOW, ConfigSetting.DISCORDBOT_EVENT_STRIKE),
		WIN("WIN", Color.MAGENTA, ConfigSetting.DISCORDBOT_EVENT_WIN),
		YOUTUBE("YOUTUBE", Color.ORANGE, ConfigSetting.DISCORDBOT_EVENT_YOUTUBE);

		private Color color;
		private ConfigSetting idEntry;
		private String name;

		private LogType(String name, Color color, ConfigSetting idEntry) {
			this.color = color;
			this.name = name;
			this.idEntry = idEntry;
		}

		public Color getColor() {
			return color;
		}

		public String getName() {
			return name;
		}

		public long getPostChannel() {
			if (idEntry == null)
				return -1;

			return idEntry.getValueAsLong() != -1 ? idEntry.getValueAsLong() : ConfigSetting.DISCORDBOT_EVENTCHANNELID.getValueAsLong();
		}

		public static LogType getType(String s) {
			for (LogType type : values())
				if (type.getName().equalsIgnoreCase(s))
					return type;

			return null;
		}
	}

	private ArrayList<Object[]> queue;

	public EventLogger(String name) {
		super(name, true);

		this.queue = new ArrayList<>();
		startQueue();
	}

	private void startQueue() {
		Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getInstance(), new Runnable() {

			@Override
			public void run() {
				if (Main.getBotLauncher() == null)
					return;

				for (Object[] msg : new ArrayList<>(queue)) {
					sendToDiscord((LogType) msg[0], (String) msg[1]);
					sendToTelegram((LogType) msg[0], (String) msg[1]);
					queue.remove(msg);
				}
			}
		}, 20, 20);
	}

	private boolean sendToDiscord(LogType type, String msg) {
		if (Main.getBotLauncher().getDiscordbot() == null)
			return true;

		try {
			Main.getBotLauncher().getDiscordbot().sendMessage(msg, type.getName(), type.getColor(), type.getPostChannel());
			return true;
		} catch (NoClassDefFoundError | BootstrapMethodError e) {
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(Main.getPrefix() + "Failed to broadcast message! Did you enter a wrong channel ID?");
			return false;
		}
	}

	private void sendToTelegram(LogType type, String message) {
		VaroTelegramBot telegramBot = Main.getBotLauncher().getTelegrambot();
		if (telegramBot == null)
			return;

		try {
			if (!type.equals(LogType.YOUTUBE))
				telegramBot.sendEvent(message);
			else
				telegramBot.sendVideo(message);
		} catch (ArrayIndexOutOfBoundsException e) {
			telegramBot.sendEvent(message);
		}
	}

	public void println(LogType type, String message) {
		message = JavaUtils.replaceAllColors(message);

		String log = getCurrentDate() + " || " + "[" + type.getName() + "] " + message.replace("%noBot%", "");

		pw.println(log);
		logs.add(log);

		pw.flush();

		if (message.contains("%noBot%"))
			return;

		if (Main.getBotLauncher() == null) {
			queue.add(new Object[] { type, message });
			return;
		}

		sendToDiscord(type, message);
		sendToTelegram(type, message);
	}
}