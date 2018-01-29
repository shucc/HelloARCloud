package cn.easyar.samples.helloarcloud;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import cn.easyar.Matrix44F;
import cn.easyar.Vec2F;

/**
 * Created by shucc on 18/1/29.
 * cc@cchao.org
 */
public class ImageRenderer {

    private final float[] sPos = new float[]{
            -1.0f, 1.0f,    //左上角
            -1.0f, -1.0f,   //左下角
            1.0f, 1.0f,     //右上角
            1.0f, -1.0f     //右下角
    };

    private final float[] sCoord = new float[]{
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
    };

    private FloatBuffer bPos;
    private FloatBuffer bCoord;

    private Bitmap bitmap;

    private int glPosition;
    private int glCoordinate;
    private int glTexture;
    private int pos_trans_box;
    private int pos_proj_box;

    public ImageRenderer() {
        try {
            bitmap = BitmapFactory.decodeStream(App.getInstance().getResources().getAssets().open("cat.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        bPos = ByteBuffer.allocateDirect(sPos.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(sPos);
        bPos.position(0);
        bCoord = ByteBuffer.allocateDirect(sCoord.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(sCoord);
        bCoord.position(0);
    }

    public void init() {
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, Utils.loadShader(App.getInstance(), R.raw.image_vertex));
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, Utils.loadShader(App.getInstance(), R.raw.image_frag));
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
        pos_trans_box = GLES20.glGetUniformLocation(program, "trans");
        pos_proj_box = GLES20.glGetUniformLocation(program, "proj");
    }

    public void render(Matrix44F projectionMatrix, Matrix44F cameraview, Vec2F size) {
        GLES20.glUniformMatrix4fv(pos_trans_box, 1, false, cameraview.data, 0);
        GLES20.glUniformMatrix4fv(pos_proj_box, 1, false, projectionMatrix.data, 0);
        GLES20.glEnableVertexAttribArray(glPosition);
        GLES20.glEnableVertexAttribArray(glCoordinate);
        GLES20.glUniform1i(glTexture, 0);
        createTexture();
        GLES20.glVertexAttribPointer(glPosition, 2, GLES20.GL_FLOAT, false, 0, bPos);
        GLES20.glVertexAttribPointer(glCoordinate, 2, GLES20.GL_FLOAT, false, 0, bCoord);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    private int createTexture() {
        int[] texture = new int[1];
        if (bitmap != null && !bitmap.isRecycled()) {
            //生成纹理
            GLES20.glGenTextures(1, texture, 0);
            //生成纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
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
            return texture[0];
        }
        return 0;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
