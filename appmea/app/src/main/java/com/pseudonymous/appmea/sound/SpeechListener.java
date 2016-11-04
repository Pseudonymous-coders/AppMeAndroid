package com.pseudonymous.appmea.sound;

import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.support.design.widget.Snackbar;
import android.util.Pair;

import com.pseudonymous.appmea.MainActivity;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by David Smerkous on 10/19/16.
 * Project: appmea
 */

public class SpeechListener implements RecognitionListener {
    public Snackbar snackbar = null;
    public ResultProcessor resultProcessor = null;


    private void setText(String toSet) {
        if(snackbar != null) snackbar.setText(toSet);
    }

    private void doProcess(String results) {
        if(resultProcessor != null) resultProcessor.onPreclaimedResult(results);
    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {
        MainActivity.LogData("READY FOR SPEECH");
    }

    @Override
    public void onBeginningOfSpeech() {
        MainActivity.LogData("Started speech");
        setText("Listening...");
    }

    @Override
    public void onRmsChanged(float v) {

    }

    @Override
    public void onBufferReceived(byte[] bytes) {
        MainActivity.LogData("BUFFER " + Arrays.toString(bytes));
    }

    @Override
    public void onEndOfSpeech() {
        MainActivity.LogData("Finished speech");
        setText("");
    }

    @Override
    public void onError(int i) {
        MainActivity.LogData("FAILED STT: " + SpeechRecognition.getError(i));
        setText("Couldn't get that please try again");
    }

    @Override
    public void onResults(Bundle bundle) {
        String results = SpeechRecognition.getWords(bundle);

        if(results == null) {
            MainActivity.LogData("FAILED GETTING SPEECH");
            return;
        }

        MainActivity.LogData("GOT FULL RESULT: " + results);
        setText("Processing...");
        doProcess(results);
    }

    @Override
    public void onPartialResults(Bundle bundle) {
        String results = SpeechRecognition.getWords(bundle);

        if(results == null) {
            MainActivity.LogData("FAILED GETTING PARTIAL SPEECH");
            return;
        }

        MainActivity.LogData("GOT PARTIAL RESULT: " + results);
        setText("Listening... " + results);
        doProcess(results);
    }

    @Override
    public void onEvent(int i, Bundle bundle) {}
}
