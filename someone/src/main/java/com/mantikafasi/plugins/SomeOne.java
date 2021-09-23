package com.mantikafasi.plugins;

import static com.discord.stores.StoreStream.getGuilds;

import android.content.Context;
import android.util.Pair;
import android.widget.TextView;
import android.widget.Toast;
import com.aliucord.utils.RxUtils;
import com.aliucord.CollectionUtils;
import com.aliucord.Logger;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.api.NotificationsAPI;
import com.aliucord.api.SettingsAPI;
import com.aliucord.entities.MessageEmbedBuilder;
import com.aliucord.entities.NotificationData;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.wrappers.GuildMemberWrapper;
import com.discord.api.guild.Guild;
import com.discord.api.guildmember.GuildMembersChunk;
import com.discord.gateway.GatewaySocket;
import com.discord.gateway.GatewaySocket$1;
import com.discord.gateway.io.OutgoingPayload;
import com.discord.models.member.GuildMember;
import com.discord.stores.StoreGatewayConnection;
import com.discord.stores.StoreGuildSubscriptions;
import com.discord.stores.StoreGuildSubscriptions$subscriptionsManager$1;
import com.discord.stores.StoreStream;
import com.discord.utilities.KotlinExtensionsKt;
import com.discord.utilities.lazy.subscriptions.GuildSubscriptionsManager;
import com.mantikafasi.plugins.ClassUtils;
import com.aliucord.wrappers.embeds.MessageEmbedWrapper;
import com.discord.models.message.Message;
import com.discord.rtcconnection.audio.DiscordAudioManager;
import com.discord.stores.ArchivedThreadsStore;
import com.discord.utilities.textprocessing.node.UserMentionNode;
import com.discord.widgets.chat.list.entries.ChatListEntry;
import com.discord.widgets.chat.list.entries.MessageEntry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

import rx.Subscriber;

@SuppressWarnings("unused")
@AliucordPlugin
public class SomeOne extends Plugin {
    int tryCount=0;
    public static final Logger logger = new Logger("SomeOne");
    StoreGatewayConnection  con;
    Context context ;
    @Override
    public void start(Context context) {
        this.context= context;


        con= StoreStream.getGatewaySocket();

        var guilds= StoreStream.getGuilds();

        long guid = Long.parseLong("811255666990907402") ;
        Map<Long, GuildMember> malp = StoreStream.getGuilds().getMembers().get(guid);








        commands.registerCommand("someone","Mention Someone",Collections.emptyList(),commandContext -> {
            Long guildID =  commandContext.getChannel().getGuildId();
            makeRequest(guildID);
            logger.info("asdasdasd");

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<GuildMembersChunk> ref = new AtomicReference<GuildMembersChunk>();
            AtomicReference<Throwable> thro = new AtomicReference<>();

            var subs =RxUtils.subscribe(con.getGuildMembersChunk(), new Subscriber<GuildMembersChunk>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable throwable) {
                    thro.set(throwable);
                    latch.countDown();
                }

                @Override
                public void onNext(GuildMembersChunk guildMembersChunk) {
                    ref.set(guildMembersChunk);
                    logger.info(guildMembersChunk.b().toString());
                    latch.countDown();
                }
            });

            //Pair<GuildMembersChunk,Throwable> obj = RxUtils.getResultBlocking((con.getGuildMembersChunk()));
            //var members = obj.first.b();
            ///

            try {
                latch.await();

            } catch (Exception e){}
            var members = ref.get().b();
            logger.info("bbbb");
            if (members.size()<1) {makeRequest(guildID);}
            tryCount=0;
            Random random = new Random();
            var member = members.get(random.nextInt(members.size()));
            GuildMemberWrapper wrapper = new GuildMemberWrapper(member);
            wrapper.getUserId();
            try {

                return new CommandsAPI.CommandResult("<@" + member.j().toString().split("id=")[1].split(",")[0] + ">",null,true);

            } catch (Exception e){
                return new CommandsAPI.CommandResult("app shitted himself", null, false);

            }


        });


    }

    public String generateRandomChar(){
        String a = "abcdefghijklmoprstuvyxwyz1234567890";
        Random random = new Random();
        return String.valueOf(a.charAt(random.nextInt(a.length()-1)));
    }
    public void makeRequest(Long guid){
        if (tryCount>3){
            logger.error(new Throwable("Couldnt Find User"));
          //return;
        }
        tryCount ++ ;
        con.requestGuildMembers(guid,generateRandomChar(),Collections.emptyList());

    }


    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }

}
