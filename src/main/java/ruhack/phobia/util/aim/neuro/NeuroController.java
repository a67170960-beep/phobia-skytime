package ruhack.phobia.util.aim.neuro;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.impl.rage.KillAura;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;
import ruhack.phobia.util.string.chat.ChatMessage;

public final class NeuroController {
   private static final NeuroController INSTANCE = new NeuroController();
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public static final int MIN_TRAIN = 150;
   public static final int RECOMMENDED_TRAIN = 8000;
   private static final float MAX_SANE_STEP = 60.0F;
   private static final float RECORD_FOV = 60.0F;
   private static final double CLINCH_DIST = 3.5;
   private static final float CLINCH_FOV = 140.0F;
   private final FakePlayerSpar spar = new FakePlayerSpar();
   private boolean registered;
   private boolean recording;
   private boolean playing;
   private volatile boolean training;
   private String recordName;
   private String playName;
   private NeuroModel model;
   private static final int MAX_STREAM = 30000;
   private static final int AUTOSAVE_TICKS = 600;
   private final List<float[]> dataset = new ArrayList<>();
   private Angle prevAngle;
   private Vec3d prevEyePos;
   private boolean lastTickRecorded;
   private boolean streamRolling;
   private int preloadedFrames;
   private int feedback;
   private int autosaveTimer;
   private int unsavedFrames;
   private int winFast;
   private int winTotal;
   private final ExecutorService saver = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "neuro-save");
      t.setDaemon(true);
      return t;
   });

   private NeuroController() {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         String name = this.recordName;
         if (this.recording && name != null) {
            float[][] snapshot = null;
            synchronized (this.dataset) {
               if (this.unsavedFrames > 0) {
                  snapshot = this.dataset.toArray(new float[0][]);
               }
            }

            if (snapshot != null) {
               float[][] data = snapshot;
               this.saver.submit(() -> NeuroStore.saveDataset(name, data));
            }
         }

         this.saver.shutdown();

         try {
            this.saver.awaitTermination(5L, TimeUnit.SECONDS);
         } catch (InterruptedException var5) {
         }
      }, "neuro-flush"));
   }

   public static NeuroController get() {
      return INSTANCE;
   }

   public void ensureRegistered() {
      if (!this.registered) {
         this.registered = true;
         EventManager.register(this);
      }
   }

   public boolean isRecording() {
      return this.recording;
   }

   public boolean isRecordingTarget(Entity entity) {
      return this.recording && this.spar.isActive() && entity == this.spar.getFake();
   }

   public boolean isPlaying() {
      return this.playing;
   }

   public boolean isTraining() {
      return this.training;
   }

   public String getRecordName() {
      return this.recordName;
   }

   public String getPlayName() {
      return this.playName;
   }

   public int getDatasetSize() {
      synchronized (this.dataset) {
         return this.dataset.size();
      }
   }

   public void record(String name) {
      this.ensureRegistered();
      if (this.recording) {
         ChatMessage.brandmessage("§7Neuro: запись уже идёт (§f" + this.recordName + "§7). Сначала §f.neuro stop§7.");
      } else if (mc.world != null && mc.player != null) {
         float[][] existing = NeuroStore.loadDataset(name);
         if (existing.length == 0 && NeuroStore.hasDataset(name)) {
            ChatMessage.brandmessage(
               "§cNeuro: датасет «§f"
                  + name
                  + "§c» есть на диске, но не читается — дозапись стёрла бы его. Почини файл или удали: §f.neuro remove "
                  + name
                  + "§c."
            );
         } else {
            if (existing.length > 0) {
               NeuroStore.backupDataset(name);
            }

            this.recordName = name;
            synchronized (this.dataset) {
               this.dataset.clear();

               for (float[] f : existing) {
                  if (f != null && f.length >= 4) {
                     this.dataset.add(new float[]{f[0], f[1], f[2], f[3], f.length > 4 ? f[4] : 1.0F});
                  }
               }

               this.preloadedFrames = this.dataset.size();
               this.unsavedFrames = 0;
            }

            this.prevAngle = null;
            this.prevEyePos = null;
            this.lastTickRecorded = false;
            this.streamRolling = false;
            this.feedback = 0;
            this.autosaveTimer = 0;
            this.winFast = 0;
            this.winTotal = 0;
            this.recording = true;
            this.playing = false;
            this.spar.spawn(name);
            if (this.preloadedFrames > 0) {
               ChatMessage.brandmessage("§aNeuro: продолжаю датасет «§f" + name + "§a» (уже §f" + this.preloadedFrames + "§a кадров).");
            } else {
               ChatMessage.brandmessage("§aNeuro: запись «§f" + name + "§a» началась.");
            }

            ChatMessage.brandmessage("§7Дерись с куклой как обычно, §fауру включать не нужно§7 — я пишу твой аим с мыши.");
            ChatMessage.brandmessage(
               "§7Кукла сама меняет темп: спокойные фазы, обычный PvP и рывки за спину — успевай и медленно вести, и резко разворачиваться."
            );
            ChatMessage.brandmessage("§7Удары по кукле на сервер не уходят, датасет автосохраняется каждые 30 сек.");
            ChatMessage.brandmessage("§7Закончишь — §f.neuro stop§7, потом §f.neuro train " + name + "§7.");
         }
      } else {
         ChatMessage.brandmessage("§cNeuro: нет мира.");
      }
   }

   public void stop() {
      boolean did = false;
      if (this.recording) {
         this.recording = false;
         this.spar.despawn();
         String name = this.recordName;
         float[][] data;
         synchronized (this.dataset) {
            data = this.dataset.toArray(new float[0][]);
            this.unsavedFrames = 0;
         }

         if (name != null && data.length > 0) {
            boolean ok;
            try {
               ok = this.saver.<Boolean>submit(() -> NeuroStore.saveDataset(name, data)).get();
            } catch (Exception e) {
               ok = false;
            }

            if (ok) {
               int fresh = Math.max(0, data.length - this.preloadedFrames);
               ChatMessage.brandmessage("§aNeuro: «§f" + name + "§a» сохранена: §f" + data.length + "§a кадров (§f+" + fresh + "§a за эту сессию).");
            } else {
               ChatMessage.brandmessage("§cNeuro: файл «§f" + name + "§c» не записался (диск/права?). На диске осталась последняя автосохранённая версия.");
            }
         } else {
            ChatMessage.brandmessage("§7Neuro: запись остановлена (пусто).");
         }

         did = true;
      }

      if (this.playing) {
         this.playing = false;
         ChatMessage.brandmessage("§7Neuro: воспроизведение остановлено (в ротации триггербот).");
         did = true;
      }

      if (!did) {
         ChatMessage.brandmessage("§7Neuro: нечего останавливать.");
      }
   }

   public void train(String name) {
      this.ensureRegistered();
      if (this.training) {
         ChatMessage.brandmessage("§7Neuro: обучение уже идёт.");
      } else {
         float[][] raw = NeuroStore.loadDataset(name);
         if (raw.length < 150) {
            ChatMessage.brandmessage(
               "§cNeuro: мало данных для «§f" + name + "§c» (§f" + raw.length + "§c/§f150§c). Запиши больше: §f.neuro record " + name + "§c."
            );
         } else {
            float[][] data = raw;
            this.training = true;
            if (raw.length < 8000) {
               ChatMessage.brandmessage(
                  "§eNeuro: в «§f"
                     + name
                     + "§e» только §f"
                     + raw.length
                     + "§e кадров — модель будет крутить одни куски по кругу, ML-АЦ это палит (нужно §f8000§e+)."
               );
               ChatMessage.brandmessage("§eДозапиши: §f.neuro record " + name + "§e — сессии складываются.");
            }

            ChatMessage.brandmessage("§aNeuro: готовлю «§f" + name + "§a» на §f" + raw.length + " §aкадрах...");
            Thread thread = new Thread(
               () -> {
                  long t0 = System.currentTimeMillis();

                  try {
                     NeuroModel m = new NeuroModel();
                     m.fit(data);

                     boolean saved;
                     try {
                        m.save(NeuroStore.modelPath(name));
                        saved = true;
                     } catch (Exception ex) {
                        saved = false;
                     }

                     if (saved) {
                        ChatMessage.brandmessage(
                           "§aNeuro: «§f"
                              + name
                              + "§a» готова за §f"
                              + (System.currentTimeMillis() - t0)
                              + " §aмс §7(кадров: §f"
                              + m.frameCount()
                              + "§7). Запускай §f.neuro play "
                              + name
                              + "§a."
                        );
                     } else {
                        ChatMessage.brandmessage(
                           "§cNeuro: модель «§f"
                              + name
                              + "§c» не записалась на диск (диск/права?) — §f.neuro play§c её не увидит. Повтори §f.neuro train "
                              + name
                              + "§c."
                        );
                     }
                  } finally {
                     this.training = false;
                  }
               },
               "neuro-train"
            );
            thread.setDaemon(true);
            thread.start();
         }
      }
   }

   public void play(String name) {
      this.ensureRegistered();
      if (!NeuroStore.hasModel(name)) {
         ChatMessage.brandmessage("§cNeuro: нет обученной модели «§f" + name + "§c». Сначала §f.neuro train " + name + "§c.");
      } else {
         NeuroModel m = new NeuroModel();
         if (m.load(NeuroStore.modelPath(name)) && m.isTrained()) {
            if (this.recording) {
               this.stop();
            }

            this.model = m;
            this.playName = name;
            this.playing = true;
            this.setAuraNeuro();
            ChatMessage.brandmessage("§aNeuro: ротацией правит модель «§f" + name + "§a».");
            if (m.frameCount() < 8000) {
               ChatMessage.brandmessage(
                  "§eNeuro: модель обучена всего на §f"
                     + m.frameCount()
                     + "§e кадрах — жди высокий детект. Дозапиши до §f8000§e+: §f.neuro record "
                     + name
                     + "§e."
               );
            }
         } else {
            ChatMessage.brandmessage("§cNeuro: модель «§f" + name + "§c» не загрузилась.");
         }
      }
   }

   public void list() {
      List<String> names = NeuroStore.list();
      if (names.isEmpty()) {
         ChatMessage.brandmessage("§7Neuro: записей нет. Создай: §f.neuro record <имя>§7.");
      } else {
         ChatMessage.brandmessage("§fNeuro — записи:");

         for (String n : names) {
            boolean ds = NeuroStore.hasDataset(n);
            boolean md = NeuroStore.hasModel(n);
            ChatMessage.brandmessage("§8- §f" + n + " §7[" + (ds ? "§aдатасет" : "§8—") + "§7/" + (md ? "§aмодель" : "§8—") + "§7]");
         }
      }
   }

   public void remove(String name) {
      if (this.recording && name.equals(this.recordName)) {
         ChatMessage.brandmessage("§cNeuro: «§f" + name + "§c» сейчас записывается — сначала §f.neuro stop§c.");
      } else {
         NeuroStore.remove(name);
         if (name.equals(this.playName)) {
            this.playing = false;
            this.model = null;
         }

         ChatMessage.brandmessage("§aNeuro: «§f" + name + "§a» удалена.");
      }
   }

   public void clear() {
      if (this.recording) {
         ChatMessage.brandmessage("§cNeuro: идёт запись «§f" + this.recordName + "§c» — сначала §f.neuro stop§c.");
      } else {
         NeuroStore.removeAll();
         this.playing = false;
         this.model = null;
         ChatMessage.brandmessage("§aNeuro: все записи удалены.");
      }
   }

   public float[] nextStep(float errYaw, float errPitch, float hitRadius) {
      return this.playing && this.model != null && this.model.isTrained() ? this.model.next(errYaw, errPitch, hitRadius) : null;
   }

   public void resetPlayback() {
      if (this.model != null) {
         this.model.resetPlayback();
      }
   }

   private void setAuraNeuro() {
      KillAura aura = KillAura.getInstance();
      if (aura != null) {
         aura.aimType.selected("Neuro");
         if (!aura.isState()) {
            aura.setState(true);
         }
      }
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (e.isSend() && this.spar.isActive()) {
         if (e.getPacket() instanceof PlayerInteractEntityC2SPacket packet && this.spar.isFakeId(packet.entityId)) {
            e.cancel();
            OtherClientPlayerEntity fake = this.spar.getFake();
            if (fake != null) {
               mc.execute(
                  () -> {
                     fake.hurtTime = 10;
                     fake.maxHurtTime = 10;
                     if (mc.world != null) {
                        mc.world
                           .playSoundClient(
                              fake.getX(), fake.getY(), fake.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, fake.getSoundCategory(), 1.0F, 1.0F, false
                           );
                     }
                  }
               );
            }
         }
      }
   }

   private void requestSave() {
      String name = this.recordName;
      if (name != null) {
         float[][] snapshot;
         synchronized (this.dataset) {
            if (this.unsavedFrames == 0 || this.dataset.isEmpty()) {
               return;
            }

            snapshot = this.dataset.toArray(new float[0][]);
            this.unsavedFrames = 0;
         }

         this.saver.submit(() -> {
            if (!NeuroStore.saveDataset(name, snapshot)) {
               synchronized (this.dataset) {
                  this.unsavedFrames++;
               }

               ChatMessage.brandmessage("§cNeuro: автосейв «§f" + name + "§c» не записался (диск/права?).");
            }
         });
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         if (this.recording) {
            OtherClientPlayerEntity fake = this.spar.getFake();
            Vec3d seenPoint = fake != null && !fake.isRemoved() ? fake.getEntityPos().add(0.0, fake.getHeight() * 0.5, 0.0) : null;
            this.spar.tick();
            if (this.spar.getFake() == null) {
               this.prevAngle = null;
               this.prevEyePos = null;
               this.lastTickRecorded = false;
            } else {
               Angle curAngle = new Angle(mc.player.getYaw(), mc.player.getPitch());
               boolean live = mc.currentScreen == null && mc.isWindowFocused() && mc.player.isAlive();
               boolean announceRoll = false;
               if (this.prevAngle != null && this.prevEyePos != null && seenPoint != null && live) {
                  Angle applied = AngleUtil.calculateDelta(this.prevAngle, curAngle);
                  Angle errSeen = AngleUtil.calculateDelta(this.prevAngle, AngleUtil.fromVec3d(seenPoint.subtract(this.prevEyePos)));
                  float fov = Math.max(Math.abs(errSeen.getYaw()), Math.abs(errSeen.getPitch()));
                  boolean sane = Math.abs(applied.getYaw()) <= 60.0F && Math.abs(applied.getPitch()) <= 60.0F;
                  boolean engaged = fov <= 60.0F || this.prevEyePos.distanceTo(seenPoint) <= 3.5 && fov <= 140.0F;
                  if (engaged && sane) {
                     synchronized (this.dataset) {
                        if (this.dataset.size() >= 30000) {
                           this.dataset.remove(0);
                           if (!this.streamRolling) {
                              this.streamRolling = true;
                              announceRoll = true;
                           }
                        }

                        this.dataset
                           .add(new float[]{applied.getYaw(), applied.getPitch(), errSeen.getYaw(), errSeen.getPitch(), this.lastTickRecorded ? 1.0F : 0.0F});
                        this.unsavedFrames++;
                     }

                     float sp = (float)Math.hypot(applied.getYaw(), applied.getPitch());
                     this.winTotal++;
                     if (sp >= 3.0F) {
                        this.winFast++;
                     }

                     this.lastTickRecorded = true;
                  } else {
                     this.lastTickRecorded = false;
                  }
               } else {
                  this.lastTickRecorded = false;
               }

               this.prevAngle = curAngle;
               this.prevEyePos = mc.player.getEyePos();
               if (announceRoll) {
                  ChatMessage.brandmessage("§7Neuro: буфер полон (§f30000§7 кадров) — старые кадры замещаются свежими.");
               }

               if (++this.feedback >= 200) {
                  this.feedback = 0;
                  int size = this.getDatasetSize();
                  int fastPct = this.winTotal > 0 ? 100 * this.winFast / this.winTotal : 0;
                  this.winFast = 0;
                  this.winTotal = 0;
                  String verdict;
                  if (fastPct > 45) {
                     verdict = "§cтемп §f" + fastPct + "%§c рывков — целься СПОКОЙНЕЕ, ведёшь роботом";
                  } else if (fastPct < 8) {
                     verdict = "§eтемп §f" + fastPct + "%§e рывков — маловато резких, добавь фликов";
                  } else {
                     verdict = "§aтемп §f" + fastPct + "%§a рывков — норм";
                  }

                  ChatMessage.brandmessage("§7Neuro: §f" + size + "§7 кадров" + (size < 8000 ? " §8(цель 8000+)" : " §a✓") + " §8· " + verdict);
               }

               if (++this.autosaveTimer >= 600) {
                  this.autosaveTimer = 0;
                  this.requestSave();
               }
            }
         }
      } else {
         if (this.recording && ++this.autosaveTimer >= 20) {
            this.autosaveTimer = 0;
            this.requestSave();
         }
      }
   }
}
