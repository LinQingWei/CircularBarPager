package daniel.olivier.stoyan.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

import stoyan.olivier.library.R;

/**
 * Created by stoyan and olivier on 12/9/14.
 */
public class CircularBarPager extends View {

    private Context mContext;

    /**
     * The max progress, default is 100
     */
    private int mMax = 100;

    /**
     * current progress, can not exceed the max progress.
     */
    private int mProgress = 0;

    /**
     * the progress area bar color
     */
    private int mReachedArcColor;

    /**
     * the bar unreached area color.
     */
    private int mUnreachedArcColor;

    /**
     * the width of the reached area
     */
    private float mReachedArcWidth;

    /**
     * the width of the unreached area
     */
    private float mUnreachedArcWidth;

    /**
     * the progress angles of the {@link #mUnReachedArcRectF} and
     * {@link #mReachedArcRectF}
     */
    private ProgressSweep mProgressSweep;

    /**
     * the suffix of the number.
     */
    private String mSuffix = "%";

    /**
     * the prefix.
     */
    private String mPrefix = "";


    private final int default_reached_color = Color.parseColor("#aed036");
    private final int default_unreached_color = Color.parseColor("#aed036");
    private final float default_reached_arc_width;
    private final float default_unreached_arc_width;

    /**
     * for save and restore instance of progressbar.
     */
    private static final String INSTANCE_STATE = "saved_instance";
    private static final String INSTANCE_REACHED_BAR_HEIGHT = "reached_bar_height";
    private static final String INSTANCE_REACHED_BAR_COLOR = "reached_bar_color";
    private static final String INSTANCE_UNREACHED_BAR_HEIGHT = "unreached_bar_height";
    private static final String INSTANCE_UNREACHED_BAR_COLOR = "unreached_bar_color";
    private static final String INSTANCE_MAX = "max";
    private static final String INSTANCE_PROGRESS = "progress";
    private static final String INSTANCE_SUFFIX = "suffix";
    private static final String INSTANCE_PREFIX = "prefix";

    /**
     * the Paint of the reached area.
     */
    private Paint mReachedBarPaint;
    /**
     * the Painter of the unreached area.
     */
    private Paint mUnreachedBarPaint;

    /**
     * reached bar area rect.
     */
    private RectF mReachedArcRectF = new RectF(0,0,0,0);

    private RectF mUnReachedArcRectF = new RectF(0,0,0,0);

    /**
     * determine if need to draw unreached area
     */
    private boolean mDrawUnreachedBar = true;

    /**
     * we should always dray reached area
     */
    private boolean mDrawReachedBar = true;

    public CircularBarPager(Context context) {
        this(context, null);
    }

