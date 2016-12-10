package tk.pseudonymous.slumberhub.accessory;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import tk.pseudonymous.slumberhub.MainActivity;

import static android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS;
import static tk.pseudonymous.slumberhub.MainActivity.LogData;
import static tk.pseudonymous.slumberhub.MainActivity.bgColor;
import static tk.pseudonymous.slumberhub.MainActivity.packagedProcessor;
import static tk.pseudonymous.slumberhub.MainActivity.tryColor;

public class SpeechRecognition extends Service
{
    //protected AudioManager mAudioManager;
    protected SpeechRecognizer mSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;


    protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));

    protected boolean mIsListening;
    protected volatile boolean mIsCountDownOn;
    private static boolean mIsStreamSolo;

    public static View changeActivity = null;
    public static Activity runActivity = null;


    public static final int
            MSG_RECOGNIZER_START_LISTENING = 1,
            MSG_RECOGNIZER_CANCEL = 2;


    @Override
    public void onCreate()
    {
        super.onCreate();
        //mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        initializeSpeech();

        LogData("CREATED SERVICE");
    }

    private void initializeSpeech() {
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        //mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_)
    }

    protected static class IncomingHandler extends Handler
    {
        private WeakReference<SpeechRecognition> mtarget;

        IncomingHandler(SpeechRecognition target)
        {
            mtarget = new WeakReference<>(target);
        }


        @Override
        public void handleMessage(Message msg)
        {
            final SpeechRecognition target = mtarget.get();

            switch (msg.what)
            {
                case MSG_RECOGNIZER_START_LISTENING:
                    LogData("Starting the listener!");
                    if (!target.mIsListening)
                    {

                        target.mSpeechRecognizer.startListening(target.mSpeechRecognizerIntent);
                        target.mIsListening = true;
                        //Log.d(TAG, "message start listening"); //$NON-NLS-1$
                    } else LogData("Listener already listening");
                    break;

                case MSG_RECOGNIZER_CANCEL:
                    target.mSpeechRecognizer.cancel();
                    target.mIsListening = false;
                    //Log.d(TAG, "message canceled recognizer"); //$NON-NLS-1$
                    break;
            }
        }
    }

    // Count down timer for Jelly Bean work around
    protected CountDownTimer mNoSpeechCountDown = new CountDownTimer(5000, 5000)
    {

        @Override
        public void onTick(long millisUntilFinished)
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void onFinish()
        {
            LogData("Ticking down");
            mIsCountDownOn = false;
            Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
            try
            {
                mServerMessenger.send(message);
                message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                mServerMessenger.send(message);
            }
            catch (RemoteException e)
            {
                LogData("Failed speech: " + e.toString());
            }
        }
    };

    @Override
    public void onDestroy()
    {
        LogData("Destroyed");
        super.onDestroy();

        if (mIsCountDownOn)
        {
            mNoSpeechCountDown.cancel();
        }
        if (mSpeechRecognizer != null)
        {
            mSpeechRecognizer.destroy();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        LogData("BOUND RECOGNITION SERVICE");

        return mServerMessenger.getBinder();
    }

    public static boolean isfaded = true;


    public static void setBGColor(String colorStart, String colorEnd) {
        if(changeActivity != null) {
            ObjectAnimator colorFade = ObjectAnimator.ofObject(changeActivity,
                    "backgroundColor", new ArgbEvaluator(),
                    Color.parseColor(colorStart), Color.parseColor(colorEnd));
            colorFade.setDuration(500);
            colorFade.start();
        }
    }

    protected class SpeechRecognitionListener implements RecognitionListener
    {

        @Override
        public void onBeginningOfSpeech()
        {
            LogData("STARTED SPEECH");
            // speech input will be processed, so there is no need for count down anymore
            if (mIsCountDownOn)
            {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
            //Log.d(TAG, "onBeginingOfSpeech"); //$NON-NLS-1$
        }

        @Override
        public void onBufferReceived(byte[] buffer)
        {

        }

        @Override
        public void onEndOfSpeech()
        {
            LogData("End of speech");

            //Log.d(TAG, "onEndOfSpeech"); //$NON-NLS-1$
        }

        @Override
        public void onError(int error)
        {
            LogData("ERROR on speech " + error);

            //Just failed recognition is 7
            //Restart service on error
            if(error != 7) {
                mSpeechRecognizer.destroy();
                mSpeechRecognizerIntent = null;
                mSpeechRecognizer = null;
                initializeSpeech();
                mIsListening = false;
            }

            if (mIsCountDownOn)
            {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }

            mIsListening = false;
            Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
            try
            {
                mServerMessenger.send(message);
            }
            catch (RemoteException e)
            {

            }
            //Log.d(TAG, "error = " + error); //$NON-NLS-1$
        }

        @Override
        public void onEvent(int eventType, Bundle params)
        {

        }

        @Override
        public void onPartialResults(Bundle partialResults)
        {
            ArrayList<String> results = partialResults.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);

            if(results == null || results.isEmpty()) {
                LogData("FAILED PARTIAL RESULTS");
                return;
            }

            for(String result : results) {
                result = result.toLowerCase().replaceAll("[^a-z0-9]", "");
                if(result.contains("lumber") || result.contains("number")) {
                    LogData("FOUND HEY SLUMBER IN PARTIAL RESULTS");
                    if(isfaded) {
                        setBGColor(bgColor, tryColor);
                        isfaded = false;
                    }
                } else LogData("Can't change background color since it's null");
            }
            LogData("PARTIAL RESULT: " + results.get(0));

        }

        @Override
        public void onReadyForSpeech(Bundle params)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            {
                //mIsCountDownOn = true;
                //mNoSpeechCountDown.start();
                //mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
            }
            LogData("Ready for speech");
        }

        public ArrayList<Pair<String, Float>> getWords(Bundle bundle) {
            if(bundle == null) {
                MainActivity.LogData("FAILED RETRIEVING ANY WORDS");
                return null;
            }

            if(!bundle.containsKey(SpeechRecognizer.RESULTS_RECOGNITION)) {
                MainActivity.LogData("NO INTENT FOR VOICE REGOGNITION STARTED");
                return null;
            }

            ArrayList<String> tries = bundle.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
            float confidence[] = bundle.getFloatArray(
                    SpeechRecognizer.CONFIDENCE_SCORES);

            ArrayList<Pair<String, Float>> combos = new ArrayList<>();


            if(tries == null || confidence == null) return combos;

            LogData("Got result count: " + tries.size() + " AND " + confidence.length);

            int toIndex = (tries.size() <= confidence.length) ? tries.size() : confidence.length;

            for(int ind = 0; ind < toIndex; ind++) {
                combos.add(new Pair<>(tries.get(ind).toLowerCase()
                        .replaceAll("[^a-zA-Z0-9 ]", ""), confidence[ind]));
            }

            /*


            int highestInd = 0;

            if(confidence != null) {
                float highest = 0;
                int ind = 0;
                for (float current : confidence) {
                    if(current > highest) {
                        highest = current;
                        highestInd = ind;
                    }
                    ind += 1;
                }
            } else {
                MainActivity.LogData("FAILED TO GET CONFIDENCE LEVEL OF SPEECH");
            }

            String compiled = null;

            if(words != null) compiled = words.get(highestInd);*/

            return combos;
        }

        //Send the message to the handler to listen again
        public void startListening() {
            mIsListening = false;
            Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
            try
            {
                mServerMessenger.send(message);
            }
            catch (RemoteException e)
            {
                LogData("Failed setting speech to listen again");
            }
        }

        /*public void deviceSpeak(final String toSpeak) {
            LogData("SPEAKING: " + toSpeak);
            MainActivity.setSoundOn();

            if(MainActivity.tts != null) {
                Thread speechThread = new Thread(new Runnable() {
                    @SuppressWarnings("deprecation")
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(100); //Wait for the sound to finish
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        MainActivity.tts.setOnUtteranceProgressListener(
                                new UtteranceProgressListener() {
                            @Override
                            public void onStart(String utteranceId) {
                                LogData("Started speaking: " + utteranceId);
                            }

                            @Override
                            public void onDone(String utteranceId) {
                                LogData("Finished speaking turning off sound");
                                MainActivity.setSoundOff();
                                startListening(); //As soon as it's done
                                // talking start voice recognition again
                            }

                            @Override
                            public void onError(String utteranceId) {
                                LogData("Failed speaking: " + utteranceId);
                            }
                        });

                        MainActivity.tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                    }
                });
                speechThread.setDaemon(true);
                speechThread.setName("TextToSpeechThread");
                speechThread.start();
            } else {
                LogData("CANNOT SPEAK: " + toSpeak + " NULL DATA");
            }
        }*/

        @Override
        public void onResults(Bundle bundle)
        {
            ArrayList<Pair<String, Float>> results = getWords(bundle);

            if(results == null || results.isEmpty()) {
                MainActivity.LogData("FAILED GETTING SPEECH");
                return;
            }

            LogData("On results (Highest chance INITIALIZE): " + results.get(0).first);

            mIsListening = false;

            isfaded = true;

            try {
                MainActivity.packagedProcessor.processRecogntion(results, mServerMessenger);
            } catch (Throwable ignored) {
                LogData("Error getting speech... Trying original method");
                Message msg = new Message();
                msg.what = SpeechRecognition.MSG_RECOGNIZER_START_LISTENING;

                try {
                    mServerMessenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                setBGColor(tryColor, bgColor);
            }
            /*
            if (mIsCountDownOn)
            {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }*/

            //Log.d(TAG, "onResults"); //$NON-NLS-1$

        }

        @Override
        public void onRmsChanged(float rmsdB)
        {

        }

    }
}