package com.example.webrtc_ex.rtc;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

class SimpleSdpObserver implements SdpObserver {

    //SDP(Session Description Protocol) 작업의 성공 및 실패에 대한 결과를 처리하기 위한 목적으로 사용되는 클래스

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        // SDP 생성에 성공했을 때 호출되는 메서드
        // 주로 로그 출력 또는 다른 처리 작업을 수행하는데 사용될 수 있음
    }

    @Override
    public void onSetSuccess() {
        // SDP 설정에 성공했을 때 호출되는 메서드
        // 주로 로그 출력 또는 다른 처리 작업을 수행하는데 사용될 수 있음
    }

    @Override
    public void onCreateFailure(String s) {
        // SDP 생성에 실패했을 때 호출되는 메서드
        // 실패 원인을 나타내는 메시지 문자열 s를 받아 처리할 수 있음
    }

    @Override
    public void onSetFailure(String s) {
        // SDP 설정에 실패했을 때 호출되는 메서드
        // 실패 원인을 나타내는 메시지 문자열 s를 받아 처리할 수 있음
    }
}
