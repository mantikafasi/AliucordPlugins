package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.aliucord.Constants;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.SettingsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.utils.DimenUtils;
import com.aliucord.wrappers.ChannelWrapper;
import com.discord.stores.StoreStream;
import com.discord.utilities.color.ColorCompat;
import com.discord.widgets.chat.input.WidgetChatInputEditText$setOnTextChangedListener$1;
import com.lytefast.flexinput.fragment.FlexInputFragment;
import com.lytefast.flexinput.widget.FlexEditText;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@SuppressWarnings("unused")
@AliucordPlugin
public class VoiceMessages extends Plugin {
    private final int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    WaveFormView waveFormView;
    FlexEditText editText;
    ImageButton recordButton;
    File outputFile;
    private MediaRecorder mediaRecorder;
    private final Runnable updateWaveform = () -> {

        while (true) {

            waveFormView.addWave((int) ((mediaRecorder.getMaxAmplitude() / 32767.0) * 254) + 1); //discord uses 8 bit , explode. Also I add 1 because if we insert 0 it breaks discorc
            waveFormView.invalidate();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    };
    private Thread updateWaveformThread;
    public static SettingsAPI staticSettings;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void start(Context context) throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Utils.showToast("This plugin requires Android 10 or higher");
            return;
        }

        staticSettings = settings;

        if (settings.getString("vendorId", null) == null) {
            settings.setString("vendorId", UUID.randomUUID().toString());
        }

        settingsTab = new SettingsTab(BottomShit.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);
        waveFormView = new WaveFormView(context);
        recordButton = new ImageButton(context);

        var drawable = ContextCompat.getDrawable(context, com.lytefast.flexinput.R.e.ic_mic_grey_24dp);
        drawable.setTint(ColorCompat.getColor(context, com.lytefast.flexinput.R.c.primary_dark_300));
        recordButton.setImageDrawable(drawable);
        recordButton.setBackgroundColor(Color.TRANSPARENT);

        mediaRecorder = new MediaRecorder();

