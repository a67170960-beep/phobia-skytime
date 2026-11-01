package ruhack.phobia.util.render;

import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;

public final class RenderBufferPool {
   private static final ByteBuffer[] BUFFERS = new ByteBuffer[4];

   private RenderBufferPool() {
   }

   public static ByteBuffer acquire(int slot, int requiredBytes) {
      ByteBuffer buffer = BUFFERS[slot];
      if (buffer == null || buffer.capacity() < requiredBytes) {
         if (buffer != null) {
            MemoryUtil.memFree(buffer);
         }

         buffer = MemoryUtil.memAlloc(nextCapacity(requiredBytes));
         BUFFERS[slot] = buffer;
      }

      buffer.clear();
      buffer.limit(requiredBytes);
      return buffer;
   }

   private static int nextCapacity(int requiredBytes) {
      int capacity = 256;

      while (capacity < requiredBytes && capacity < 1073741824) {
         capacity <<= 1;
      }

      return Math.max(requiredBytes, capacity);
   }
}
