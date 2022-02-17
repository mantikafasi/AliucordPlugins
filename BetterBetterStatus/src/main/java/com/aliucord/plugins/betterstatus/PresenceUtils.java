/*package com.aliucord.plugins.betterstatus;

import c0.z.d.m;
import com.discord.api.activity.Activity;
import com.discord.api.activity.ActivityType;
import com.discord.api.presence.ClientStatus;
import com.discord.api.presence.ClientStatuses;
import com.discord.models.presence.Presence;

import java.util.List;

public final class PresenceUtils {
    public static final PresenceUtils INSTANCE = new PresenceUtils();

    public final boolean isDesktop(ClientStatuses clientStatuses)
    {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.ONLINE;
        return a == clientStatus && clientStatuses.b() != clientStatus && clientStatuses.c() != clientStatus;
    }

    public final boolean isDesktopAndMobile(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.ONLINE;
        return a == clientStatus && clientStatuses.b() == clientStatus && clientStatuses.c() != clientStatus;
    }

    public final boolean isDesktopAndMobileDND(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.DND;
        return a == clientStatus && clientStatuses.b() == clientStatus && clientStatuses.c() != clientStatus;
    }

    public final boolean isDesktopAndMobileIDLE(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.IDLE;
        return a == clientStatus && clientStatuses.b() == clientStatus && clientStatuses.c() != clientStatus;
    }

    public final boolean isDesktopAndWeb(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.ONLINE;
        return a == clientStatus && clientStatuses.c() == clientStatus && clientStatuses.b() != clientStatus;
    }

    public final boolean isDesktopAndWebDND(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.DND;
        return a == clientStatus && clientStatuses.c() == clientStatus && clientStatuses.b() != clientStatus;
    }

    public final boolean isDesktopAndWebIDLE(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.IDLE;
        return a == clientStatus && clientStatuses.c() == clientStatus && clientStatuses.b() != clientStatus;
    }

    public final boolean isDesktopDND(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.DND;
        return a == clientStatus && clientStatuses.b() != clientStatus && clientStatuses.c() != clientStatus;
    }

    public final boolean isDesktopIDLE(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.IDLE;
        return a == clientStatus && clientStatuses.b() != clientStatus && clientStatuses.c() != clientStatus;
    }

    public final boolean isDesktopWebMobile(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.ONLINE;
        return a == clientStatus && clientStatuses.c() == clientStatus && clientStatuses.b() == clientStatus;
    }

    public final boolean isDesktopWebMobileDND(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.DND;
        return a == clientStatus && clientStatuses.c() == clientStatus && clientStatuses.b() == clientStatus;
    }

    public final boolean isDesktopWebMobileIDLE(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.IDLE;
        return a == clientStatus && clientStatuses.c() == clientStatus && clientStatuses.b() == clientStatus;
    }

    public final boolean isMobile(final ClientStatuses clientStatuses) {
        final ClientStatus b = clientStatuses.b();
        final ClientStatus online = ClientStatus.ONLINE;
        return b == online && clientStatuses.c() != online && clientStatuses.a() != online;
    }

    public final boolean isMobileDND(final ClientStatuses clientStatuses) {
        final ClientStatus b = clientStatuses.b();
        final ClientStatus dnd = ClientStatus.DND;
        return b == dnd && clientStatuses.c() != dnd && clientStatuses.a() != dnd;
    }

    public final boolean isMobileIDLE(final ClientStatuses clientStatuses) {
        final ClientStatus b = clientStatuses.b();
        final ClientStatus idle = ClientStatus.IDLE;
        return b == idle && clientStatuses.c() != idle && clientStatuses.a() != idle;
    }

    public final boolean isWeb(final ClientStatuses clientStatuses) {
        final ClientStatus c = clientStatuses.c();
        final ClientStatus online = ClientStatus.ONLINE;
        return c == online && clientStatuses.b() != online && clientStatuses.a() != online;
    }

    public final boolean isWebDND(final ClientStatuses clientStatuses) {
        final ClientStatus c = clientStatuses.c();
        final ClientStatus dnd = ClientStatus.DND;
        return c == dnd && clientStatuses.b() != dnd && clientStatuses.a() != dnd;
    }

    public final boolean isWebIDLE(final ClientStatuses clientStatuses) {
        final ClientStatus c = clientStatuses.c();
        final ClientStatus idle = ClientStatus.IDLE;
        return c == idle && clientStatuses.b() != idle && clientStatuses.a() != idle;
    }

    public final boolean isWebMobile(final ClientStatuses clientStatuses) {
        final ClientStatus c = clientStatuses.c();
        final ClientStatus online = ClientStatus.ONLINE;
        return c == online && clientStatuses.b() == online && clientStatuses.a() != online;
    }

    public final boolean isWebMobileDND(final ClientStatuses clientStatuses) {
        final ClientStatus c = clientStatuses.c();
        final ClientStatus dnd = ClientStatus.DND;
        return c == dnd && clientStatuses.b() == dnd && clientStatuses.a() != dnd;
    }

    public final boolean isWebMobileIDLE(final ClientStatuses clientStatuses) {
        final ClientStatus c = clientStatuses.c();
        final ClientStatus idle = ClientStatus.IDLE;
        return c == idle && clientStatuses.b() == idle && clientStatuses.a() != idle;
    }

    public final boolean isDesktopWebMobileOFFLINE() {
        ClientStatuses clientStatuses = null;
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.OFFLINE;
        return a == null && clientStatuses.c() == null && clientStatuses.b() == null;
    }

    public final Activity getActivityByType(final List<Activity> list, final ActivityType activityType)
    {
        return null;
    }
}*/


