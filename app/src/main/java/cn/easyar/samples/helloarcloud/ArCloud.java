//================================================================================================================================
//
//  Copyright (c) 2015-2017 VisionStar Information Technology (Shanghai) Co., Ltd. All Rights Reserved.
//  EasyAR is the registered trademark or trademark of VisionStar Information Technology (Shanghai) Co., Ltd in China
//  and other countries for the augmented reality technology developed by VisionStar Information Technology (Shanghai) Co., Ltd.
//
//================================================================================================================================

package cn.easyar.samples.helloarcloud;

import android.app.Activity;
import android.opengl.GLES20;
import android.util.Base64;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;

import cn.easyar.CameraCalibration;
import cn.easyar.CameraDevice;
import cn.easyar.CameraDeviceFocusMode;
import cn.easyar.CameraDeviceType;
import cn.easyar.CameraFrameStreamer;
import cn.easyar.CloudRecognizer;
import cn.easyar.CloudStatus;
import cn.easyar.Frame;
import cn.easyar.FunctorOfVoidFromCloudStatus;
import cn.easyar.FunctorOfVoidFromCloudStatusAndListOfPointerOfTarget;
import cn.easyar.FunctorOfVoidFromPointerOfTargetAndBool;
import cn.easyar.ImageTarget;
import cn.easyar.ImageTracker;
import cn.easyar.Renderer;
import cn.easyar.Target;
import cn.easyar.TargetInstance;
import cn.easyar.TargetStatus;
import cn.easyar.Vec2I;
import cn.easyar.Vec4I;
import cn.easyar.samples.helloarcloud.renderer.ImageRenderer;

public class ArCloud {

    private final String TAG = getClass().getName();

    private CameraDevice cameraDevice;

    private CameraFrameStreamer cameraFrameStreamer;

    private ArrayList<ImageTracker> trackers;

    private Renderer bgRenderer;

    private ImageRenderer targetRenderer;

    private CloudRecognizer cloudRecognizer;

    private boolean viewPortChanged = false;

    private Vec2I viewSize = new Vec2I(0, 0);

    private int rotation = 0;

    private Vec4I viewport = new Vec4I(0, 0, 1280, 720);

    private Activity activity;

    public ArCloud(Activity activity) {
        this.activity = activity;
        trackers = new ArrayList<>();
    }

    public boolean initialize(String cloudServerAddress, String cloudKey, String cloudSecret) {
        cameraDevice = new CameraDevice();
        cameraFrameStreamer = new CameraFrameStreamer();
        cameraFrameStreamer.attachCamera(cameraDevice);
        cloudRecognizer = new CloudRecognizer();
        cloudRecognizer.attachStreamer(cameraFrameStreamer);
        boolean status = true;
        status &= cameraDevice.open(CameraDeviceType.Default);
        cameraDevice.setSize(new Vec2I(1280, 720));
        cloudRecognizer.open(cloudServerAddress, cloudKey, cloudSecret, new FunctorOfVoidFromCloudStatus() {
            @Override
            public void invoke(int status) {
                if (status == CloudStatus.Success) {
                    Log.i("HelloAR", "CloudRecognizerInitCallBack: Success");
                } else if (status == CloudStatus.Reconnecting) {
                    Log.i("HelloAR", "CloudRecognizerInitCallBack: Reconnecting");
                } else if (status == CloudStatus.Fail) {
                    Log.i("HelloAR", "CloudRecognizerInitCallBack: Fail");
                } else {
                    Log.i("HelloAR", "CloudRecognizerInitCallBack: " + Integer.toString(status));
                }
            }
        }, new FunctorOfVoidFromCloudStatusAndListOfPointerOfTarget() {

            private HashSet<String> uids = new HashSet<>();

            @Override
            public void invoke(int status, ArrayList<Target> targets) {
                if (status == CloudStatus.Success) {
                    Log.i("HelloAR", "CloudRecognizerCallBack: Success");
                } else if (status == CloudStatus.Reconnecting) {
                    Log.i("HelloAR", "CloudRecognizerCallBack: Reconnecting");
                } else if (status == CloudStatus.Fail) {
                    Log.i("HelloAR", "CloudRecognizerCallBack: Fail");
                } else {
                    Log.i("HelloAR", "CloudRecognizerCallBack: " + Integer.toString(status));
                }
                synchronized (uids) {
                    for (Target t : targets) {
                        if (!uids.contains(t.uid())) {
                            Log.i("HelloAR", "add cloud target: " + t.uid());
                            uids.add(t.uid());
                            trackers.get(0).loadTarget(t, new FunctorOfVoidFromPointerOfTargetAndBool() {
                                @Override
                                public void invoke(Target target, boolean status) {
                                    Log.i("HelloAR", String.format("load target (%b): %s (%d)", status, target.name(), target.runtimeID()));
                                }
                            });
                        }
                    }
                }
            }
        });
        if (!status) {
            return status;
        }
        ImageTracker tracker = new ImageTracker();
        tracker.attachStreamer(cameraFrameStreamer);
        trackers.add(tracker);
        return status;
    }

