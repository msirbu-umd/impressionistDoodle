package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.text.MessageFormat;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 5;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        //TODO
        _offScreenBitmap.eraseColor(Color.WHITE);
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){

        float curTouchX = motionEvent.getX();
        float curTouchY = motionEvent.getY();
        //long startTime = SystemClock.elapsedRealtime();
        long startTime = System.currentTimeMillis();

        Bitmap imageViewBitmap = _imageView.getDrawingCache();
        int colorAtTouchPixelInImage = imageViewBitmap.getPixel((int) curTouchX,
                (int) curTouchY);
        float brushRadius = 25;
        float circleRadius = 25;
        float circleSplatterRadius = 10;
        double noScaleSpeed = 0.5;
        _paint.setColor(colorAtTouchPixelInImage);

        //Basically, the way this works is to liste for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location
        switch(motionEvent.getAction()){

            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                int historySize = motionEvent.getHistorySize();
                for (int i = 0; i < historySize; i++) {

                    float touchX = motionEvent.getHistoricalX(i);
                    float touchY = motionEvent.getHistoricalY(i);

                    if(_brushType == BrushType.Square) {
                        _offScreenCanvas.drawRect(touchX - brushRadius / 2, touchY - brushRadius / 2, touchX + brushRadius / 2,
                                touchY + brushRadius / 2, _paint);
                    }else if(_brushType == BrushType.Circle){

                        if(_lastPoint == null){
                            _offScreenCanvas.drawCircle(touchX, touchY, circleRadius, _paint);
                        }else {
                            double distance = getDistance(touchX, touchY, _lastPoint);
                            long timeDiff = startTime - _lastPointTime;
                            double speed = distance / timeDiff;
                            Log.d("Speed2", "The speed is: " + speed);
                            double scaleFactor = speed / noScaleSpeed;
                            Log.d("Speed2", "The Scale Factor is: " + scaleFactor);

                            float newBrushRadius = circleRadius * (float) scaleFactor;
                            Log.d("Speed2", "The new brush radius is: " + newBrushRadius);


                            if (newBrushRadius < 10) {
                                _offScreenCanvas.drawCircle(touchX, touchY, 10, _paint);
                            } else if (newBrushRadius > 80) {
                                _offScreenCanvas.drawCircle(touchX, touchY, 80, _paint);
                            } else {
                                _offScreenCanvas.drawCircle(touchX, touchY, newBrushRadius,
                                        _paint);
                            }
                        }

                        //_offScreenCanvas.drawCircle(touchX, touchY, circleRadius, _paint);
                    }else if(_brushType == BrushType.CircleSplatter) {
                        Random rand = new Random();
                        int numCircles = rand.nextInt(6) + 2;
                        _offScreenCanvas.drawCircle(touchX, touchY, circleSplatterRadius, _paint);
                        for(int j = 0; j < numCircles; j++){
                            int distanceX = rand.nextInt(12)-6;
                            int distanceY = rand.nextInt(12)-6;

                            float newX = touchX + distanceX;
                            float newY = touchY + distanceY;

                            colorAtTouchPixelInImage = imageViewBitmap.getPixel((int) newX,
                                    (int) newY);
                            _paint.setColor(colorAtTouchPixelInImage);
                            _offScreenCanvas.drawCircle(newX, newY, circleSplatterRadius, _paint);
                        }
                    }
                }

                if(_brushType == BrushType.Square) {
                    _offScreenCanvas.drawRect(curTouchX - brushRadius / 2, curTouchY - brushRadius / 2,
                            curTouchX + brushRadius / 2, curTouchY + brushRadius / 2, _paint);
                }else if(_brushType == BrushType.Circle){

                    if(_lastPoint == null){
                        _offScreenCanvas.drawCircle(curTouchX, curTouchY, circleRadius, _paint);
                    }else{
                        double distance = getDistance(curTouchX, curTouchY, _lastPoint);
                        long timeDiff = startTime - _lastPointTime;
                        double speed = distance / timeDiff;
                        Log.d("Speed", "The speed is: "+ speed);
                        double scaleFactor = speed/noScaleSpeed;
                        Log.d("Speed", "The Scale Factor is: "+ scaleFactor);

                        float newBrushRadius = circleRadius * (float) scaleFactor;
                        Log.d("Speed", "The new brush radius is: "+ newBrushRadius);

                        if(newBrushRadius < 10){
                            _offScreenCanvas.drawCircle(curTouchX, curTouchY, 10, _paint);
                        }else if(newBrushRadius > 80){
                            _offScreenCanvas.drawCircle(curTouchX, curTouchY, 80, _paint);
                        }else {
                            _offScreenCanvas.drawCircle(curTouchX, curTouchY, newBrushRadius,
                                    _paint);
                        }

                    }
                }else if(_brushType == BrushType.CircleSplatter){
                    Random rand = new Random();
                    int numCircles = rand.nextInt(6) + 2;
                    _offScreenCanvas.drawCircle(curTouchX, curTouchY, circleSplatterRadius, _paint);
                    for(int j = 0; j < numCircles; j++){
                        int distanceX = rand.nextInt(12)-6;
                        int distanceY = rand.nextInt(12)-6;

                        float newX = curTouchX + distanceX;
                        float newY = curTouchY + distanceY;

                        colorAtTouchPixelInImage = imageViewBitmap.getPixel((int) newX,
                                (int) newY);
                        _paint.setColor(colorAtTouchPixelInImage);
                        _offScreenCanvas.drawCircle(newX, newY, circleSplatterRadius, _paint);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                _lastPointTime = -1;
                _lastPoint = null;
                break;
        }

        _lastPointTime = startTime;
        _lastPoint = new Point((int) curTouchX, (int) curTouchY);

        invalidate();
        return true;
    }

    private double getDistance(float curX, float curY ,Point lastPoint){
        return Math.sqrt(Math.pow((double) curX - lastPoint.x, 2) +
                Math.pow((double) curY - lastPoint.y, 2));
    }

    private class PaintPoint {
        private Paint _paint = new Paint();
        private PointF _point;
        private float _brushRadius;

        public PaintPoint(float x, float y, float brushRadius, Paint paintSrc){
            // Copy the fields from paintSrc into this paint
            _paint.set(paintSrc);
            _point = new PointF(x, y);
            _brushRadius = brushRadius;
        }

        public Paint getPaint(){
            return _paint;
        }

        public float getX(){
            return _point.x;
        }

        public float getY(){
            return _point.y;
        }

        public float getBrushRadius(){
            return _brushRadius;
        }
    }

    public Bitmap getOffScreenBitmap(){
        return _offScreenBitmap;
    }



    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

