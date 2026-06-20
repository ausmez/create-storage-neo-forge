package net.fxnt.fxntstorage.util;

public final class DeployerContext {

    public static final ThreadLocal<Boolean> DEPLOYER_ACTIVE = ThreadLocal.withInitial(() -> false);

    private DeployerContext() {}
}