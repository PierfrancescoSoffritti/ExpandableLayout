package com.pierfrancescosoffritti.expandablelayout;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.Set;

public class ExpandableLayout extends LinearLayout {

    private static final int EXPANSION_DEFAULT_DURATION = 300;

    private static final byte _EXPANDING = 0;
    private static final byte _COLLAPSING = 1;

    @IntDef({_EXPANDING, _COLLAPSING})
    @Retention(RetentionPolicy.SOURCE)
    private @interface ExpansionDirection {}

    public static final int COLLAPSED = 0;
    public static final int ANIMATING = 1;
    public static final int EXPANDED = 2;
    @IntDef({EXPANDED, COLLAPSED, ANIMATING})
    @Retention(RetentionPolicy.SOURCE)
    private @interface State {}

    private @State int state = COLLAPSED;

    // view that will expand
    private View expandableView;

    // current slide value, between 1.0 and 0.0 (1.0 = EXPANDED, 0.0 = COLLAPSED)
    private float currentExpansion;

    // max value by which expandableView view can expand.
    private int maxExpansion = -1;

    @ExpansionDirection private byte expansionDirection;

    // duration of the expansion in milliseconds
    private long expansionDuration = EXPANSION_DEFAULT_DURATION;

    private ValueAnimator valueAnimator = new ValueAnimator();
    private Interpolator interpolator = new AccelerateDecelerateInterpolator();

    private final Set<OnExpandListener> listeners;

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

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        expandableView = findViewById(R.id.expandable_view);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if(maxExpansion < 0)
            for(int i=0; i<getChildCount(); i++) {
                View child = getChildAt(i);
                if(child.getId() == R.id.expandable_view) {
                    maxExpansion = child.getMeasuredHeight();
                    if(state == COLLAPSED)
                        animate(0, 0);

                }
            }
    }

    public void toggle(boolean animate) {
        System.out.println("state: " +state);
        System.out.println("expansionDirection: " +expansionDirection);

        if(state == EXPANDED || (state == ANIMATING && expansionDirection == _EXPANDING))
            collapse(animate);
        else if(state == COLLAPSED || (state == ANIMATING && expansionDirection == _COLLAPSING))
            expand(animate);
    }

    public void expand(boolean animate) {
        if(state == EXPANDED)
            return;

        if(valueAnimator.isRunning())
            valueAnimator.cancel();

        long duration = animate ? expansionDuration : 0;
        animate(maxExpansion, duration);
    }

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

        syncHeight();

        expansionDirection = finalHeight == maxExpansion ? _EXPANDING : _COLLAPSING;
        System.out.println("expansionDirection2: " +expansionDirection);

        valueAnimator = ValueAnimator.ofInt(expandableView.getLayoutParams().height, finalHeight);
        valueAnimator.setInterpolator(interpolator);
        valueAnimator.setDuration(duration);
        valueAnimator.addUpdateListener(animatorUpdateListener);
        valueAnimator.start();
    }

    private final ValueAnimator.AnimatorUpdateListener animatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int animationValue = (Integer) animation.getAnimatedValue();

            if(animationValue < 0 || animationValue > maxExpansion)
                Log.i(getClass().getSimpleName(), "Warning. The animation value are out of range, check the provided interpolator");

            ViewGroup.LayoutParams layoutParams = expandableView.getLayoutParams();
            layoutParams.height = animationValue;
            expandableView.setLayoutParams(layoutParams);

            updateState(animationValue);
        }
    };

    private void syncHeight() {
        if(state == COLLAPSED) {
            ViewGroup.LayoutParams layoutParams = expandableView.getLayoutParams();
            layoutParams.height = 0;
            expandableView.setLayoutParams(layoutParams);
        } else if(state == EXPANDED) {
            ViewGroup.LayoutParams layoutParams = expandableView.getLayoutParams();
            layoutParams.height = maxExpansion;
            expandableView.setLayoutParams(layoutParams);
        }
    }

    private void updateState(int currentHeight) {
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
    }

    /**
     * Implement this interface if you want to observe slide changes
     */
    public interface OnExpandListener {
        void onExpand(ExpandableLayout expandableLayout, float currentExpansion);
    }
}
