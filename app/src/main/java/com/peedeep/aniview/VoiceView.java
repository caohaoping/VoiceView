package com.peedeep.aniview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hp on 2019/7/2.
 * TODO
 */
public class VoiceView extends View {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int POINT_COUNTS = 3;
    private boolean isOpen;
    private static final int STATE_IDLE = 0;
    private static final int STATE_WAKEUP = 1;
    private static final int STATE_LISTENING = 2;
    private static final int STATE_THINKING = 3;
    private int state = STATE_IDLE;
    private int width = dip2px(getContext(), 150);
    private int height = dip2px(getContext(), 300);
    private int color;
    private float radius;
    private Paint containerPaint;
    private Path containerRectPath;
    private Rect borderRect;
    private Paint borderPaint;
    private float containerProgress;
    private List<Point> points;
    private Paint pointPaint;
    private float centerX;
    private float pointProgress;
    private float percentOne;
    private float bigPointProgress;
    private float thinkingProgress;
    private float pointRadius;
    private float eyeRadius;
    private Path eatPath;
    private RectF eyesRectF;

    public VoiceView(Context context) {
        this(context, null);
    }

    public VoiceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VoiceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.VoiceView);
        color = array.getColor(R.styleable.VoiceView_color, Color.MAGENTA);
        array.recycle();

        containerPaint = new Paint();
        containerPaint.setAntiAlias(true);
        containerPaint.setStyle(Paint.Style.FILL);
        containerPaint.setColor(color);

        containerRectPath = new Path();

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.BLUE);
        borderRect = new Rect();

        points = new ArrayList<>(POINT_COUNTS);
        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setColor(Color.WHITE);

        eatPath = new Path();
        eyesRectF = new RectF();

        doThinking(1, 1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        drawBorder(canvas);
        drawContainer(canvas);
        drawPoints(canvas);
        drawBigPoint(canvas);
        drawEyes(canvas);
    }

    private void drawEyes(Canvas canvas) {
        if (!isOpen && (state == STATE_THINKING || state == STATE_IDLE)) {
            float eyeW = height / 8f / thinkingProgress;
            float eyeH = height / 4f * thinkingProgress;
            float eyeX = (height - eyeW * 2) * 2 / 5;
            float eyeY = (height - eyeH) / 2;
            Log.i(TAG, "drawEyes: " + eyeW + ", " + eyeH + ", " + eyeX + ", " + eyeY);
            eyesRectF.set(eyeX, eyeY, eyeX + eyeW, eyeY + eyeH);
            canvas.drawRoundRect(eyesRectF, eyeRadius, eyeRadius, pointPaint);
            eyesRectF.set((eyeX * 3 / 2) + eyeW, eyeY, (eyeX * 3 / 2) + eyeW * 2, eyeY + eyeH);
            canvas.drawRoundRect(eyesRectF, eyeRadius, eyeRadius, pointPaint);
        }
    }

    private void drawBorder(Canvas canvas) {
        canvas.drawRect(borderRect, borderPaint);
    }

    private void drawContainer(Canvas canvas) {
        canvas.drawCircle(radius, radius, radius, containerPaint);
        float circleCenterX = containerProgress * (width - height) + radius;
        if (isOpen && circleCenterX > width - radius) {
            circleCenterX = width - radius;
        } else if (!isOpen && circleCenterX < radius) {
            circleCenterX = radius;
        }
        if (circleCenterX != radius) {
            containerRectPath.reset();
            containerRectPath.moveTo(radius, 0);
            containerRectPath.lineTo(circleCenterX, 0);
            containerRectPath.lineTo(circleCenterX, height);
            containerRectPath.lineTo(radius, height);
            containerRectPath.close();
            canvas.drawPath(containerRectPath, containerPaint);
            canvas.drawCircle(circleCenterX, radius, radius, containerPaint);
        }
    }

    private void drawPoints(Canvas canvas) {
        if (isOpen && containerProgress == 1 && state == STATE_WAKEUP) {
            canvas.save();
            canvas.translate(pointProgress * width, 0);
            for (int i = 0; i < POINT_COUNTS; i++) {
                Point point = points.get(i);
                canvas.drawCircle(point.x, point.y, point.radius, pointPaint);
            }
            canvas.restore();
        }
    }

    private void drawBigPoint(Canvas canvas) {
        if (isOpen && state == STATE_LISTENING) {
            Log.d(TAG, "drawBigPoint: " + bigPointProgress);
            float eatRadius = (float) (pointRadius * (Math.abs(bigPointProgress - 0.5) + 1.5));
            for (int i = 0; i < POINT_COUNTS; i++) {
                Point point = points.get(i);
                float cx = point.x - bigPointProgress * percentOne + width + percentOne;
                if (i == 0) {
                    canvas.drawCircle(percentOne, radius, eatRadius, pointPaint);
                    if (percentOne + eatRadius >= cx - point.radius) {
                        eatPath.reset();
                        eatPath.moveTo(percentOne, radius - eatRadius);
                        eatPath.lineTo(cx, radius - point.radius);
                        eatPath.lineTo(cx, radius + point.radius);
                        eatPath.lineTo(percentOne, radius + eatRadius);
                        eatPath.close();
                        canvas.drawPath(eatPath, pointPaint);
                    }
                }
                if (cx >= percentOne) {
                    canvas.drawCircle(cx, point.y, point.radius, pointPaint);
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        }
        Log.d(TAG, "onMeasure: " + width + ", " + height);
        radius = height >> 1;
        centerX = width >> 1;
        percentOne = width / (POINT_COUNTS + 1);
        pointRadius = radius / 5;
        eyeRadius = 5;
        for (int i = 1; i <= POINT_COUNTS; i++) {
            points.add(new Point(i, i * percentOne - width, radius, pointRadius));
        }
        borderRect.set(0, 0, width, height);
        updateLinearGradient(Color.BLUE, Color.RED);
        setMeasuredDimension(width, height);
    }

    private int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    private void updateLinearGradient(int myStartColor, int myEndColor) {
        int startColor = ColorUtils.setAlphaComponent(myStartColor, (int) (1f * 255));
        int endColor = ColorUtils.setAlphaComponent(myEndColor, (int) (1f * 255));
        int w = width;
        int h = height;
        int r = (int) (Math.sqrt(w * w + h * h) / 2);
        int y = (int) (r * Math.sin(2 * Math.PI * 45 / 360));
        int x = (int) (r * Math.cos(2 * Math.PI * 45 / 360));
        LinearGradient shader = new LinearGradient((w >> 1) - x, (h >> 1) - y,
                (w >> 1) + x, (h >> 1) + y, startColor, endColor, Shader.TileMode.CLAMP);
        containerPaint.setShader(shader);
    }

    public void switchFloat() {
        Log.d(TAG, "switchFloat: " + state);
        isOpen = !isOpen;
        if (isOpen) {
            wakeup();
        } else {
            startThinking();
        }
    }

    private void stop() {
        state = STATE_IDLE;
        Log.i(TAG, "stop: ");
        ValueAnimator animator = ValueAnimator.ofFloat(1, 0);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                containerProgress = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }

    private void wakeup() {
        state = STATE_WAKEUP;
        Log.i(TAG, "wakeup: ");
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                containerProgress = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                translatePoint();
            }

            @Override
            public void onAnimationStart(Animator animation) {
            }
        });
        animator.start();
    }

    private void translatePoint() {
        final ValueAnimator pointAnimator = ValueAnimator.ofFloat(0, 1);
        pointAnimator.setDuration(200);
        pointAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pointAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                pointProgress = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        pointAnimator.start();
    }

    public void startListening() {
        if (!isOpen) return;
        state = STATE_LISTENING;
        Log.d(TAG, "startListening: ");
        final ValueAnimator bigPointAnimator = ValueAnimator.ofFloat(0, 1);
        bigPointAnimator.setDuration(700);
        bigPointAnimator.setInterpolator(new LinearInterpolator());
        bigPointAnimator.setRepeatMode(ValueAnimator.RESTART);
        bigPointAnimator.setRepeatCount(ValueAnimator.INFINITE);
        bigPointAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                bigPointProgress = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        bigPointAnimator.start();
    }

    public void startThinking() {
        state = STATE_THINKING;
        ValueAnimator animator = ValueAnimator.ofFloat(1, 0);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                containerProgress = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                doThinking(1, 1);
            }

            @Override
            public void onAnimationStart(Animator animation) {

            }
        });
        animator.start();
    }

    private void doThinking(float start, float end) {
        final ValueAnimator animator = ValueAnimator.ofFloat(start, 0.5f, end);
        animator.setDuration(1000);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setRepeatMode(ValueAnimator.RESTART);
//        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                thinkingProgress = (float) animation.getAnimatedValue();
//                generateColor();
                invalidate();
            }
        });
        animator.start();
    }

    private void generateColor() {
        String startHex = Integer.toHexString((int) -thinkingProgress);
        String endHex = Integer.toHexString((int) (-thinkingProgress + 10));
        Log.d(TAG, "onAnimationUpdate: " + startHex + ", " + endHex);
        int startColor = Color.parseColor("#" + startHex);
        int endColor = Color.parseColor("#" + endHex);
        updateLinearGradient(startColor, endColor);
    }

}
