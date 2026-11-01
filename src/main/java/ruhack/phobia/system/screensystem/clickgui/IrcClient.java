package ruhack.phobia.system.screensystem.clickgui;

import com.adl.nativeprotect.User;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.RunCommand;
import net.minecraft.util.Formatting;
import ruhack.phobia.system.commandsystem.CommandManager;
import ruhack.phobia.util.string.chat.ChatMessage;

public final class IrcClient {
   public static final IrcClient INSTANCE = new IrcClient();
   private static final String HOST = "150.241.70.170";
   private static final int PORT = 20036;
   private static final int CONNECT_TIMEOUT_MS = 5000;
   private static final int READ_TIMEOUT_MS = 15000;
   private static final long RECONNECT_DELAY_MS = 3000L;
   private static final long POSITION_UPDATE_MS = 250L;
   private static final long POSITION_TTL_MS = 5000L;
   private final ConcurrentLinkedQueue<IrcClient.Event> events = new ConcurrentLinkedQueue<>();
   private final ConcurrentLinkedQueue<IrcClient.CoordinateEvent> coordinateEvents = new ConcurrentLinkedQueue<>();
   private final Map<String, IrcClient.PartyPosition> partyPositions = new ConcurrentHashMap<>();
   private final AtomicBoolean started = new AtomicBoolean();
   private final AtomicBoolean showNextPartyInfo = new AtomicBoolean();
   private final Object writerLock = new Object();
   private volatile BufferedWriter writer;
   private volatile Socket socket;
   private volatile IrcClient.State state = IrcClient.State.CONNECTING;
   private volatile int onlineCount;
   private volatile String nickname = "leaked by angelw1ngsov";
   private volatile String role = "User";
   private volatile IrcClient.PartySnapshot party = IrcClient.PartySnapshot.EMPTY;

   private IrcClient() {
   }

   public void startFromProfile() {
      this.start(profile("username", "leaked by angelw1ngsov"), profile("role", "User"));
   }

   public void start(String nickname, String role) {
      this.nickname = sanitizeIdentity(nickname, "leaked by angelw1ngsov");
      this.role = normalizeRole(role);
      if (this.started.compareAndSet(false, true)) {
         Thread thread = new Thread(this::connectionLoop, "Phobia-IRC");
         thread.setDaemon(true);
         thread.start();
         Thread positionThread = new Thread(this::positionLoop, "Phobia-Party-Position");
         positionThread.setDaemon(true);
         positionThread.start();
      }
   }

   public boolean sendMessage(String text) {
      String message = text == null ? "" : text.trim();
      return !message.isEmpty() && message.codePointCount(0, message.length()) <= 200
         ? this.sendLine("MSG|" + message.replace('\r', ' ').replace('\n', ' '))
         : false;
   }

   public IrcClient.Event pollEvent() {
      return this.events.poll();
   }

   public IrcClient.State state() {
      return this.state;
   }

   public int onlineCount() {
      return this.onlineCount;
   }

   public IrcClient.PartySnapshot party() {
      return this.party;
   }

   public IrcClient.CoordinateEvent pollCoordinateEvent() {
      return this.coordinateEvents.poll();
   }

   public boolean createParty() {
      return this.sendLine("PARTY|CREATE");
   }

   public boolean inviteToParty(String login) {
      String target = sanitizeIdentity(login, "");
      return !target.isEmpty() && this.sendLine("PARTY|INVITE|" + target);
   }

   public boolean acceptParty(String owner) {
      String login = sanitizeIdentity(owner, "");
      return !login.isEmpty() && this.sendLine("PARTY|ACCEPT|" + login);
   }

   public boolean declineParty(String owner) {
      String login = sanitizeIdentity(owner, "");
      return !login.isEmpty() && this.sendLine("PARTY|DECLINE|" + login);
   }

   public boolean disbandParty() {
      return this.sendLine("PARTY|DISBAND");
   }

   public boolean requestPartyInfo(boolean showInChat) {
      if (showInChat) {
         this.showNextPartyInfo.set(true);
      }

      boolean sent = this.sendLine("PARTY|INFO");
      if (!sent) {
         this.showNextPartyInfo.set(false);
      }

      return sent;
   }

   public boolean sendCoordinates(int x, int y, int z) {
      return this.sendLine("PARTY|COORD|" + x + "|" + y + "|" + z);
   }

   public boolean sendPartyPosition(double x, double y, double z) {
      return this.sendLine("PARTY|POSITION|" + x + "|" + y + "|" + z);
   }

