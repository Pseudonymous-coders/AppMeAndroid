package com.pseudonymous.appmea.sound;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Pair;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.pseudonymous.appmea.MainActivity;
import com.rey.material.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by David Smerkous on 10/19/16.
 * Project: appmea
 */

public class SpeechRecognition {

    public static boolean speechAvailable(Context context) {
        return context.getPackageManager()
                .queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0)
                .size() > 0; //Check to see if the recognizer intent has any speech response
    }

    public static void requestPermission(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            MainActivity.LogData("No access to record audio!");
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(activity.getApplicationContext(), "Yep", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        1);
                MainActivity.LogData("Requested audio permissions");
            }
        } else {
            MainActivity.LogData("Have access to record audio...");
        }
    }

    public static String getWords(Bundle bundle) {
        if(bundle == null) {
            MainActivity.LogData("FAILED RETRIEVING ANY WORDS", true);
            return null;
        }

        if(!bundle.containsKey(SpeechRecognizer.RESULTS_RECOGNITION)) {
            MainActivity.LogData("NO INTENT FOR VOICE REGOGNITION STARTED", true);
            return null;
        }
        ArrayList<String> words = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        float confidence[] = bundle.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);


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
            MainActivity.LogData("FAILED TO GET CONFIDENCE LEVEL OF SPEECH", true);
        }

        String compiled = null;

        if(words != null) compiled = words.get(highestInd);

        return compiled;
    }

    public static SpeechRecognizer startRecognition(Context context, Snackbar snackbar,
                                                    ResultProcessor resultProcessor) {
        SpeechListener listener = new SpeechListener();
        listener.snackbar = snackbar;
        listener.resultProcessor = resultProcessor;
        SpeechRecognizer speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(listener);

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speechRecognizer.startListening(intent);

        MainActivity.LogData("STARTING LISTENING FOR SPEECH");

        return speechRecognizer;
    }


    public static boolean containsSingle(ArrayList<String> total, String tofind) {
        for(String find : total) {
            if(find.contains(tofind)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsAll(ArrayList<String> full, String[] tofind) {
        return containsAll(full, new ArrayList<>(Arrays.asList(tofind)));
    }

    public static boolean containsAll(ArrayList<String> full, ArrayList<String> tofind) {
        for(String part : tofind) {
            boolean passed = false;

            for(String find : full) {
                if(find.contains(part)) {
                    passed = true;
                    break;
                }
            }
            if(!passed) return false;
        }
        return true;
    }

    public static ArrayList<String> ProcessText(String text) {
        String toproc = text.toLowerCase();
        toproc = toproc.replaceAll("[^A-Za-z0-9 ]", "");
        MainActivity.LogData("Processed text: " + toproc);

        return new ArrayList<>(Arrays.asList(toproc.split("\\s+")));
    }

    public static String getError(int errorCode)
    {
        String message;
        switch (errorCode)
        {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

}
