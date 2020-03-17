package coffee.nils.dev.receipts;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;
import androidx.appcompat.widget.AppCompatSeekBar;

public class ReverseSeekBar extends AppCompatSeekBar
{

    public ReverseSeekBar(Context context) {
        super(context);
    }

    public ReverseSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ReverseSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float px = this.getWidth() / 2.0f;
        float py = this.getHeight() / 2.0f;

        canvas.scale(-1, 1, px, py);

        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        event.setLocation(this.getWidth() - event.getX(), event.getY());

        return super.onTouchEvent(event);
    }
}
