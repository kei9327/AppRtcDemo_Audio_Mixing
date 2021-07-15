package io.github.kei9327.webrtc.util;

import org.webrtc.EglBase;

import java.util.HashSet;
import java.util.Set;

public class EglBaseProvider {
    private static final String RELEASE_MESSAGE_TEMPLATE =
            "EglBaseProvider released %s " + "unavailable";
    private static volatile EglBaseProvider instance;
    private static volatile Set<Object> eglBaseProviderOwners = new HashSet<>();

    private EglBase rootEglBase;
    private EglBase localEglBase;
    private EglBase remoteEglBase;

    public static EglBaseProvider instance(Object owner) {
        synchronized (EglBaseProvider.class) {
            if (instance == null) {
                instance = new EglBaseProvider();
            }
            eglBaseProviderOwners.add(owner);

            return instance;
        }
    }

    public EglBase getRootEglBase() {
        synchronized (EglBaseProvider.class) {
            checkReleased("getRootEglBase");
            return instance.rootEglBase;
        }
    }

    public EglBase getLocalEglBase() {
        synchronized (EglBaseProvider.class) {
            checkReleased("getLocalEglBase");
            return instance.localEglBase;
        }
    }

    public EglBase getRemoteEglBase() {
        synchronized (EglBaseProvider.class) {
            checkReleased("getRemoteEglBase");
            return instance.remoteEglBase;
        }
    }

    public void release(Object owner) {
        synchronized (EglBaseProvider.class) {
            eglBaseProviderOwners.remove(owner);
            if (instance != null && eglBaseProviderOwners.isEmpty()) {
                instance.remoteEglBase.release();
                instance.remoteEglBase = null;
                instance.localEglBase.release();
                instance.localEglBase = null;
                instance.rootEglBase.release();
                instance.rootEglBase = null;
                instance = null;
            }
        }
    }

    public static void waitForNoOwners() {
        while (true) {
            synchronized (EglBaseProvider.class) {
                if (eglBaseProviderOwners.isEmpty()) {
                    break;
                }
            }
        }
    }

    private EglBaseProvider() {
        rootEglBase = EglBase.create();
        localEglBase = EglBase.create(rootEglBase.getEglBaseContext());
        remoteEglBase = EglBase.create(rootEglBase.getEglBaseContext());
    }

    private void checkReleased(String methodName) {
        if (instance == null) {
            String releaseErrorMessage = String.format(RELEASE_MESSAGE_TEMPLATE, methodName);

            throw new IllegalStateException(releaseErrorMessage);
        }
    }
}
