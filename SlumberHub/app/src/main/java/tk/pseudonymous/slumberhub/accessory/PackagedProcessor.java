package tk.pseudonymous.slumberhub.accessory;

import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;

import com.google.code.chatterbotapi.ChatterBot;
import com.google.code.chatterbotapi.ChatterBotFactory;
import com.google.code.chatterbotapi.ChatterBotSession;
import com.google.code.chatterbotapi.ChatterBotThought;
import com.google.code.chatterbotapi.ChatterBotType;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

import tk.pseudonymous.slumberhub.MainActivity;

import static android.content.Context.KEYGUARD_SERVICE;
import static tk.pseudonymous.slumberhub.MainActivity.LogData;
import static tk.pseudonymous.slumberhub.MainActivity.bgColor;
import static tk.pseudonymous.slumberhub.MainActivity.deviceSpeak;
import static tk.pseudonymous.slumberhub.MainActivity.setSoundOff;
import static tk.pseudonymous.slumberhub.MainActivity.setSoundOn;
import static tk.pseudonymous.slumberhub.MainActivity.tryColor;
import static tk.pseudonymous.slumberhub.MainActivity.wakeUpDevice;
import static tk.pseudonymous.slumberhub.accessory.SpeechRecognition.changeActivity;
import static tk.pseudonymous.slumberhub.accessory.SpeechRecognition.runActivity;
import static tk.pseudonymous.slumberhub.accessory.SpeechRecognition.setBGColor;
import static tk.pseudonymous.slumberhub.fragments.SettingsFragment.setLightState;

/**
 * Created by David Smerkous on 12/4/16.
 * Project: SlumberHub
 */

public class PackagedProcessor {

    public static final String KEYWORDS[] = new String[] {"lumbe", "number"};

    private ChatterBotFactory chatterBotFactory;
    private ChatterBot cleverBot;
    private ChatterBotSession botSession;

    private Messenger messenger;

