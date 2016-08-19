package com.pierfrancescosoffritti.expandablelayout;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.Set;

public class ExpandableLayout extends LinearLayout {

    private static final int EXPANSION_DEFAULT_DURATION = 300;

    // direction in which the layout is moving
    private static final byte _EXPANDING = 0;
    private static final byte _COLLAPSING = 1;
    @IntDef({_EXPANDING, _COLLAPSING})
    @Retention(RetentionPolicy.SOURCE)
    private @interface ExpansionDirection {}

    @ExpansionDirection private byte expansionDirection;

    // layout state
    public static final int COLLAPSED = 0;
    public static final int ANIMATING = 1;
    public static final int EXPANDED = 2;
    @IntDef({EXPANDED, COLLAPSED, ANIMATING})
    @Retention(RetentionPolicy.SOURCE)
    private @interface State {}

    @State private int state = COLLAPSED;

    // view that will expand
    @Nullable private View expandableView;

    // current slide value, between 1.0 and 0.0 (1.0 = EXPANDED, 0.0 = COLLAPSED)
    @FloatRange(from = 0f, to = 1f) private float currentExpansion;

    // max value by which expandableView view can expand.
    private int maxExpansion = -1;

    // duration of the expansion in milliseconds
    private long expansionDuration = EXPANSION_DEFAULT_DURATION;

    @NonNull private ValueAnimator valueAnimator = new ValueAnimator();
    @NonNull private Interpolator interpolator = new AccelerateDecelerateInterpolator();

    @NonNull private final Set<OnExpandListener> listeners;

    public ExpandableLayout(Context context) {
        this(context, null);
    }

