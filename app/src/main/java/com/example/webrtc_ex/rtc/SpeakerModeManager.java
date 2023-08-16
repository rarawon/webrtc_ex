package com.example.webrtc_ex.rtc;

import android.content.Context;
import android.media.AudioManager;

public class SpeakerModeManager {

    private Context context;
    private AudioManager audioManager;

    public SpeakerModeManager(Context context) {
        this.context = context;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    // 스피커 모드를 활성화하는 메서드
    public void enableSpeakerMode() {
        // 통신 모드로 설정하여 스피커 사용 가능하도록 함
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        // 스피커폰 활성화
        audioManager.setSpeakerphoneOn(true);
    }

    // 스피커 모드를 비활성화하는 메서드
    public void disableSpeakerMode() {
        // 스피커폰 비활성화
        audioManager.setSpeakerphoneOn(false);
        // 일반 모드로 복원
        audioManager.setMode(AudioManager.MODE_NORMAL);
    }
}