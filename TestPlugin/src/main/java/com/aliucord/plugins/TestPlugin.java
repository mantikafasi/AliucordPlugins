package com.aliucord.plugins;

import static java.util.Collections.emptyList;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.discord.app.AppFragment;
import com.discord.models.domain.NonceGenerator;
import com.discord.restapi.RestAPIParams;
import com.discord.utilities.time.ClockFactory;

@SuppressWarnings("unused")
@AliucordPlugin
public class TestPlugin extends Plugin {
    @Override
    public void start(Context context) throws NoSuchMethodException {
        Utils.mainThread.postDelayed(() -> {
            var llLayout = (ViewGroup) Utils.appActivity.findViewById(android.R.id.content);
            var tw = new TextView(context);

            //llLayout.removeAllViews();


            tw.setText("I hate ven");
            llLayout.addView(tw);
        }, 4000);


        patcher.patch(AppFragment.class.getDeclaredMethod("onViewBound", View.class), new Hook(cf -> {
            try {
                var mainfragment = (AppFragment) cf.thisObject;
                var a = (ViewGroup) mainfragment.getView().getRootView();
                var tw2 = new TextView(a.getContext());

                a.addView(tw2);
                tw2.setText("LOREM IPSUM");
                tw2.setBackgroundColor(0);

                //a.removeAllViews();
                logger.info(a.toString());
            } catch (Exception e) {
                logger.error(e);
            }


        }));


        /*
        RxUtils.subscribe(RxUtils.onBackpressureBuffer(StoreStream.getGatewaySocket().getMessageCreate()), RxUtils.createActionSubscriber(message -> {
            if (message == null) return;

            Message entry = new Message(message);
            String cont = entry.getContent().toLowerCase();
            //
            if (cont.contains("i love alicord") ) {
                RxUtils.subscribe(RestAPI.getApi().createOrFetchDM(entry.getAuthor().i()),channel -> {
                    RxUtils.subscribe(RestAPI.getApi().sendMessage(ChannelWrapper.getId(channel),createMessage("I love alicord too <@"+entry.getAuthor().i()+">")),message1 -> null);
                    return null;
                });
            }
        }));

         */
    }

    public RestAPIParams.Message createMessage(String message) {
        return new RestAPIParams.Message(
                message, // Content
                String.valueOf(NonceGenerator.computeNonce(ClockFactory.get())), // Nonce
                null, // ApplicationId
                null, // Activity
                emptyList(), // stickerIds
                null, // messageReference
                new RestAPIParams.Message.AllowedMentions( // https://discord.com/developers/docs/resources/channel#allowed-mentions-object-allowed-mentions-structure
                        emptyList(), // parse
                        emptyList(), //users
                        emptyList(), // roles
                        false // repliedUser
                ), null,null
        );

    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