    public void dispose() {
        for (ImageTracker tracker : trackers) {
            tracker.dispose();
        }
        trackers.clear();
        if (cloudRecognizer != null) {
            cloudRecognizer.dispose();
            cloudRecognizer = null;
        }
        targetRenderer = null;
        if (bgRenderer != null) {
            bgRenderer.dispose();
            bgRenderer = null;
        }
        if (cameraFrameStreamer != null) {
            cameraFrameStreamer.dispose();
            cameraFrameStreamer = null;
        }
        if (cameraDevice != null) {
            cameraDevice.dispose();
            cameraDevice = null;
        }
    }

    public boolean start() {
        boolean status = true;
        status &= (cameraDevice != null) && cameraDevice.start();
        status &= (cameraFrameStreamer != null) && cameraFrameStreamer.start();
        status &= (cloudRecognizer != null) && cloudRecognizer.start();
        cameraDevice.setFocusMode(CameraDeviceFocusMode.Continousauto);
        for (ImageTracker tracker : trackers) {
            status &= tracker.start();
        }
        return status;
    }

    public boolean stop() {
        boolean status = true;
        for (ImageTracker tracker : trackers) {
            status &= tracker.stop();
        }
        status &= (cloudRecognizer != null) && cloudRecognizer.stop();
        status &= (cameraFrameStreamer != null) && cameraFrameStreamer.stop();
        status &= (cameraDevice != null) && cameraDevice.stop();
        return status;
    }

    public void initGL() {
        if (bgRenderer != null) {
            bgRenderer.dispose();
        }
        bgRenderer = new Renderer();
        targetRenderer = new ImageRenderer(activity);
        targetRenderer.init();
    }

    public void resizeGL(int width, int height) {
        viewSize = new Vec2I(width, height);
        viewPortChanged = true;
    }

    private void updateViewport() {
        CameraCalibration cameraCalibration = cameraDevice != null ? cameraDevice.cameraCalibration() : null;
        int rotation = cameraCalibration != null ? cameraCalibration.rotation() : 0;
        if (rotation != this.rotation) {
            this.rotation = rotation;
            viewPortChanged = true;
        }
        if (viewPortChanged) {
            Vec2I size = new Vec2I(1, 1);
            if ((cameraDevice != null) && cameraDevice.isOpened()) {
                size = cameraDevice.size();
            }
            if (rotation == 90 || rotation == 270) {
                size = new Vec2I(size.data[1], size.data[0]);
            }
            float scaleRatio = Math.max((float) viewSize.data[0] / (float) size.data[0], (float) viewSize.data[1] / (float) size.data[1]);
            Vec2I viewport_size = new Vec2I(Math.round(size.data[0] * scaleRatio), Math.round(size.data[1] * scaleRatio));
            viewport = new Vec4I((viewSize.data[0] - viewport_size.data[0]) / 2, (viewSize.data[1] - viewport_size.data[1]) / 2, viewport_size.data[0], viewport_size.data[1]);
            if ((cameraDevice != null) && cameraDevice.isOpened()) {
                viewPortChanged = false;
            }
        }
    }

    public void render() {
        GLES20.glClearColor(1.f, 1.f, 1.f, 1.f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (bgRenderer != null) {
            Vec4I default_viewport = new Vec4I(0, 0, viewSize.data[0], viewSize.data[1]);
            GLES20.glViewport(default_viewport.data[0], default_viewport.data[1], default_viewport.data[2], default_viewport.data[3]);
            if (bgRenderer.renderErrorMessage(default_viewport)) {
                return;
            }
        }
        if (cameraFrameStreamer == null) {
            return;
        }
        Frame frame = cameraFrameStreamer.peek();
        try {
            updateViewport();
            GLES20.glViewport(viewport.data[0], viewport.data[1], viewport.data[2], viewport.data[3]);
            if (bgRenderer != null) {
                bgRenderer.render(frame, viewport);
            }
            for (TargetInstance targetInstance : frame.targetInstances()) {
                int status = targetInstance.status();
                if (status == TargetStatus.Tracked) {
                    Target target = targetInstance.target();
                    String metaStr = new String(Base64.decode(target.meta().getBytes(), Base64.DEFAULT));
                    Log.d(TAG, "render: " + metaStr);
                    ImageTarget imagetarget = target instanceof ImageTarget ? (ImageTarget) (target) : null;
                    if (imagetarget == null) {
                        continue;
                    }
                    if (targetRenderer != null) {
                        targetRenderer.render(cameraDevice.projectionGL(0.2f, 500.f), targetInstance.poseGL(), imagetarget.size(), metaStr, metaStr, target.uid());
                    }
                }
            }
        } finally {
            frame.dispose();
        }
    }
}