package com.aliucord.plugins.betterstatus;

import com.discord.api.presence.ClientStatus;
import com.discord.api.presence.ClientStatuses;

public final class PresenceUtils {

    public static boolean isDesktop(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.ONLINE;
        return a == clientStatus && clientStatuses.b() != clientStatus && clientStatuses.c() != clientStatus;
    }

    public static boolean isDesktopAndMobile(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.ONLINE;
        return a == clientStatus && clientStatuses.b() == clientStatus && clientStatuses.c() != clientStatus;
    }

    public static boolean isDesktopAndMobileDND(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.DND;
        return a == clientStatus && clientStatuses.b() == clientStatus && clientStatuses.c() != clientStatus;
    }

    public static boolean isDesktopAndMobileIDLE(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.IDLE;
        return a == clientStatus && clientStatuses.b() == clientStatus && clientStatuses.c() != clientStatus;
    }

    public static boolean isDesktopAndWeb(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.ONLINE;
        return a == clientStatus && clientStatuses.c() == clientStatus && clientStatuses.b() != clientStatus;
    }

    public static boolean isDesktopAndWebDND(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.DND;
        return a == clientStatus && clientStatuses.c() == clientStatus && clientStatuses.b() != clientStatus;
    }

    public static boolean isDesktopAndWebIDLE(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.IDLE;
        return a == clientStatus && clientStatuses.c() == clientStatus && clientStatuses.b() != clientStatus;
    }

    public static boolean isDesktopDND(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.DND;
        return a == clientStatus && clientStatuses.b() != clientStatus && clientStatuses.c() != clientStatus;
    }

    public static boolean isDesktopIDLE(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.IDLE;
        return a == clientStatus && clientStatuses.b() != clientStatus && clientStatuses.c() != clientStatus;
    }

    public static boolean isDesktopWebMobile(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.ONLINE;
        return a == clientStatus && clientStatuses.c() == clientStatus && clientStatuses.b() == clientStatus;
    }

    public static boolean isDesktopWebMobileDND(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.DND;
        return a == clientStatus && clientStatuses.c() == clientStatus && clientStatuses.b() == clientStatus;
    }

    public static boolean isDesktopWebMobileIDLE(ClientStatuses clientStatuses) {
        ClientStatus a = clientStatuses.a();
        ClientStatus clientStatus = ClientStatus.IDLE;
        return a == clientStatus && clientStatuses.c() == clientStatus && clientStatuses.b() == clientStatus;
    }

    public static boolean isMobile(final ClientStatuses clientStatuses) {
        final ClientStatus b = clientStatuses.b();
        final ClientStatus online = ClientStatus.ONLINE;
        return b == online && clientStatuses.c() != online && clientStatuses.a() != online;
    }

    public static boolean isMobileDND(final ClientStatuses clientStatuses) {
        final ClientStatus b = clientStatuses.b();
        final ClientStatus dnd = ClientStatus.DND;
        return b == dnd && clientStatuses.c() != dnd && clientStatuses.a() != dnd;
    }

    public static boolean isMobileIDLE(final ClientStatuses clientStatuses) {
        final ClientStatus b = clientStatuses.b();
        final ClientStatus idle = ClientStatus.IDLE;
        return b == idle && clientStatuses.c() != idle && clientStatuses.a() != idle;
    }

    public static boolean isWeb(final ClientStatuses clientStatuses) {
        final ClientStatus c = clientStatuses.c();
        final ClientStatus online = ClientStatus.ONLINE;
        return c == online && clientStatuses.b() != online && clientStatuses.a() != online;
    }

    public static boolean isWebDND(final ClientStatuses clientStatuses) {
        final ClientStatus c = clientStatuses.c();
        final ClientStatus dnd = ClientStatus.DND;
        return c == dnd && clientStatuses.b() != dnd && clientStatuses.a() != dnd;
    }

    public static boolean isWebIDLE(final ClientStatuses clientStatuses) {
        final ClientStatus c = clientStatuses.c();
        final ClientStatus idle = ClientStatus.IDLE;
        return c == idle && clientStatuses.b() != idle && clientStatuses.a() != idle;
    }

    public static boolean isWebMobile(final ClientStatuses clientStatuses) {
        final ClientStatus c = clientStatuses.c();
        final ClientStatus online = ClientStatus.ONLINE;
        return c == online && clientStatuses.b() == online && clientStatuses.a() != online;
    }

    public static boolean isWebMobileDND(final ClientStatuses clientStatuses) {
        final ClientStatus c = clientStatuses.c();
        final ClientStatus dnd = ClientStatus.DND;
        return c == dnd && clientStatuses.b() == dnd && clientStatuses.a() != dnd;
    }

    public static boolean isWebMobileIDLE(final ClientStatuses clientStatuses) {
        final ClientStatus c = clientStatuses.c();
        final ClientStatus idle = ClientStatus.IDLE;
        return c == idle && clientStatuses.b() == idle && clientStatuses.a() != idle;
    }

    public static boolean isCompletelyOffline(ClientStatuses statuses) {
        return statuses.a() == ClientStatus.OFFLINE && statuses.b() == ClientStatus.OFFLINE && statuses.c() == ClientStatus.OFFLINE;
    }


}