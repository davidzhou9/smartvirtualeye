package es.mrjon.bluedroidduino;

/**
 * Created by David on 7/7/2015.
 */

import java.lang.*;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;
import android.media.*;
import java.util.*;
import android.content.*;

public class SoundManager {

    private static SoundManager mInstance;

    private SoundPool mSoundPool;

    private HashMap<Integer, Integer> mSoundPoolMap;

    private Vector<Integer> mAvailibleSounds = new Vector<Integer>();
    private Vector<Integer> mKillSoundQueue = new Vector<Integer>();

    private Handler mHandler = new Handler();
    private boolean mMuted = false;

    private static final int MAX_STREAMS = 2;
    private static final int KILL_AFTER = 3000;

    public static final int SOUND_SELECT = 0;
    public static final int SOUND_LOCKED = 1;


    @SuppressLint("UseSparseArrays")
    public SoundManager(Context context) {
        mSoundPool = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 0);
        mSoundPoolMap = new HashMap<Integer, Integer>();

        loadSounds(context);
    }

    public static SoundManager getInstance(Context context){
        if(mInstance == null){
            mInstance = new SoundManager(context);
            Log.d("SPARTA", "Instanciation");
        }
        return mInstance;
    }

    /**
     * Load all sounds and put them in their respective keys.
     * @param context
     */
    public void loadSounds(Context context){
        addSound(context, SOUND_SELECT, R.raw.beep);
    }

    public void loadSounds(Context context, int num){
        if (num==0)
            return;
        else if (num<30)
            addSound(context, SOUND_SELECT, R.raw.six);
        else if (num<60)
            addSound(context, SOUND_SELECT, R.raw.five);
        else if (num<90)
            addSound(context, SOUND_SELECT, R.raw.four);
        else
            addSound(context, SOUND_SELECT, R.raw.three);
    }


    /**
     * Put the sounds to their correspondig keys in sound pool.
     * @param context
     * @param key
     * @param soundID
     */
    public void addSound(Context context, int key, int soundID) {
        mAvailibleSounds.add(key);
        mSoundPoolMap.put(key, mSoundPool.load(context, soundID, 1));
    }

    /**
     * Find sound with the key and play it
     * @param key
     */
    public void playSound(int key) {
        if(mMuted)
            return;

        //If we have the sound
        if(mAvailibleSounds.contains(key)) {

            //We play it
            int soundId = mSoundPool.play(mSoundPoolMap.get(key), 1, 1, 1, 0, 1f);

            mKillSoundQueue.add(soundId);

            //And schedule the current sound to stop after set milliseconds
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    if (!mKillSoundQueue.isEmpty()) {
                        mSoundPool.stop(mKillSoundQueue.firstElement());
                    }
                }
            }, 200);
        }
    }

    /**
     * Initialize the control stream with the activity to music
     * @param activity
     */
    public static void initStreamTypeMedia(Activity activity){
        activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    /**
     * Is sound muted
     * @param muted
     */
    public void setMuted(boolean muted) {
        this.mMuted = muted;
    }
}