    public CircularBarPager(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.CircularBarPagerStyle);
    }

    public CircularBarPager(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;

        default_reached_arc_width = dp2px(1.5f);
        default_unreached_arc_width = dp2px(1.0f);

        //load styled attributes.
        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CircularBarPager,
                defStyleAttr, 0);

        mReachedArcColor = attributes.getColor(R.styleable.CircularBarPager_progress_reached_color, default_reached_color);
        mUnreachedArcColor = attributes.getColor(R.styleable.CircularBarPager_progress_unreached_color, default_unreached_color);

        mReachedArcWidth = attributes.getDimension(R.styleable.CircularBarPager_progress_reached_arc_width, default_reached_arc_width);
        mUnreachedArcWidth = attributes.getDimension(R.styleable.CircularBarPager_progress_unreached_arc_width, default_unreached_arc_width);


        setMax(attributes.getInt(R.styleable.CircularBarPager_max, 100));
        setProgress(attributes.getInt(R.styleable.CircularBarPager_progress,0));
        //
        attributes.recycle();

        initializePainters();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measure(widthMeasureSpec,true), measure(heightMeasureSpec,false));
    }

    private int measure(int measureSpec,boolean isWidth){
        int result;
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);
        int padding = isWidth?getPaddingLeft()+getPaddingRight():getPaddingTop()+getPaddingBottom();
        if(mode == MeasureSpec.EXACTLY){
            result = size;
        }else{
            result = isWidth ? getSuggestedMinimumWidth() : getSuggestedMinimumHeight();
            result += padding;
            if(mode == MeasureSpec.AT_MOST){
                if(isWidth) {
                    result = Math.max(result, size);
                }
                else{
                    result = Math.min(result, size);
                }
            }
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        calculateDrawRectF();

        if(mDrawReachedBar){
            canvas.drawArc(mReachedArcRectF, mProgressSweep.reachedStart, mProgressSweep.reachedSweep, false, mReachedBarPaint);
            canvas.drawArc(mUnReachedArcRectF, mProgressSweep.unReachedStart, mProgressSweep.unRreachedSweep, false, mUnreachedBarPaint);
//            canvas.drawLine(mReachedArcRectF.centerX(), mReachedArcRectF.top - mReachedArcWidth, mReachedArcRectF.centerX() + 1, mReachedArcRectF.top, mReachedBarPaint);
        }
    }

    /**
     * Calculates the coordinates of {@link #mUnReachedArcRectF} and
     * {@link #mReachedArcRectF}
     */
    private void calculateDrawRectF(){
        mReachedArcRectF = getArcRect(mReachedArcWidth/2);
        mUnReachedArcRectF = getArcRect(mUnreachedArcWidth/2);
    }

    /**
     * Calculates the coordinates of {@link android.graphics.RectF} that
     * are perfectly within the available window
     * @param offset Half the width of the pain stroke
     * @return The rectF
     */
    private RectF getArcRect(float offset){
        RectF workingSurface = new RectF();
        workingSurface.left = getPaddingLeft() + offset;
        workingSurface.top = getPaddingTop() + offset;
        workingSurface.right = getWidth() - getPaddingRight() - offset;
        workingSurface.bottom = getHeight() - getPaddingBottom() - offset;

        float width = workingSurface.right - workingSurface.left;
        float height = workingSurface.bottom - workingSurface.top;
        float radius = Math.min(width, height)/2;
        float centerX = width/2;
        float centerY = height/2;

        //float left, float top, float right, float bottom
        return new RectF(centerX - radius + offset, centerY - radius + offset, centerX + radius + offset, centerY + radius + offset);
    }

    private void initializePainters(){
        mReachedBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mReachedBarPaint.setColor(mReachedArcColor);
        mReachedBarPaint.setAntiAlias(true);
        mReachedBarPaint.setStrokeWidth(mReachedArcWidth);
        mReachedBarPaint.setStyle(Paint.Style.STROKE);

        mUnreachedBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mUnreachedBarPaint.setColor(mUnreachedArcColor);
        mUnreachedBarPaint.setAntiAlias(true);
        mUnreachedBarPaint.setStrokeWidth(mUnreachedArcWidth);
        mUnreachedBarPaint.setStyle(Paint.Style.STROKE);

    }


    public int getUnreachedBarColor() {
        return mUnreachedArcColor;
    }

    public int getReachedBarColor() {
        return mReachedArcColor;
    }

    public int getProgress() {
        return mProgress;
    }

    public int getMax() {
        return mMax;
    }

    public float getReachedBarHeight(){
        return mReachedArcWidth;
    }

    public float getUnreachedBarHeight(){
        return mUnreachedArcWidth;
    }

    public void setUnreachedBarColor(int BarColor) {
        this.mUnreachedArcColor = BarColor;
        mUnreachedBarPaint.setColor(mReachedArcColor);
        invalidate();
    }

    public void setReachedBarColor(int ProgressColor) {
        this.mReachedArcColor = ProgressColor;
        mReachedBarPaint.setColor(mReachedArcColor);
        invalidate();
    }

    public void setReachedBarHeight(float height){
        mReachedArcWidth = height;
    }

    public void setUnreachedBarHeight(float height){
        mUnreachedArcWidth = height;
    }

    public void setMax(int Max) {
        if(Max > 0){
            this.mMax = Max;
            invalidate();
        }
    }

    public void setSuffix(String suffix){
        if(suffix == null){
            mSuffix = "";
        }else{
            mSuffix = suffix;
        }
    }

    public String getSuffix(){
        return mSuffix;
    }

    public void setPrefix(String prefix){
        if(prefix == null)
            mPrefix = "";
        else{
            mPrefix = prefix;
        }
    }

    public String getPrefix(){
        return mPrefix;
    }

    public void incrementProgressBy(int by){
        if(by > 0){
            setProgress(getProgress() + by);
        }
    }

    public void setProgress(int Progress) {
        if(Progress <= getMax()  && Progress >= 0){
            this.mProgress = Progress;
            this.mProgressSweep = new ProgressSweep(mProgress);
            invalidate();
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(INSTANCE_STATE, super.onSaveInstanceState());
        bundle.putFloat(INSTANCE_REACHED_BAR_HEIGHT,getReachedBarHeight());
        bundle.putFloat(INSTANCE_UNREACHED_BAR_HEIGHT, getUnreachedBarHeight());
        bundle.putInt(INSTANCE_REACHED_BAR_COLOR,getReachedBarColor());
        bundle.putInt(INSTANCE_UNREACHED_BAR_COLOR,getUnreachedBarColor());
        bundle.putInt(INSTANCE_MAX,getMax());
        bundle.putInt(INSTANCE_PROGRESS, getProgress());
        bundle.putString(INSTANCE_SUFFIX,getSuffix());
        bundle.putString(INSTANCE_PREFIX, getPrefix());
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if(state instanceof Bundle){
            final Bundle bundle = (Bundle)state;
            mReachedArcWidth = bundle.getFloat(INSTANCE_REACHED_BAR_HEIGHT);
            mUnreachedArcWidth = bundle.getFloat(INSTANCE_UNREACHED_BAR_HEIGHT);
            mReachedArcColor = bundle.getInt(INSTANCE_REACHED_BAR_COLOR);
            mUnreachedArcColor = bundle.getInt(INSTANCE_UNREACHED_BAR_COLOR);
            initializePainters();
            setMax(bundle.getInt(INSTANCE_MAX));
            setProgress(bundle.getInt(INSTANCE_PROGRESS));
            setPrefix(bundle.getString(INSTANCE_PREFIX));
            setSuffix(bundle.getString(INSTANCE_SUFFIX));
            super.onRestoreInstanceState(bundle.getParcelable(INSTANCE_STATE));
            return;
        }
        super.onRestoreInstanceState(state);
    }

    public float dp2px(float dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return  dp * scale + 0.5f;
    }

    public float sp2px(float sp){
        final float scale = getResources().getDisplayMetrics().scaledDensity;
        return sp * scale;
    }

    private class ProgressSweep {
        private static final float START_12 = 270f;
        private static final float START_3 = 0f;
        private static final float START_6 = 90f;
        private static final float START_9 = 180f;

        public float reachedStart = START_12;
        public float reachedSweep = 0f;
        public float unReachedStart = START_12;
        public float unRreachedSweep = 360f;

        private float mProgress;

        public ProgressSweep(int progress){
            mProgress = progress;
            enforceBounds();
            updateAngles();
        }

        private void enforceBounds() {
            if(mProgress < 0 ){
                mProgress = 0;
            }
            if(mProgress > mMax){
                mProgress = mMax;
            }
        }

        private void updateAngles() {
            reachedSweep = mProgress/mMax * 360f;
            unReachedStart = 270f + mProgress/mMax * 360f;
            unRreachedSweep = 360f - (mProgress/mMax * 360f);
        }

        public void increment(int val){
            mProgress += val;
            enforceBounds();
            updateAngles();
        }
    }
}

