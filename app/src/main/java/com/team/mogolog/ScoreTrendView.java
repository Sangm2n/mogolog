package com.team.mogolog;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Draws the most recent raw scores supplied for one subject, in exam date order. */
public final class ScoreTrendView extends View {
    private static final int MAX_POINTS = 5;
    private static final int VERTICAL_SCORE_MARGIN = 5;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics fontMetrics = new Paint.FontMetrics();
    private final Path path = new Path();
    private final float dotRadius;
    private final float lineWidth;
    private final float labelGap;
    private final int primaryColor;
    private final int baselineColor;
    private final String emptyMessage;

    private List<Point> points = Collections.emptyList();
    private List<String> labels = Collections.emptyList();

    public ScoreTrendView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dotRadius = getResources().getDimension(R.dimen.home_score_chart_dot_radius);
        lineWidth = getResources().getDimension(R.dimen.home_score_chart_stroke_width);
        labelGap = getResources().getDimension(R.dimen.space_4);
        primaryColor = ContextCompat.getColor(context, R.color.primary);
        baselineColor = ContextCompat.getColor(context, R.color.surface_muted);
        emptyMessage = context.getString(R.string.home_score_trend_empty);
        labelPaint.setColor(ContextCompat.getColor(context, R.color.text_sub));
        labelPaint.setTextSize(getResources().getDimension(R.dimen.home_score_chart_label_size));
        labelPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.getFontMetrics(fontMetrics);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        if (isInEditMode()) {
            setPoints(Arrays.asList(new Point("6월", 72), new Point("7월", 76), new Point("8월", 74),
                    new Point("9월", 84), new Point("실모", 86)));
        } else {
            setContentDescription(emptyMessage);
        }
    }

    /** Expects points ordered oldest to newest. Pass an empty list to show the empty state. */
    public void setPoints(@NonNull List<Point> examScores) {
        int first = Math.max(0, examScores.size() - MAX_POINTS);
        points = new ArrayList<>(examScores.subList(first, examScores.size()));
        labels = new ArrayList<>(points.size());
        StringBuilder description = new StringBuilder();
        for (Point point : points) {
            labels.add(getResources().getString(R.string.home_score_trend_point,
                    point.examLabel, point.rawScore));
            if (description.length() > 0) {
                description.append(", ");
            }
            description.append(getResources().getString(R.string.home_score_trend_point_description,
                    point.examLabel, point.rawScore));
        }
        setContentDescription(points.isEmpty() ? emptyMessage : description.toString());
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        if (points.isEmpty()) {
            float centeredBaseline = (height - fontMetrics.ascent - fontMetrics.descent) / 2f;
            canvas.drawText(emptyMessage, width / 2f, centeredBaseline, labelPaint);
            return;
        }

        float labelBaseline = height - fontMetrics.descent;
        float baseline = Math.max(dotRadius, labelBaseline + fontMetrics.ascent - 2 * labelGap);
        linePaint.setColor(baselineColor);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(labelGap / 4f);
        canvas.drawLine(0f, baseline, width, baseline, linePaint);

        int minScore = points.get(0).rawScore;
        int maxScore = minScore;
        for (Point point : points) {
            minScore = Math.min(minScore, point.rawScore);
            maxScore = Math.max(maxScore, point.rawScore);
        }
        int lowerBound = minScore - VERTICAL_SCORE_MARGIN;
        int upperBound = maxScore + VERTICAL_SCORE_MARGIN;
        float plotHeight = Math.max(0f, baseline - 2 * dotRadius);
        float cellWidth = width / points.size();

        path.rewind();
        for (int index = 0; index < points.size(); index++) {
            float x = cellWidth * (index + 0.5f);
            float y = chartY(points.get(index).rawScore, lowerBound, upperBound, plotHeight);
            if (index == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
            CharSequence label = TextUtils.ellipsize(labels.get(index), labelPaint,
                    Math.max(0f, cellWidth - labelGap), TextUtils.TruncateAt.END);
            canvas.drawText(label, 0, label.length(), x, labelBaseline, labelPaint);
        }
        linePaint.setColor(primaryColor);
        linePaint.setStrokeWidth(lineWidth);
        canvas.drawPath(path, linePaint);

        linePaint.setStyle(Paint.Style.FILL);
        for (int index = 0; index < points.size(); index++) {
            float x = cellWidth * (index + 0.5f);
            float y = chartY(points.get(index).rawScore, lowerBound, upperBound, plotHeight);
            canvas.drawCircle(x, y, dotRadius, linePaint);
        }
    }

    private float chartY(int score, int lowerBound, int upperBound, float plotHeight) {
        return dotRadius + (upperBound - score) * plotHeight / (upperBound - lowerBound);
    }

    public static final class Point {
        private final String examLabel;
        private final int rawScore;

        public Point(@NonNull String examLabel, int rawScore) {
            this.examLabel = examLabel;
            this.rawScore = rawScore;
        }
    }
}