    public PackagedProcessor() {
        Thread initChatterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                chatterBotFactory = new ChatterBotFactory();
                try {
                    cleverBot = chatterBotFactory.create(ChatterBotType.CLEVERBOT);
                } catch (Exception e) {
                    LogData("Failed creating cleverBot instance");
                    e.printStackTrace();
                }
                botSession = cleverBot.createSession();
            }
        });

        initChatterThread.setDaemon(false);
        initChatterThread.setName("InitChatterBox");
        initChatterThread.start();

    }


    public void proccessHighestChance(String phrase) {
        phrase = phrase.replaceAll("^\\s+|\\s+$|\\*", "");

        String chunked[] = phrase.split("\\s+");

        if(phrase.contains(KEYWORDS[0])) {
            if (phrase.indexOf(KEYWORDS[0]) < 15) {
                phrase = TextUtils.join(" ", Arrays.copyOfRange(chunked, 1, chunked.length));
                LogData("PARSED PHRASE: " + phrase);
            }
        }

        if(chunked.length == 0) {
            LogData("Nothing but hey slumber found!");
            deviceSpeak(null, messenger);
            if(changeActivity != null && runActivity != null) {
                runActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setBGColor(tryColor, bgColor);
                    }
                });
            }
            return;
        }

        String wakePhrase[] = new String[] {
                "wake up",
                "upstairs",
                "wakeup",
                "im",
                "I am",
                "what"
        }, turnOfLamp[] = new String[] {
                "turn",
                "lamp",
                "light",
                "off",
                "on"
        }, creators[] = new String[] {
                "who",
                "are",
                "is",
                "your",
                "creator",
                "maker"
        }, whyproduct[] = new String[]{
                "should",
                "buy",
                "get",
                "slumber",
                "product"
        };

        boolean wakeUpPhrases[] = findKeyPhrases(chunked, wakePhrase);
        boolean turnOfPhrases[] = findKeyPhrases(chunked, turnOfLamp);
        boolean creatorPhrases[] = findKeyPhrases(chunked, creators);
        boolean buyingPhrases[] = findKeyPhrases(chunked, whyproduct);


        String toSpeakDevice = null;

        if((notFound(wakeUpPhrases, 3, 5)
                && isIn(phrase, wakePhrase[0])
                && isNotIn(phrase, wakePhrase[4]))
                && wordsLess(chunked, 4)
                && (orFound(wakeUpPhrases, 2))) {
            LogData("Waking up");
            wakeUpDevice();
        } else if(andFound(turnOfPhrases, 0, 3) && orFound(turnOfPhrases, 1, 2) &&
                notFound(turnOfPhrases, 4)) {
            LogData("Turning off the light");
            setLightState(false);
            toSpeakDevice = "Turning off the lamp";
        } else if(andFound(turnOfPhrases, 0, 4) && orFound(turnOfPhrases, 1, 2) &&
                notFound(turnOfPhrases, 3)) {
            LogData("Turning on the lights");
            setLightState(true);
            toSpeakDevice = "Turning on the lamp";
        } else if(andFound(creatorPhrases, 0, 3) && orFound(creatorPhrases, 1, 2) &&
                orFound(creatorPhrases, 4, 5)) {
            LogData("Who are your creators");
            toSpeakDevice = "The creators are Eli Smith, Usaid Malik and David Smerkous";
        } else if(andFound(buyingPhrases, 0) && orFound(buyingPhrases, 1, 2) &&
                orFound(buyingPhrases, 3, 4)) {
            LogData("Yeah of course buy this");
            toSpeakDevice = "Of course you should buy me I'm amazing";
        } else {
            LogData("Attempting cleverBot no command found");

            ChatterBotThought thoughts = new ChatterBotThought();
            thoughts.setText(phrase);

            String response = "";

            try {
                ChatterBotThought cleverResponse = botSession.think(thoughts);
                response = cleverResponse.getText();

                LogData("EMOTIONS: " + Arrays.toString(cleverResponse.getEmotions()));
            } catch (Exception e) {
                LogData("FAILED GETTING RESPONSE FROM CLEVERBOT");
                e.printStackTrace();
            }
            LogData("RESPONSE FROM CLEVERBOT: " + response);
            toSpeakDevice = response;
        }

        MainActivity.deviceSpeak(toSpeakDevice, messenger); //Try speech
        if(changeActivity != null) {
            if(runActivity != null) {
                runActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setBGColor(tryColor, bgColor);
                    }
                });
            }
        }
    }


    public boolean hasKeyWords(String fullString) {
        for(String keyWord : KEYWORDS) {
            if(fullString.contains(keyWord)) return true;
        }
        return false;
    }

    public boolean allFound(boolean phrases[]) {
        for(boolean phrase : phrases) {
            if(!phrase) return false;
        }
        return true;
    }

    public boolean anyFound(boolean phrases[]) {
        for(boolean phrase : phrases) {
            if(phrase) return true;
        }
        return false;
    }

    public boolean isIn(String full, String... parts) {
        for(String part : parts) {
            if(!full.contains(part)) return false;
        }
        return true;
    }

    public boolean isNotIn(String full, String... parts) {
        for(String part : parts) {
            if(full.contains(part)) return false;
        }
        return true;
    }

    public boolean wordsLess(String words[], int amount) {
        return (words.length < amount);
    }

    public boolean orFound(boolean phrases[], int... indexes) {
        boolean orFind = false;

        for(int i : indexes) {
                if(phrases[i]) { orFind = true; break; }
        }

        return orFind;
    }

    public boolean andFound(boolean phrases[], int... indexes) {
        boolean andFind = true;

        for(int i : indexes) {
            if(!phrases[i]) { andFind = false; break; }
        }

        return andFind;
    }

    public boolean notFound(boolean phrases[], int... indexes) {
        boolean foundPhrase = true;

        for(int i : indexes) {
            if(phrases[i]) { foundPhrase = false; break; }
        }

        return foundPhrase;

    }

    /*protected static class TextSpeechHandler extends Handler {
        private WeakReference<TextToSpeechService> mtarget;

        TextSpeechHandler(TextToSpeechService target) {
            mtarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            final TextToSpeechService target = mtarget.get();

            switch (msg.what) {
                case MSG_TTS_START_TALKING:
                    String toSpeak = (String) msg.obj;
                    if(target.mTts != null) {
                        if(toSpeak == null || toSpeak.isEmpty()) {
                            LogData("The sent SPEECH command was null or empty!");
                        } else {
                            target.mTts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                            LogData("Started speaking");
                        }
                    } else LogData("Not bound to TTS Engine yet");
                    break;

                case MSG_TTS_STOP_TALKING:
                    if(target.mTts != null) target.mTts.stop();
                    LogData("Stopped talking");
                    break;
            }
        }
    }*/

    public boolean[] findKeyPhrases(String parsedString[], String toFind[]) {
        boolean foundKeyPhrases[] = new boolean[toFind.length];

        try {
            int ind = 0;
            for (String found : toFind) {
                boolean phraseFound = false;

                for (String parsed : parsedString) {
                    if (found.contains(parsed) || parsed.contains(found)) {
                        phraseFound = true;
                        break;
                    }
                }

                foundKeyPhrases[ind] = phraseFound;
                ind++;
            }
        } catch (Throwable err) {
            LogData("Failed finding key phrases in speech ERROR: " + err.toString());
        }
        return foundKeyPhrases;
    }

    public void processRecogntion(ArrayList<Pair<String, Float>> combinations,
                                  Messenger messenger) {
        this.messenger = messenger;

        ArrayList<Pair<String, Float>> choosing = new ArrayList<>();

        for(Pair<String, Float> combo : combinations) {
            if(hasKeyWords(combo.first)) {
                choosing.add(new Pair<>(combo.first, combo.second)); //Add pair to found list
            }
        }

        if(choosing.isEmpty()) {
            LogData("Not a valid voice command!");
            MainActivity.deviceSpeak(null, messenger);
            return;
        }

        float highestChoosing = 0;
        int indexPull = 0;
        int currentIndex = 0;

        for(Pair<String, Float> chose : choosing) {
            if(chose.second > highestChoosing) indexPull = currentIndex;
        }

        Pair<String, Float> highestChance = choosing.get(indexPull);
        final String chanceString = highestChance.first;
        float chanceConfidence = highestChance.second;

        LogData("HIGHEST CHANCE STRING " + chanceString +
                " WITH CONFIDENCE OF " + chanceConfidence);


        Thread processThread = new Thread(new Runnable() {
            @Override
            public void run() {
                proccessHighestChance(chanceString);
            }
        });

        processThread.setDaemon(false);
        processThread.setName("VoiceProcessing");
        processThread.start();
    }

}
