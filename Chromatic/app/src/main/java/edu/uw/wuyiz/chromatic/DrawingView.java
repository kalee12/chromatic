package edu.uw.wuyiz.chromatic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wuyiz on 11/28/17.
 */

public class DrawingView extends View {

    /**
     * Action done on the image: NONE, DRAG, and ZOOM
     */
    public enum ACTION {
        NONE, DRAG, ZOOM
    }

    /**
     * Direction of the aciton: UP, DOWN, LEFT, and RIGHT
     */
    public enum DIRECTION {
        UP, DOWN, LEFT, RIGHT
    }

    private List<CustomBitmap> bitmapList;
    private Context curContext;
    private CustomBitmap curCustomBitmap;
    private Matrix curMatrix;
    private ACTION action;

    private float oldDist;
    private float newDist;
    private float curScaleParam;

    public DrawingView(Context context) {
        super(context);
        this.curContext = context;
        this.bitmapList = new ArrayList<>();
        this.curMatrix = new Matrix();
        // set default action to none;
        this.action = ACTION.NONE;
    }

    public DrawingView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.curContext = context;
        this.bitmapList = new ArrayList<>();
        this.curMatrix = new Matrix();
        // set default action to none;
        this.action = ACTION.NONE;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                action = ACTION.DRAG;

                if (curCustomBitmap == null && bitmapList.size() > 0) {
                    curCustomBitmap = bitmapList.get(bitmapList.size() - 1);
                }

                for (CustomBitmap customBitmap : bitmapList) {
                    float[] values = new float[9];
                    customBitmap.getMatrix().getValues(values);
                    float globalX = values[Matrix.MTRANS_X];
                    float globalY = values[Matrix.MTRANS_Y];
                    float width = customBitmap.getWidthAfterScale();
                    float height = customBitmap.getHeightAfterScale();
                    float midX = (globalX + width) / 2;
                    float midY = (globalY + height) / 2;
                    PointF pointF = new PointF(midX, midY);
                    customBitmap.midPoint = pointF;
                }

                for (CustomBitmap customBitmap : bitmapList) {
                    float[] values = new float[9];
                    customBitmap.getMatrix().getValues(values);
                    float globalX = values[Matrix.MTRANS_X];
                    float globalY = values[Matrix.MTRANS_Y];
                    float width = customBitmap.getWidthAfterScale();
                    float height = customBitmap.getHeightAfterScale();

                    Rect rect = new Rect((int) globalX, (int) globalY,
                            (int) (globalX + width), (int) (globalY + height));

                    if (motionEvent.getX() > rect.left && motionEvent.getX() < rect.right &&
                            motionEvent.getY() > rect.top && motionEvent.getY() < rect.bottom) {
                        curCustomBitmap = customBitmap;
                        change(true, motionEvent);
                        break;
                    } else if (motionEvent.getX() < rect.left &&
                            motionEvent.getX() > (rect.left - width) &&
                            motionEvent.getY() < rect.top &&
                            motionEvent.getY() > (rect.top - height)) {
                        curCustomBitmap = customBitmap;
                        change(true, motionEvent);
                        break;
                    } else if (motionEvent.getX() > (rect.left - height) &&
                            motionEvent.getX() < rect.left &&
                            motionEvent.getY() > rect.top &&
                            motionEvent.getY() < (rect.top + width)) {
                        curCustomBitmap = customBitmap;
                        change(true, motionEvent);
                        break;
                    } else if (motionEvent.getX() > rect.left &&
                            motionEvent.getX() < (rect.left + height) &&
                            motionEvent.getY() > (rect.top - width) &&
                            motionEvent.getY() < rect.top) {
                        curCustomBitmap = customBitmap;
                        change(true, motionEvent);
                        break;
                    }
                }

                change(false, motionEvent);
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                action = ACTION.ZOOM;
                oldDist = calculateSpacing(motionEvent);
                curCustomBitmap.oldRotation = calculateRotation(motionEvent);
                curCustomBitmap.startDist = calculateDistance(motionEvent);

                if (curCustomBitmap.getStartDist() > 10f) {
                    curCustomBitmap.midPoint = calculateMidPoint(motionEvent);
                    curMatrix.set(curCustomBitmap.getMatrix());
                }

                break;

