package com.example.webrtc_ex.rtc;

import static org.webrtc.SessionDescription.Type.ANSWER;
import static org.webrtc.SessionDescription.Type.OFFER;

import static io.socket.client.Socket.EVENT_CONNECT;
import static io.socket.client.Socket.EVENT_DISCONNECT;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.webrtc_ex.R;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;

import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;

import org.webrtc.VideoRenderer;

import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.socket.client.IO;
import io.socket.client.Socket;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class webrtcActivity extends AppCompatActivity {

    private static final String TAG = "webrtcActivity";

    // 방 이름
    private String mRoomName;
    // 로컬 비디오 뷰를 나타내는 SurfaceViewRenderer 변수
    private SurfaceViewRenderer mLocalVideoView;

    // 원격 비디오 뷰를 나타내는 SurfaceViewRenderer 변수
    private SurfaceViewRenderer mRemoteVideoView;

    // 화상 통화 액티비티를 위한 요청 코드
    private static final int RC_CALL = 111;

    // 비디오 트랙 식별자
    public static final String VIDEO_TRACK_ID = "ARDAMSv0";

    // 비디오 해상도 너비
    public static final int VIDEO_RESOLUTION_WIDTH = 1280;

    // 비디오 해상도 높이
    public static final int VIDEO_RESOLUTION_HEIGHT = 720;

    // 초당 프레임 수
    public static final int FPS = 30;

    // 서버와 통신을 위한 소켓 변수
    private Socket socket;

    // 소켓 옵션 설정을 위한 변수
    private IO.Options options;

    // 현재 통화를 시작하는 주체인지 여부를 나타내는 변수
    private boolean isInitiator;

    // 통화 채널이 준비되었는지 여부를 나타내는 변수
    private boolean isChannelReady;

    // 통화가 시작되었는지 여부를 나타내는 변수
    private boolean isStarted;

    // 오디오 제약 조건을 나타내는 변수
    MediaConstraints audioConstraints;

    // 비디오 제약 조건을 나타내는 변수
    MediaConstraints videoConstraints;

    // SDP 제약 조건을 나타내는 변수
    MediaConstraints sdpConstraints;

    // 비디오 소스를 나타내는 변수
    VideoSource videoSource;

    // 로컬 비디오 트랙을 나타내는 변수
    VideoTrack localVideoTrack;

    // 오디오 소스를 나타내는 변수
    AudioSource audioSource;

    // 로컬 오디오 트랙을 나타내는 변수
    AudioTrack localAudioTrack;

    // 비디오 데이터를 처리하기 위한 SurfaceTextureHelper 변수
    SurfaceTextureHelper surfaceTextureHelper;

    // 피어 연결을 나타내는 변수
    private PeerConnection peerConnection;

    // EGL 컨텍스트를 나타내는 변수
    private EglBase rootEglBase;

    // 피어 연결을 생성하기 위한 팩토리 변수
    private PeerConnectionFactory factory;

    // 카메라로부터 받은 비디오 트랙을 나타내는 변수
    private VideoTrack videoTrackFromCamera;
    private MediaStream mediaStream;


    ImageView switchBtn;
    ImageView micCtl;
    ImageView exitBtn;

    private VideoCapturer videoCapturer;


    private boolean isMicMuted = false; // 마이크 음소거 상태를 나타내는 변수
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean enableAudio = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webrtc);

        mRoomName = getIntent().getExtras().getString("ROOM_NAME");
        if (mRoomName == null) {
            this.finish();
        }

        switchBtn = findViewById(R.id.switchbtn);
        micCtl = findViewById(R.id.mic_ctl);
        exitBtn = findViewById(R.id.exit);

        switchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // switchBtn을 클릭했을 때의 동작을 처리하는 코드를 작성하세요
                // 예: 전면/후면 카메라 전환 기능 등
                Toast.makeText(webrtcActivity.this, "화면 전환", Toast.LENGTH_SHORT).show();
                switchCamera();
            }
        });

        micCtl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // micCtl을 클릭했을 때의 동작을 처리하는 코드를 작성하세요
                // 예: 마이크 뮤트/언뮤트 기능 등
                Toast.makeText(webrtcActivity.this, "음소거", Toast.LENGTH_SHORT).show();

                boolean enabled = onToggleMic();
                micCtl.setAlpha(enabled ? 1.0f : 0.3f);
            }
        });

        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // exitBtn을 클릭했을 때의 동작을 처리하는 코드를 작성하세요
                // 예: 앱 종료 기능 등
                Toast.makeText(webrtcActivity.this, "나가기", Toast.LENGTH_SHORT).show();


                hangup();

                // 액티비티 종료
                finish();

            }
        });

        // 시작
        start();

    }

    private void hangup() {
        Log.d(TAG, "hangup");

        socket.emit("bye");
        socket.close();

        isInitiator = false;
        isChannelReady = false;
        isStarted = false;

        // WebRTC 통화 종료 및 관련 리소스 해제
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection.dispose();
            peerConnection = null;
        }

        rootEglBase.release();
        videoCapturer.dispose();
        PeerConnectionFactory.shutdownInternalTracer();


    }


    @Override
    protected void onDestroy() {

        hangup();

        super.onDestroy();
    }

    private boolean onToggleMic() {
        isMicMuted = !isMicMuted;

        // 로컬 오디오 트랙의 활성화 여부 설정
        localAudioTrack.setEnabled(!isMicMuted);

        // UI 업데이트: 버튼의 투명도 설정
        micCtl.setAlpha(isMicMuted ? 0.3f : 1.0f);

        return isMicMuted; // 마이크 상태 반환
    }


    /**
     * 권한이 부여된 후 호출되는 메서드. 통화를 시작합니다.
     */
    @AfterPermissionGranted(RC_CALL)
    private void start() {
        // 필요한 권한들
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

        // 권한이 있는지 확인
        if (EasyPermissions.hasPermissions(this, perms)) {
            // 시그널링 서버에 연결
            connectToSignallingServer();

            // 화면 표시를 위한 SurfaceView 초기화
            initializeSurfaceViews();

            // 피어 연결 팩토리 초기화
            initializePeerConnectionFactory();

            // 카메라로부터 비디오 트랙 생성 및 표시
            createVideoTrackFromCameraAndShowIt();

            // 피어 연결 초기화
            initializePeerConnections();

            // 비디오 스트리밍 시작
            startStreamingVideo();
        } else {
            // 권한 부여 요청
            EasyPermissions.requestPermissions(this, "Need some permissions", RC_CALL, perms);
        }
    }


    /**
     * 시그널링 서버에 연결하는 메서드
     */
    private void connectToSignallingServer() {
        try {
            // 시그널링 서버 URL 설정
            String serverURL = "http://192.168.0.4:3030/"; // 시그널링 서버 주소
//            String serverURL = "http://10.0.2.2:3030/"; // 시그널링 서버 주소
            Log.e(TAG, "REPLACE ME: IO Socket:" + serverURL);
            options = new IO.Options();
            options.transports = new String[]{"websocket"}; // 웹소켓 옵션 설정
            socket = IO.socket(serverURL, options);

            // 소켓 이벤트 핸들링
            socket.on(EVENT_CONNECT, args -> {
                Log.d(TAG, "connectToSignallingServer: connect");
                socket.emit("create or join", "cuarto");
            }).on("ipaddr", args -> {
                Log.d(TAG, "connectToSignallingServer: ipaddr");
            }).on("created", args -> {
                Log.d(TAG, "connectToSignallingServer: created");
                isInitiator = true;
            }).on("full", args -> {
                Log.d(TAG, "connectToSignallingServer: full");
            }).on("join", args -> {
                Log.d(TAG, "connectToSignallingServer: join");
                Log.d(TAG, "connectToSignallingServer: Another peer made a request to join room");
                Log.d(TAG, "connectToSignallingServer: This peer is the initiator of room");
                isChannelReady = true;
            }).on("joined", args -> {
                Log.d(TAG, "connectToSignallingServer: joined");
                isChannelReady = true;
            }).on("log", args -> {
                for (Object arg : args) {
                    Log.e(TAG, "sever_log " + String.valueOf(arg));
                }
            }).on("message", args -> {
                Log.d(TAG, "connectToSignallingServer: got a message");
            }).on("message", args -> {
                try {
                    if (args[0] instanceof String) {
                        String message = (String) args[0];
                        if (message.equals("got user media")) {
                            maybeStart();
                        }
                    } else {
                        JSONObject message = (JSONObject) args[0];
                        Log.d(TAG, "connectToSignallingServer: got message " + message);
                        if (message.getString("type").equals("offer")) {
                            Log.d(TAG, "connectToSignallingServer: received an offer " + isInitiator + " " + isStarted);
                            if (!isInitiator && !isStarted) {
                                maybeStart();
                            }
                            peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(OFFER, message.getString("sdp")));
                            doAnswer();
                        } else if (message.getString("type").equals("answer") && isStarted) {
                            peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(ANSWER, message.getString("sdp")));
                        } else if (message.getString("type").equals("candidate") && isStarted) {
                            Log.d(TAG, "connectToSignallingServer: receiving candidates");
                            IceCandidate candidate = new IceCandidate(message.getString("id"), message.getInt("label"), message.getString("candidate"));
                            peerConnection.addIceCandidate(candidate);
                        }
//                    else if (message == "bye" && isStarted) {
//                    handleRemoteHangup();
//                }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }).on(EVENT_DISCONNECT, args -> {
                Log.d(TAG, "connectToSignallingServer: disconnect");
            });

            // 소켓 연결
            socket.connect();
            Log.d("SOCKET", "Connection success : " + socket.id());

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }


    /**
     * 전/후면 카메라 전환
     */
    private void switchCamera() {
        CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) videoCapturer;
        cameraVideoCapturer.switchCamera(null);
    }

    /**
     * 응답 생성 및 전송하는 메서드
     */
    private void doAnswer() {
        peerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                // 로컬 세션 설명 설정
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);

                // 응답 메시지 생성 및 전송
                JSONObject message = new JSONObject();
                try {
                    message.put("type", "answer");
                    message.put("sdp", sessionDescription.description);
                    sendMessage(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new MediaConstraints());
    }


    /**
     * 통화 시작 여부를 결정하고 필요한 경우 통화를 시작하는 메서드
     */
    private void maybeStart() {
        Log.d(TAG, "maybeStart: " + isStarted + " " + isChannelReady);

        // 아직 통화가 시작되지 않았고 채널이 준비된 경우
//        if (!isStarted && isChannelReady) {
        if (!isStarted && isChannelReady) {
            isStarted = true;

            // 주최자인 경우 통화 시작
            if (isInitiator) {
                doCall();
            }
        }
    }


    /**
     * 통화를 시작하고 제안을 생성하고 전송하는 메서드
     */
    private void doCall() {
        // 제안에 대한 미디어 제약 조건 설정
        MediaConstraints sdpMediaConstraints = new MediaConstraints();
        sdpMediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpMediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        // 제안 생성 및 로컬 세션 설명 설정
        peerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(TAG, "onCreateSuccess: ");
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);

                // 제안 메시지 생성 및 전송
                JSONObject message = new JSONObject();
                try {
                    message.put("type", "offer");
                    message.put("sdp", sessionDescription.description);
                    sendMessage(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, sdpMediaConstraints);
    }


    /**
     * 메시지를 소켓을 통해 전송하는 메서드
     */
    private void sendMessage(Object message) {
        socket.emit("message", message);
    }


    /**
     * SurfaceView를 초기화하는 메서드
     */
    private void initializeSurfaceViews() {
        rootEglBase = EglBase.create();

// 로컬 비디오 뷰 초기화
        mLocalVideoView = findViewById(R.id.surface_view);
// 로컬 비디오 뷰에 EGL 컨텍스트를 초기화합니다.
        mLocalVideoView.init(rootEglBase.getEglBaseContext(), null);
// 하드웨어 스케일러 사용을 활성화합니다.
        mLocalVideoView.setEnableHardwareScaler(true);
// 비디오 화면을 미러링합니다.
        mLocalVideoView.setMirror(false);

// 원격 비디오 뷰 초기화
        mRemoteVideoView = findViewById(R.id.surface_view2);
// 원격 비디오 뷰에 EGL 컨텍스트를 초기화합니다.
        mRemoteVideoView.init(rootEglBase.getEglBaseContext(), null);
// 하드웨어 스케일러 사용을 활성화합니다.
        mRemoteVideoView.setEnableHardwareScaler(true);
// 비디오 화면을 미러링합니다.
        mRemoteVideoView.setMirror(true);

        // 더 많은 뷰 추가 가능
    }


    /**
     * PeerConnectionFactory를 초기화하는 메서드
     */
    private void initializePeerConnectionFactory() {
        // Android 플랫폼에 PeerConnectionFactory 초기화
        PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true);

        // 팩토리 생성
        factory = new PeerConnectionFactory(null);

        // 비디오 하드웨어 가속 설정
        factory.setVideoHwAccelerationOptions(rootEglBase.getEglBaseContext(), rootEglBase.getEglBaseContext());
    }


    /**
     * 카메라로부터 비디오 트랙을 생성하고 표시하는 메서드
     */
    private void createVideoTrackFromCameraAndShowIt() {
        // 오디오 제약 조건 초기화
        audioConstraints = new MediaConstraints();

        // 비디오 캡처러 생성
        videoCapturer = createVideoCapturer();

        // 비디오 소스 생성 및 캡처러로부터 비디오 캡처 시작
        videoSource = factory.createVideoSource(videoCapturer);
        videoCapturer.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);

        // 비디오 트랙 생성 및 설정
        videoTrackFromCamera = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        videoTrackFromCamera.setEnabled(true);
        videoTrackFromCamera.addRenderer(new VideoRenderer(mLocalVideoView));

        // 오디오 소스 생성 및 오디오 트랙 생성
        audioSource = factory.createAudioSource(audioConstraints);
        localAudioTrack = factory.createAudioTrack("101", audioSource);
    }


    /**
     * PeerConnection을 초기화하는 메서드
     */
    private void initializePeerConnections() {
        // 팩토리를 사용하여 PeerConnection 생성
        peerConnection = createPeerConnection(factory);
    }

    /**
     * 비디오 스트리밍을 시작하는 메서드
     */
    private void startStreamingVideo() {
        // 로컬 미디어 스트림 생성 및 비디오 트랙 및 오디오 트랙 추가
        mediaStream = factory.createLocalMediaStream("ARDAMS");
        mediaStream.addTrack(videoTrackFromCamera);
        mediaStream.addTrack(localAudioTrack);

        // 피어 연결에 미디어 스트림 추가
        peerConnection.addStream(mediaStream);

        // 메시지 전송
        sendMessage("got user media");
    }


    /**
     * PeerConnection을 생성하는 메서드
     */
    private PeerConnection createPeerConnection(PeerConnectionFactory factory) {
        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
        String URL = "stun:stun.l.google.com:19302";
        iceServers.add(new PeerConnection.IceServer(URL));

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        MediaConstraints pcConstraints = new MediaConstraints();

        PeerConnection.Observer pcObserver = new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(TAG, "onSignalingChange: ");
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(TAG, "onIceConnectionChange: ");
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
                Log.d(TAG, "onIceConnectionReceivingChange: ");
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(TAG, "onIceGatheringChange: ");
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(TAG, "onIceCandidate: ");
                JSONObject message = new JSONObject();

                try {
                    message.put("type", "candidate");
                    message.put("label", iceCandidate.sdpMLineIndex);
                    message.put("id", iceCandidate.sdpMid);
                    message.put("candidate", iceCandidate.sdp);

                    Log.d(TAG, "onIceCandidate: sending candidate " + message);
                    sendMessage(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                Log.d(TAG, "onIceCandidatesRemoved: ");
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d(TAG, "onAddStream: " + mediaStream.videoTracks.size());
                VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                AudioTrack remoteAudioTrack = mediaStream.audioTracks.get(0);
                remoteAudioTrack.setEnabled(true);
                remoteVideoTrack.setEnabled(true);
                remoteVideoTrack.addRenderer(new VideoRenderer(findViewById(R.id.surface_view2)));

            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.d(TAG, "onRemoveStream: ");
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.d(TAG, "onDataChannel: ");
            }

            @Override
            public void onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded: ");
            }
        };

        return factory.createPeerConnection(rtcConfig, pcConstraints, pcObserver);
    }

    /**
     * 비디오 캡처러를 생성하는 메서드
     */
    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer;
        if (useCamera2()) {
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            videoCapturer = createCameraCapturer(new Camera1Enumerator(true));
        }
        return videoCapturer;
    }


    /**
     * 카메라 캡처러를 생성하는 메서드
     */
    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }


    /**
     * Camera2 API를 사용할지 여부를 반환하는 메서드
     */
    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this);
    }
}