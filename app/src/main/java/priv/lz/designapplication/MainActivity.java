package priv.lz.designapplication;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.android.view.DrawableSpan;

/**
 * @author lizan
 */
public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();

        LinearLayout view1 = findViewById(R.id.view1);
        LinearLayout view2 = findViewById(R.id.view2);
        LinearLayout view3 = findViewById(R.id.view3);

        Intent intent1 = new Intent(this, ASRActivity.class);
        Intent intent2 = new Intent(this, TTSActivity.class);
        Intent intent3 = new Intent(this, OCRActivity.class);
        button(view1, intent1);
        button(view2, intent2);
        button(view3, intent3);
    }

    private void button(LinearLayout view1, Intent intent) {
        DrawableSpan drawable = new DrawableSpan(DrawableSpan.Style.RECTANGLE, 30, 30);
        drawable.setRound(new float[]{30});
        drawable.setModel(DrawableSpan.Model.FLAT);
        drawable.setColor(0xFFECECEC); //0xFFECECEC
        drawable.setContainerdeltaLength(20);


        final DrawableSpan drawable1 = new DrawableSpan(drawable, DrawableSpan.Style.CIRCLE, DrawableSpan.Model.FLAT);

        final DrawableSpan.Model[] mode = new DrawableSpan.Model[]{
                DrawableSpan.Model.FLAT,
                DrawableSpan.Model.CONCAVE,
                DrawableSpan.Model.CONVEX,
                DrawableSpan.Model.PRESSED
        };

        final DrawableSpan drawable6 = new DrawableSpan(drawable, DrawableSpan.Model.FLAT);
        DrawableSpan.invalidateView(drawable6, view1);

        final int[] length = drawable6.getContainerdeltaLength();
        final ValueAnimator valueAnimator = ValueAnimator.ofInt(length[0], 0);
        // 持续时间
        valueAnimator.setDuration(4); //button动画持续时间@@@@@@@@@@@@@@@@@@@@@@@
        // 加速插值器
        valueAnimator.setInterpolator(new AccelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                drawable6.setContainerdeltaLength(value);
                drawable6.invalidate();
            }
        });
        final ValueAnimator valueAnimator1 = ValueAnimator.ofInt(0, length[0]);
        // 持续时间
        valueAnimator1.setDuration(200);
        // 加速插值器
        valueAnimator1.setInterpolator(new AccelerateInterpolator());
        valueAnimator1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                drawable6.setContainerdeltaLength(value);
                drawable6.invalidate();
            }
        });
        valueAnimator.addListener(new Animator.AnimatorListener(){
            @Override
            public void onAnimationStart(Animator p1) {
            }
            @Override
            public void onAnimationEnd(Animator p1) {
                valueAnimator1.start();
            }
            @Override
            public void onAnimationCancel(Animator p1) {
            }
            @Override
            public void onAnimationRepeat(Animator p1) {
            }
        });

        view1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                valueAnimator.start();
                startActivity(intent);
            }
        });

    }
}