            case MotionEvent.ACTION_MOVE:
                if (action == ACTION.DRAG) {
                    float dX = motionEvent.getX() - curCustomBitmap.getStartPoint().x;
                    float dY = motionEvent.getY() - curCustomBitmap.getStartPoint().y;
                    curCustomBitmap.matrix.set(curMatrix);
                    curCustomBitmap.matrix.postTranslate(dX, dY);
                } else if (action == ACTION.ZOOM) {
                    float endDist = calculateDistance(motionEvent);
                    curCustomBitmap.newRotation =
                            calculateRotation(motionEvent) - curCustomBitmap.getOldRotation();
                    if (endDist > 10f) {
                        curScaleParam = endDist / curCustomBitmap.getStartDist();
                        curCustomBitmap.matrix.set(curMatrix);
                        curCustomBitmap.matrix.postScale(curScaleParam, curScaleParam,
                                curCustomBitmap.getMidPoint().x,
                                curCustomBitmap.getMidPoint().y);
                        curCustomBitmap.matrix.postRotate(curCustomBitmap.getNewRotation(),
                                curCustomBitmap.getMidPoint().x, curCustomBitmap.getMidPoint().y);
                    }
                    newDist = calculateSpacing(motionEvent);
                    if (newDist > oldDist + 1 || newDist < oldDist - 1) {
                        scaleAll(newDist / oldDist);
                        oldDist = newDist;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
            case MotionEvent.ACTION_POINTER_UP:
                action = ACTION.NONE;
                setWeightAndHeight(curScaleParam);
                break;
        }
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        for (CustomBitmap customBitmap : bitmapList) {
            canvas.drawBitmap(customBitmap.getBitmap(), customBitmap.getMatrix(), paint);
        }
    }

    public void setOffset(DIRECTION direction) {
        if (direction == DIRECTION.UP) {
            curCustomBitmap.matrix.postTranslate(0, -1);
        } else if (direction == DIRECTION.DOWN) {
            curCustomBitmap.matrix.postTranslate(0, 1);
        } else if (direction == DIRECTION.LEFT) {
            curCustomBitmap.matrix.postTranslate(-1, 0);
        } else if (direction == DIRECTION.RIGHT) {
            curCustomBitmap.matrix.postTranslate(1, 0);
        }

        invalidate();
    }

    public void addBitmap(CustomBitmap customBitmap) {
        bitmapList.add(customBitmap);
    }

    private float calculateRotation(MotionEvent motionEvent) {
        double deltaX = (motionEvent.getX(0) - motionEvent.getX(1));
        double deltaY = (motionEvent.getY(0) - motionEvent.getY(1));
        double radians = Math.atan2(deltaY, deltaX);
        return (float) Math.toDegrees(radians);
    }

    private float calculateDistance(MotionEvent motionEvent) {
        float dX = motionEvent.getX(1) - motionEvent.getX(0);
        float dY = motionEvent.getY(1) - motionEvent.getY(0);
        return (float) Math.sqrt(dX * dX + dY * dY);
    }

    private float calculateSpacing(MotionEvent motionEvent) {
        float x = motionEvent.getX(0) - motionEvent.getX(1);
        float y = motionEvent.getY(0) - motionEvent.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private PointF calculateMidPoint(MotionEvent motionEvent) {
        float midX = (motionEvent.getX(1) + motionEvent.getX(0)) / 2;
        float midY = (motionEvent.getY(1) + motionEvent.getY(0)) / 2;
        return new PointF(midX, midY);
    }

    private void scaleAll(float scaleParam) {
        for (CustomBitmap customBitmap : bitmapList) {
            customBitmap.matrix.postScale(scaleParam, scaleParam,
                    customBitmap.getMidPoint().x, customBitmap.getMidPoint().y);
        }
    }

    private void setWeightAndHeight(float scaleParam) {
        for (CustomBitmap customBitmap : bitmapList) {
            customBitmap.widthAfterScale = ((int) (customBitmap.getWidthAfterScale() * scaleParam));
            customBitmap.heightAfterScale = ((int) (customBitmap.getHeightAfterScale() * scaleParam));

            float[] values = new float[9];
            customBitmap.getMatrix().getValues(values);
            float globalX = values[Matrix.MTRANS_X];
            float globalY = values[Matrix.MTRANS_Y];
            float width= customBitmap.getWidthAfterScale();
            float height = customBitmap.getHeightAfterScale();

            Rect rect = new Rect((int) globalX,
                    (int) globalY,
                    (int) (globalX + width),
                    (int) (globalY + height));

            customBitmap.x = rect.left;
            customBitmap.y = rect.top;
            customBitmap.scaleParam = scaleParam;
        }
    }

    private void change(boolean isChanged, MotionEvent motionEvent) {
        if (isChanged) {
            bitmapList.remove(curCustomBitmap);
            bitmapList.add(curCustomBitmap);
        }

        curMatrix.set(curCustomBitmap.getMatrix());
        curCustomBitmap.matrix.set(curMatrix);
        curCustomBitmap.startPoint.set(motionEvent.getX(), motionEvent.getY());
        postInvalidate();
    }

    private List<CustomBitmap> getViews() {
        return bitmapList;
    }
}