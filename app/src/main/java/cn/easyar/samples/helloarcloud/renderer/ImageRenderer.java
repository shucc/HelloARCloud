package cn.easyar.samples.helloarcloud.renderer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import cn.easyar.Matrix44F;
import cn.easyar.Vec2F;
import cn.easyar.samples.helloarcloud.App;
import cn.easyar.samples.helloarcloud.R;
import cn.easyar.samples.helloarcloud.utils.RawUtils;

/**
 * Created by shucc on 18/1/29.
 * cc@cchao.org
 */
public class ImageRenderer {

    private final String TAG = getClass().getName();

    //纹理坐标
    private final float[] sCoord = new float[]{
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
    };

    private FloatBuffer rightPos;
    private FloatBuffer leftPos;
    private FloatBuffer bCoord;

    private Bitmap leftWordBitmap;
    private Bitmap rightWordBitmap;

    private int glPosition;
    private int glCoordinate;
    private int glTexture;
    private int glTrans;
    private int glProject;

    private String targetUid;

    private Activity activity;

    private int[] texture = new int[2];

    public ImageRenderer(Activity activity) {
        this.activity = activity;
        bCoord = ByteBuffer.allocateDirect(sCoord.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(sCoord);
        bCoord.position(0);
    }

    public void init() {
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, RawUtils.loadRaw(R.raw.image_vertex));
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, RawUtils.loadRaw(R.raw.image_frag));
        //创建一个空的OpenGL ES程序
        int program = GLES20.glCreateProgram();
        //顶点着色器加入到程序
        GLES20.glAttachShader(program, vertexShader);
        //片元着色器加入到程序
        GLES20.glAttachShader(program, fragmentShader);
        //连接到程序
        GLES20.glLinkProgram(program);
        //将程序加入到OpenGL ES 2.0环境
        GLES20.glUseProgram(program);
        glPosition = GLES20.glGetAttribLocation(program, "vPosition");
        glCoordinate = GLES20.glGetAttribLocation(program, "vCoordinate");
        glTexture = GLES20.glGetUniformLocation(program, "vTexture");
        glTrans = GLES20.glGetUniformLocation(program, "trans");
        glProject = GLES20.glGetUniformLocation(program, "proj");
    }

    public void render(Matrix44F projectionMatrix, Matrix44F cameraView, Vec2F size, String leftContent, String rightContent, String uid) {
        //设置防止绘制的图片的背景为透明
        //开启混合
        GLES20.glEnable(GLES20.GL_BLEND);
        //设置混合因子
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        if (TextUtils.isEmpty(targetUid) || !targetUid.equals(uid)) {
            targetUid = uid;
            float size0 = size.data[0];
            float size1 = size.data[1];
            //右侧顶点坐标
            float[] rightOriginPos = new float[]{
                    -size0 / 2 - size0, size1 / 2,  //左上角
                    -size0 / 2 - size0, -size1 / 2, //左下角
                    size0 / 2 - size0, size1 / 2,   //右上角
                    size0 / 2 - size0, -size1 / 2   //右下角
            };
            float[] leftOriginPos = new float[]{
                    -size0 / 2 + size0, size1 / 2,  //左上角
                    -size0 / 2 + size0, -size1 / 2, //左下角
                    size0 / 2 + size0, size1 / 2,   //右上角
                    size0 / 2 + size0, -size1 / 2   //右下角
            };
            rightPos = ByteBuffer.allocateDirect(rightOriginPos.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(rightOriginPos);
            rightPos.position(0);
            leftPos = ByteBuffer.allocateDirect(leftOriginPos.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(leftOriginPos);
            leftPos.position(0);
            leftWordBitmap = loadResultBitmap(true);
            //leftWordBitmap = drawTextToBitmap(App.getInstance(), leftContent, size0, size1);
            rightWordBitmap = loadResultBitmap(false);
            //rightWordBitmap = drawTextToBitmap(App.getInstance(), rightContent, size0, size1);
        }
        GLES20.glUniformMatrix4fv(glTrans, 1, false, cameraView.data, 0);
        GLES20.glUniformMatrix4fv(glProject, 1, false, projectionMatrix.data, 0);
        GLES20.glEnableVertexAttribArray(glPosition);
        GLES20.glEnableVertexAttribArray(glCoordinate);
        GLES20.glUniform1i(glTexture, 0);
        createTexture(leftWordBitmap, 0);
        //绘制识别目标左边图片
        GLES20.glVertexAttribPointer(glPosition, 2, GLES20.GL_FLOAT, false, 0, leftPos);
        GLES20.glVertexAttribPointer(glCoordinate, 2, GLES20.GL_FLOAT, false, 0, bCoord);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        createTexture(rightWordBitmap, 1);
        //绘制识别目标右边图片
        GLES20.glVertexAttribPointer(glPosition, 2, GLES20.GL_FLOAT, false, 0, rightPos);
        GLES20.glVertexAttribPointer(glCoordinate, 2, GLES20.GL_FLOAT, false, 0, bCoord);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    private int createTexture(Bitmap bitmap, int texturesIndex) {
        if (bitmap != null && !bitmap.isRecycled()) {
            //生成纹理
            GLES20.glGenTextures(1, texture, texturesIndex);
            //生成纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[texturesIndex]);
            //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            //根据以上指定的参数，生成一个2D纹理
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            return texture[texturesIndex];
        }
        return texturesIndex;
    }

    private Bitmap drawTextToBitmap(Context context, String content, float size0, float size1) {
        Bitmap leftWordBitmap = Bitmap.createBitmap(512, (int) (512 * size1 / size0), Bitmap.Config.ARGB_4444);
        // get a canvas to paint over the leftWordBitmap
        Canvas canvas = new Canvas(leftWordBitmap);
        leftWordBitmap.eraseColor(Color.TRANSPARENT);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        TextView tv = new TextView(context);
        tv.setTextColor(Color.BLUE);
        tv.setTextSize(8);
        tv.setText(content);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setGravity(Gravity.TOP);
        tv.setPadding(0, 0, 0, 0);
        tv.setDrawingCacheEnabled(true);
        tv.measure(View.MeasureSpec.makeMeasureSpec(canvas.getWidth(), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(canvas.getHeight(), View.MeasureSpec.AT_MOST));
        tv.layout(0, 0, tv.getMeasuredWidth(), tv.getMeasuredHeight());
        LinearLayout parent = null;
        if (!leftWordBitmap.isRecycled()) {
            parent = new LinearLayout(context);
            parent.setDrawingCacheEnabled(true);
            parent.measure(View.MeasureSpec.makeMeasureSpec(canvas.getWidth(),
                    View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(
                    canvas.getHeight(), View.MeasureSpec.EXACTLY));
            parent.layout(0, 0, parent.getMeasuredWidth(),
                    parent.getMeasuredHeight());
            parent.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            parent.setOrientation(LinearLayout.VERTICAL);
            parent.setBackgroundColor(context.getResources().getColor(R.color.transparent));
            parent.addView(tv);
        }
        canvas.drawBitmap(parent.getDrawingCache(), 0, 0, new Paint());
        tv.setDrawingCacheEnabled(false);
        parent.setDrawingCacheEnabled(false);
        Matrix matrix = new Matrix();
        matrix.postScale(1, -1);
        matrix.postRotate(180);
        Bitmap resultBitmap = Bitmap.createBitmap(leftWordBitmap, 0, 0, leftWordBitmap.getWidth(), leftWordBitmap.getHeight(), matrix, true);
        return resultBitmap;
    }

    private Bitmap loadResultBitmap(boolean isLeft) {
        Bitmap resultBitmap = null;
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(activity.getResources().getAssets().open(isLeft ? "one_result_left.png" : "one_result_right.png"));
            Matrix matrix = new Matrix();
            matrix.postScale(1, -1);
            matrix.postRotate(180);
            resultBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultBitmap;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
