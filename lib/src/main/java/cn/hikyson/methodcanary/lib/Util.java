package cn.hikyson.methodcanary.lib;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    public static final Pattern PATTERN_THREAD = Pattern.compile("^\\[THREAD]id=(\\d*);name=(.*);priority=(\\d*)$");
    public static final Pattern PATTERN_METHOD_ENTER = Pattern.compile("^PUSH:et=(\\d*);cn=(.*);ma=(-?\\d*);mn=(.*);md=(.*)$");
    public static final Pattern PATTERN_METHOD_EXIT = Pattern.compile("^POP:et=(\\d*);cn=(.*);ma=(-?\\d*);mn=(.*);md=(.*)$");
    public static final String START_THREAD = "[THREAD]";
    public static final String START_METHOD_ENTER = "PUSH:";
    public static final String START_METHOD_EXIT = "POP:";

    static boolean mergeInToFile(File sourceFile, Map<ThreadInfo, List<MethodEvent>> old) {
        Map<ThreadInfo, List<MethodEvent>> methodEventMap = new HashMap<>(old);
        if (!createOrExistsFile(sourceFile)) {
            return false;
        }
        File tmp = new File(sourceFile.getParentFile(), sourceFile.getName() + "~");
        if (tmp.exists() && !tmp.delete()) {
            return false;
        }
        BufferedSource bufferedSource;
        BufferedSink bufferedSink;
        try {
            bufferedSource = Okio.buffer(Okio.source(sourceFile));
            bufferedSink = Okio.buffer(Okio.sink(tmp));
            ThreadInfo currentThreadInfo = null;
            boolean shouldInsertToCurrentThread = false;
            while (true) {
                String line = bufferedSource.readUtf8Line();
                if (line == null || line.startsWith(START_THREAD)) {
                    if (shouldInsertToCurrentThread) {
                        List<MethodEvent> currentMethodEvents = methodEventMap.remove(currentThreadInfo);
                        if (currentMethodEvents != null && !currentMethodEvents.isEmpty()) {
                            for (MethodEvent methodEvent : currentMethodEvents) {
                                bufferedSink.writeUtf8(methodEvent + "\n");
                            }
                        }
                    }
                    if (line == null) {
                        break;
                    }
                    bufferedSink.writeUtf8(line + "\n");
                    Matcher m = PATTERN_THREAD.matcher(line);
                    if (m.find()) {
                        long id = Long.parseLong(m.group(1));
                        String name = m.group(2);
                        int priority = Integer.parseInt(m.group(3));
                        currentThreadInfo = new ThreadInfo(id, name, priority);
                        //如果当前线程信息和需要插入的方法线程一致的话，说明合并的方法都需要插入到该线程信息下
                        shouldInsertToCurrentThread = methodEventMap.containsKey(currentThreadInfo);
                    } else {
                        throw new IllegalStateException("illegal format for [THREAD] line:" + line);
                    }
                } else {
                    bufferedSink.writeUtf8(line + "\n");
                }
            }
            bufferedSink.writeUtf8(serializeMethodEvent(methodEventMap));
            bufferedSink.flush();
        } catch (Throwable e) {
            return false;
        }
        closeSilently(bufferedSource);
        closeSilently(bufferedSink);
        if (!sourceFile.delete()) {
            return false;
        }
        return tmp.renameTo(sourceFile);
    }

    private static String serializeMethodEvent(Map<ThreadInfo, List<MethodEvent>> methodEventMap) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<ThreadInfo, List<MethodEvent>> entry : methodEventMap.entrySet()) {
            sb.append(entry.getKey()).append("\n");
            List<MethodEvent> mes = entry.getValue();
            for (MethodEvent methodEvent : mes) {
                sb.append(methodEvent).append("\n");
            }
        }
        return String.valueOf(sb);
    }

    static boolean writeFileFromBytesByChannel(final File file,
                                               final byte[] bytes,
                                               final boolean append,
                                               final boolean isForce) {
        if (bytes == null) {
            return false;
        }
        FileChannel fc = null;
        try {
            fc = new FileOutputStream(file, append).getChannel();
            fc.position(fc.size());
            fc.write(ByteBuffer.wrap(bytes));
            if (isForce) {
                fc.force(true);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (fc != null) {
                    fc.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean createOrExistsFile(final File file) {
        if (file == null) return false;
        if (file.exists()) return file.isFile();
        if (!createOrExistsDir(file.getParentFile())) return false;
        try {
            return file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean createOrExistsDir(final File file) {
        return file != null && (file.exists() ? file.isDirectory() : file.mkdirs());
    }

    static boolean writeFileFromBytesByStream(final File file,
                                              final byte[] bytes,
                                              final boolean append) {
        if (bytes == null || !createOrExistsFile(file)) return false;
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file, append));
            bos.write(bytes);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void closeSilently(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException var2) {
            }
        }
    }

    static boolean isPageType(Object o) {
        return o instanceof Activity || o instanceof Fragment || o instanceof android.app.Fragment;
    }

}