   public IrcClient.PartyPosition partyPosition(String login) {
      if (login == null) {
         return null;
      }

      IrcClient.PartyPosition position = this.partyPositions.get(login.toLowerCase(Locale.ROOT));
      if (position != null && System.currentTimeMillis() - position.updatedAt() <= 5000L) {
         return position;
      }

      if (position != null) {
         this.partyPositions.remove(login.toLowerCase(Locale.ROOT), position);
      }

      return null;
   }

   public String login() {
      return this.nickname;
   }

   private void connectionLoop() {
      while (this.started.get()) {
         this.state = IrcClient.State.CONNECTING;

         try (Socket connected = new Socket()) {
            connected.connect(new InetSocketAddress("150.241.70.170", 20036), 5000);
            connected.setTcpNoDelay(true);
            connected.setKeepAlive(true);
            connected.setSoTimeout(15000);
            BufferedWriter connectedWriter = new BufferedWriter(new OutputStreamWriter(connected.getOutputStream(), StandardCharsets.UTF_8));
            this.socket = connected;
            this.writer = connectedWriter;
            this.sendLine("HELLO|" + this.nickname + "|" + this.role);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connected.getInputStream(), StandardCharsets.UTF_8))) {
               this.readLoop(reader);
            }
         } catch (IOException ignored) {
            this.state = IrcClient.State.OFFLINE;
            this.onlineCount = 0;
            this.party = IrcClient.PartySnapshot.EMPTY;
            this.partyPositions.clear();
         } finally {
            this.writer = null;
            this.socket = null;
         }

         if (!this.started.get()) {
            break;
         }

         sleep(3000L);
      }
   }

   private void positionLoop() {
      for (; this.started.get(); sleep(250L)) {
         MinecraftClient client = MinecraftClient.getInstance();
         ClientPlayerEntity player = client.player;
         if (this.state == IrcClient.State.ONLINE && player != null && this.party.contains(this.nickname)) {
            this.sendPartyPosition(player.getX(), player.getY(), player.getZ());
         }
      }
   }

   private void readLoop(BufferedReader reader) throws IOException {
      while (this.started.get()) {
         try {
            String line = reader.readLine();
            if (line == null) {
               throw new IOException("IRC connection closed");
            }

            this.handleLine(line);
         } catch (SocketTimeoutException ignored) {
            if (!this.sendLine("PING")) {
               throw new IOException("IRC heartbeat failed");
            }
         }
      }
   }

   private void handleLine(String line) {
      if (line.equals("SYSTEM|Connected")) {
         this.state = IrcClient.State.ONLINE;
      } else if (!line.equals("PONG")) {
         if (line.startsWith("ONLINE|")) {
            try {
               this.onlineCount = Math.max(0, Integer.parseInt(line.substring("ONLINE|".length())));
            } catch (NumberFormatException var4) {
            }
         } else if (line.startsWith("CHAT|")) {
            String[] parts = line.split("\\|", 5);
            if (parts.length == 5) {
               this.events.add(new IrcClient.Event(normalizeRole(parts[1]), sanitizeIdentity(parts[2], "User"), parts[4], parseTimestamp(parts[3])));
            }
         } else if (line.startsWith("PARTY_RESULT|")) {
            String[] parts = line.split("\\|", 3);
            if (parts.length == 3) {
               showPartyMessage(parts[2], parts[1].equals("OK") ? Formatting.GREEN : Formatting.RED);
            }
         } else if (line.startsWith("PARTY_INVITE|")) {
            showPartyInvitation(sanitizeIdentity(line.substring("PARTY_INVITE|".length()), "User"));
         } else if (line.equals("PARTY_INFO") || line.startsWith("PARTY_INFO|")) {
            this.handlePartyInfo(line);
         } else if (line.startsWith("PARTY_COORD|")) {
            String[] parts = line.split("\\|", 5);
            if (parts.length == 5) {
               try {
                  this.coordinateEvents
                     .add(new IrcClient.CoordinateEvent(parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4])));
               } catch (NumberFormatException var5) {
               }
            }
         } else {
            if (line.startsWith("PARTY_POSITION|")) {
               String[] parts = line.split("\\|", 5);
               if (parts.length == 5) {
                  try {
                     String login = sanitizeIdentity(parts[1], "");
                     if (!login.isEmpty()) {
                        this.partyPositions
                           .put(
                              login.toLowerCase(Locale.ROOT),
                              new IrcClient.PartyPosition(
                                 login, Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4]), System.currentTimeMillis()
                              )
                           );
                     }
                  } catch (NumberFormatException var6) {
                  }
               }
            }
         }
      }
   }

   private void handlePartyInfo(String line) {
      if (line.equals("PARTY_INFO")) {
         this.party = IrcClient.PartySnapshot.EMPTY;
         this.partyPositions.clear();
      } else {
         String[] parts = line.split("\\|");
         if (parts.length >= 3) {
            this.party = new IrcClient.PartySnapshot(parts[1], List.of(parts).subList(2, parts.length));
            this.partyPositions.keySet().removeIf(login -> !this.party.contains(login));
         }
      }

      if (this.showNextPartyInfo.getAndSet(false)) {
         IrcClient.PartySnapshot snapshot = this.party;
         String message = snapshot.members().isEmpty()
            ? "Вы не состоите в Party"
            : "Владелец: " + snapshot.owner() + " | Участники: " + String.join(", ", snapshot.members());
         showPartyMessage(message, snapshot.members().isEmpty() ? Formatting.RED : Formatting.GRAY);
      }
   }

   private static void showPartyInvitation(String owner) {
      MinecraftClient client = MinecraftClient.getInstance();
      client.execute(
         () -> {
            String prefix = CommandManager.getInstance().getPrefix();
            ChatMessage.brandmessage(Text.literal("Пользователь \"" + owner + "\" хочет добавить вас в Party!").formatted(Formatting.WHITE));
            Text actions = Text.literal("Выберите ")
               .formatted(Formatting.GRAY)
               .append(
                  Text.literal("[Принять]").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withClickEvent(new RunCommand(prefix + "party accept " + owner)))
               )
               .append(Text.literal(" или ").formatted(Formatting.GRAY))
               .append(
                  Text.literal("[Отклонить]").setStyle(Style.EMPTY.withColor(Formatting.RED).withClickEvent(new RunCommand(prefix + "party decline " + owner)))
               );
            ChatMessage.brandmessage(actions);
         }
      );
   }

   private static void showPartyMessage(String message, Formatting color) {
      MinecraftClient client = MinecraftClient.getInstance();
      client.execute(() -> ChatMessage.brandmessage(Text.literal("Party: ").formatted(Formatting.GRAY).append(Text.literal(message).formatted(color))));
   }

   private static long parseTimestamp(String value) {
      try {
         return Long.parseLong(value);
      } catch (NumberFormatException ignored) {
         return System.currentTimeMillis();
      }
   }

   private boolean sendLine(String line) {
      BufferedWriter current = this.writer;
      if (current == null) {
         return false;
      }

      synchronized (this.writerLock) {
         boolean var10000;
         try {
            current.write(line);
            current.newLine();
            current.flush();
            var10000 = true;
         } catch (IOException ignored) {
            this.closeSocket();
            return false;
         }

         return var10000;
      }
   }

   private void closeSocket() {
      Socket current = this.socket;
      if (current != null) {
         try {
            current.close();
         } catch (IOException var3) {
         }
      }
   }

   private static String sanitizeIdentity(String value, String fallback) {
      if (value == null) {
         return fallback;
      }

      String sanitized = value.replace("|", "").replaceAll("\\p{Cntrl}", "").trim();
      if (sanitized.isEmpty()) {
         return fallback;
      }

      int length = sanitized.codePointCount(0, sanitized.length());
      return length <= 24 ? sanitized : sanitized.substring(0, sanitized.offsetByCodePoints(0, 24));
   }

   public static String normalizeRole(String value) {
      if (value == null) {
         return "User";
      }

      return switch (value.trim().toLowerCase(Locale.ROOT)) {
         case "admin" -> "Admin";
         case "media" -> "Media";
         case "beta" -> "Beta";
         default -> "User";
      };
   }

   private static void sleep(long millis) {
      try {
         Thread.sleep(millis);
      } catch (InterruptedException ignored) {
         Thread.currentThread().interrupt();
      }
   }

   private static String profile(String key, String fallback) {
      try {
         String value = User.getInstance().profile(key);
         return value != null && !value.isBlank() ? value : fallback;
      } catch (Throwable ignored) {
         return fallback;
      }
   }

   public record CoordinateEvent(String login, int x, int y, int z) {
   }

   public record Event(String role, String author, String text, long timestamp) {
   }

   public record PartyPosition(String login, double x, double y, double z, long updatedAt) {
   }

   public record PartySnapshot(String owner, List<String> members) {
      private static final IrcClient.PartySnapshot EMPTY = new IrcClient.PartySnapshot("", List.of());

      public PartySnapshot {
         owner = owner == null ? "" : owner;
         members = List.copyOf(members);
      }

      public boolean contains(String login) {
         return login != null && this.members.stream().anyMatch(member -> member.equalsIgnoreCase(login));
      }
   }

   public enum State {
      CONNECTING,
      ONLINE,
      OFFLINE;
   }
}