    public ExpandableLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpandableLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        listeners = new HashSet<>();
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
    }

    public void setExpansionDuration(long expansionDuration) {
        this.expansionDuration = expansionDuration;
    }

    public void setInterpolator(@NonNull Interpolator interpolator) {
        if(interpolator instanceof OvershootInterpolator)
            Log.e(getClass().getSimpleName(), "Warning: " +OvershootInterpolator.class.getSimpleName() +" may cause problem to the animation");

        this.interpolator = interpolator;
    }

    /**
     * @return the state of the view. {@link ExpandableLayout#COLLAPSED}, {@link ExpandableLayout#EXPANDED} or {@link ExpandableLayout#ANIMATING}
     */
    @State
    public int getState() {
        return state;
    }

    /**
     * @return the provided expandable view
     */
    @Nullable
    public View getExpandableView() {
        return expandableView;
    }

    /**
     * get the current expansion
     * @return a float from 0 to 1. 0 if collapsed and 1 if expanded
     */
    @FloatRange(from = 0f, to = 1f)
    public float getCurrentExpansion() {
        return currentExpansion;
    }

    /**
     * @return the duration of the expand/collapse animation
     */
    public long getExpansionDuration() {
        return expansionDuration;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        expandableView = findViewById(R.id.expandable_view);

        if(expandableView == null)
            throw new IllegalStateException("No view with the id 'R.id.expandable_view'");
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        if(expandableView == null)
            throw new IllegalStateException("expandableView == null");

        if(maxExpansion < 0) {
            recalculateMaxExpansion();

            if (state == COLLAPSED)
                animate(0, 0);
        }

        super.onLayout(changed, l, t, r, b);
    }

    public void recalculateMaxExpansion() {
        if(expandableView == null)
            throw new IllegalStateException("expandableView == null");

        maxExpansion = 0;

        if(expandableView instanceof ViewGroup) {
            for(int i=0; i<((ViewGroup) expandableView).getChildCount(); i++) {
                View child = ((ViewGroup) expandableView).getChildAt(i);
                if(child.getVisibility() != GONE)
                    maxExpansion += child.getMeasuredHeight();
            }
        } else
            maxExpansion = expandableView.getMeasuredHeight();
    }

    /**
     * If expandable view is  collapsed: expand. Else: collapse
     * @param animate should play animation?
     */
    public void toggle(boolean animate) {
        if(state == EXPANDED || (state == ANIMATING && expansionDirection == _EXPANDING))
            collapse(animate);
        else if(state == COLLAPSED || (state == ANIMATING && expansionDirection == _COLLAPSING))
            expand(animate);
    }

    /**
     * Expand the expandable view
     * @param animate should play animation?
     */
    public void expand(boolean animate) {
        if(state == EXPANDED)
            return;

        if(valueAnimator.isRunning())
            valueAnimator.cancel();

        long duration = animate ? expansionDuration : 0;
        animate(maxExpansion, duration);
    }

    /**
     * Collapse the expandable view
     * @param animate should play animation?
     */
    public void collapse(boolean animate) {
        if(state == COLLAPSED)
            return;

        if(valueAnimator.isRunning())
            valueAnimator.cancel();

        long duration = animate ? expansionDuration : 0;
        animate(0, duration);
    }

    private void animate(final int finalHeight, long duration) {
        if(finalHeight != 0 && finalHeight != maxExpansion)
            throw new IllegalArgumentException("finalHeight != 0 && finalHeight != maxExpansion");
        if(expandableView == null)
            throw new IllegalArgumentException("expandableView == null");

        // just in case .. :)
        syncHeight();

        // set animation direction
        expansionDirection = finalHeight == maxExpansion ? _EXPANDING : _COLLAPSING;

        valueAnimator = ValueAnimator.ofInt(expandableView.getLayoutParams().height, finalHeight);
        valueAnimator.setInterpolator(interpolator);
        valueAnimator.setDuration(duration);
        valueAnimator.addUpdateListener(animatorUpdateListener);
        valueAnimator.start();
    }

    private final ValueAnimator.AnimatorUpdateListener animatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if(expandableView == null)
                throw new IllegalArgumentException("expandableView == null");

            int animationValue = (Integer) animation.getAnimatedValue();

            if(animationValue < 0 || animationValue > maxExpansion)
                Log.e(getClass().getSimpleName(), "Warning: The animation values are out of range, check the interpolator");

            ViewGroup.LayoutParams layoutParams = expandableView.getLayoutParams();
            layoutParams.height = animationValue;
            expandableView.setLayoutParams(layoutParams);

            updateState(animationValue);
        }
    };

    private void syncHeight() {
        if(expandableView == null)
            throw new IllegalArgumentException("expandableView == null");
        if(maxExpansion < 0)
            throw new IllegalStateException("maxExpansion < 0");

        ViewGroup.LayoutParams layoutParams = expandableView.getLayoutParams();

        if(state == COLLAPSED)
            layoutParams.height = 0;
        else if(state == EXPANDED)
            layoutParams.height = maxExpansion;
        else
            return;

        expandableView.setLayoutParams(layoutParams);
    }

    /**
     * only method responsible for updating the state of the view
     * @param currentHeight expandable view height
     */
    private void updateState(int currentHeight) {
        if(expandableView == null)
            throw new IllegalArgumentException("expandableView == null");

        if(currentHeight == maxExpansion) {
            state = EXPANDED;
            expandableView.setVisibility(VISIBLE);
        } else if(currentHeight == 0) {
            state = COLLAPSED;
            expandableView.setVisibility(GONE);
        } else {
            state = ANIMATING;
            expandableView.setVisibility(VISIBLE);
        }

        currentExpansion = (float)currentHeight/maxExpansion;
        notifyListeners(currentExpansion);
    }

    public void addOnExpandListener(@NonNull OnExpandListener listener) {
        listeners.add(listener);
    }

    public void removeOnExpandListener(@NonNull OnExpandListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(float currentSlide) {
        for(OnExpandListener listener : listeners)
            listener.onExpand(this, currentSlide);
    }

    /**
     * Implement this interface if you want to observe expansion changes
     */
    public interface OnExpandListener {
        void onExpand(ExpandableLayout expandableLayout, float currentExpansion);
    }
}