        recordButton.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    try {
                        onRecordStart();
                    } catch (IOException e) {
                        logger.error(e);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    onRecordStop(true,StoreStream.getChannelsSelected().getId());
                    return true;
                case (MotionEvent.ACTION_MOVE):
                    // check if user moved finger out of button
                    if ( motionEvent.getY() < 0 || motionEvent.getY() > view.getHeight()) {
                        onRecordStop(false,0L);
                        Utils.showToast("Cancelled recording");
                    }
                    return true;
            }
            return false;
        });


        try {
            recordButton.setVisibility(StoreStream.getChannelsSelected().getSelectedChannel().i() == 0L ? View.VISIBLE : View.GONE);
        } catch (NullPointerException ignored) {
            // if no channel is selected plugin will throw error
        }

        patcher.patch(FlexInputFragment.class.getDeclaredMethod("onViewCreated", View.class, Bundle.class), cf -> {
            var input = (FlexInputFragment) cf.thisObject;


            editText = input.getView().findViewById(Utils.getResId("text_input", "id"));

            var viewgroup = ((ViewGroup) input.getView().findViewById(Utils.getResId("main_input_container", "id")));
            viewgroup.addView(waveFormView, 0);
            waveFormView.setVisibility(View.GONE);

            var buttonViewGroup = ((ViewGroup) input.getView().findViewById(Utils.getResId("main_input_container", "id")));
            viewgroup.addView(recordButton);
            var params = (ViewGroup.LayoutParams) waveFormView.getLayoutParams();
            params.height = DimenUtils.dpToPx(30);

        });

        patcher.patch(WidgetChatInputEditText$setOnTextChangedListener$1.class.getDeclaredMethod("afterTextChanged", Editable.class), cf -> {
            if (editText.getText() == null || editText.getText().toString().equals("") && new ChannelWrapper(StoreStream.getChannelsSelected().getSelectedChannel()).isDM() ) {
                recordButton.setVisibility(View.VISIBLE);
            } else {
                recordButton.setVisibility(View.GONE);
            }
        });

        patcher.patch(StoreStream.class.getDeclaredMethod("handleChannelSelected", long.class), cf -> {
            Utils.mainThread.post(()->{
                var id = (long) cf.args[0];
                try {
                    var channel = new ChannelWrapper(StoreStream.getChannels().getChannel(id));

                    if (channel.isDM()) {
                        recordButton.setVisibility(View.VISIBLE);
                    } else {
                        recordButton.setVisibility(View.GONE);
                    }
                } catch (NullPointerException ignored) {
                    // if no channel is selected plugin will throw error
                }
            });
            /*
            if (id != 0L) {
                var meID = StoreStream.getUsers().getMe().getId();

                var guild = StoreStream.getGuilds().getGuild(StoreStream.getGuildSelected().getSelectedGuildId());
                try {

                    var stageInstances = (StoreStageInstances)ReflectUtils.getField(StoreStream.getPermissions(), "storeStageInstances");
                    var storeThreadsJoined = (StoreThreadsJoined)ReflectUtils.getField(StoreStream.getPermissions(), "storeThreadsJoined");
                    var storeChannels = (StoreChannels)ReflectUtils.getField(StoreStream.getPermissions(), "storeChannels");
                    var hasJoined = storeThreadsJoined.hasJoinedInternal(channel.getId());
                    var member = StoreStream.getGuilds().getMember(guild.getId(), meID);
                    var parentId = channel.getParentId();
                    var parentChannel = storeChannels.getGuildChannelInternal$app_productionGoogleRelease(guild.getId(), parentId);
                    //computePermissions(long meID, Channel channel, Channel parentChannel, long ownerId, GuildMember member, Map<Long, GuildRole> roles, Map<Long, StageInstance> stageInstances, boolean hasJoined)
                    var permissions = PermissionUtils.computePermissions(meID, StoreStream.getChannels().getChannel(id), parentChannel, guild.getOwnerId(), member, StoreStream.getGuilds().getRoles().get(guild.getId()), stageInstances.getStageInstancesForGuild(guild.getId()), hasJoined);
                    var mask = (1L << 46);
                    if((permissions & mask) != mask)
                        hasPerms = true;
                    Utils.showToast(String.valueOf(permissions));
                    logger.info(String.valueOf(permissions));

                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
             */

        });
        /*
        patcher.patch(AnalyticSuperProperties.class.getDeclaredMethod("getSuperProperties"), cf -> {
            var map = (Map<String, Object>) cf.getResult();
            map.put("client_version", "175.6 - rn");
            map.put("client_build_number", 175206);
            cf.setResult(map);
        });

         */

    }

    private double calculateMagnitude(short[] buffer) {

        double sum = 0;
        for (short s : buffer) {
            sum += s * s;
        }
        double rms = Math.sqrt(sum / buffer.length);
        return 20 * Math.log10(rms);
    }

    public void onRecordStart() throws IOException {
        //check permission
        if (ContextCompat.checkSelfPermission(Utils.getAppContext(), android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(Utils.getAppActivity(), new String[]{android.Manifest.permission.RECORD_AUDIO}, 1);

            return;
        }

        waveFormView.reset();

        // prepare media recorder
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.OGG);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS);

        mediaRecorder.setAudioEncodingBitRate(settings.getInt("audioQuality", 128) * 1024);
        mediaRecorder.setAudioSamplingRate(44100);

        outputFile = File.createTempFile("audio_record", ".ogg", new File(Constants.BASE_PATH));
        outputFile.deleteOnExit();
        mediaRecorder.setOutputFile(outputFile.getAbsolutePath());

        mediaRecorder.prepare();

        mediaRecorder.start();

        editText.setVisibility(View.GONE);
        waveFormView.setVisibility(View.VISIBLE);

        updateWaveformThread = new Thread(updateWaveform);
        updateWaveformThread.start();

    }

    public void onRecordStop(boolean send, long discordid) {
        try {
            mediaRecorder.stop();

            if (send) {
                Utils.threadPool.execute(() -> {
                    var waveform = waveFormView.getWaveForm();
                    var filename = DiscordAPI.uploadFile(outputFile, StoreStream.getChannelsSelected().getId());

                    // extract duration
                    Uri uri = Uri.parse(outputFile.getAbsolutePath());
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();

                    mmr.setDataSource(Utils.getAppContext(), uri);
                    String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    float seconds = (Integer.parseInt(durationStr) / 1000.0f);

                    DiscordAPI.sendVoiceMessage(filename, seconds, waveform, discordid);
                });
            }


        } catch (RuntimeException e) {
            // if you instantly stop recording it causes crash
            logger.error(e);
        }

        mediaRecorder.reset();

        waveFormView.setVisibility(View.GONE);
        editText.setVisibility(View.VISIBLE);

        if (updateWaveformThread != null && updateWaveformThread.isAlive()) {
            updateWaveformThread.interrupt();
        }
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
