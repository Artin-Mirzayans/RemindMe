import React, { useState } from "react";
import { EvalsSummary } from "../../props/EvalsProps";

import "./GenerationsChart.css";

interface Props {
  summaries: EvalsSummary[];
  labels: Record<string, string>;
}

// slots 1-3 of the validated categorical palette (blue / orange / aqua) - the
// only three that clear every colorblind-safety check pairwise, which is all
// three features need
const SERIES_COLORS = ["var(--series-1)", "var(--series-2)", "var(--series-3)"];

const CHART_WIDTH = 920;
const CHART_HEIGHT = 220;
const BAR_MAX_WIDTH = 18;
const SEGMENT_GAP = 2;
const AXIS_LEFT = 34;
const AXIS_BOTTOM = 24;
const AXIS_TOP = 10;

const niceMax = (value: number): number => {
  if (value <= 0) return 4;
  const magnitude = Math.pow(10, Math.floor(Math.log10(value)));
  const steps = [1, 2, 5, 10];
  for (const step of steps) {
    if (value <= step * magnitude) return step * magnitude;
  }
  return 10 * magnitude;
};

const formatDay = (iso: string) =>
  new Date(`${iso}T00:00:00Z`).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    timeZone: "UTC",
  });

const GenerationsChart: React.FC<Props> = ({ summaries, labels }) => {
  const [hovered, setHovered] = useState<number | null>(null);

  const days = summaries[0]?.daily.map((d) => d.day) ?? [];
  if (days.length === 0) return null;

  const perDay = days.map((day, i) =>
    summaries.map((s) => s.daily[i]?.generations ?? 0)
  );
  const totalsPerDay = perDay.map((values) => values.reduce((a, b) => a + b, 0));
  const maxTotal = niceMax(Math.max(...totalsPerDay));

  const plotWidth = CHART_WIDTH - AXIS_LEFT;
  const plotHeight = CHART_HEIGHT - AXIS_BOTTOM - AXIS_TOP;
  const slot = plotWidth / days.length;
  const barWidth = Math.min(BAR_MAX_WIDTH, slot * 0.6);

  // dedupe after rounding - a maxTotal of 1 would otherwise show "1" twice
  // (0.5 rounds up to 1, same as the max itself)
  const yTicks = Array.from(new Set([0, Math.round(maxTotal / 2), maxTotal]));

  return (
    <div className="generations-chart">
      <div className="generations-chart-head">
        <h3 className="generations-chart-title">Real Claude calls per day</h3>
        <ul className="generations-chart-legend">
          {summaries.map((s, i) => (
            <li key={s.feature}>
              <span
                className="generations-chart-swatch"
                style={{ background: SERIES_COLORS[i] }}
              />
              {labels[s.feature] ?? s.feature}
            </li>
          ))}
        </ul>
      </div>

      <svg
        viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`}
        className="generations-chart-svg"
        role="img"
        aria-label="Real generations per day, by feature, over the last 30 days"
      >
        {yTicks.map((tick) => {
          const y = AXIS_TOP + plotHeight - (tick / maxTotal) * plotHeight;
          return (
            <g key={tick}>
              <line
                x1={AXIS_LEFT}
                x2={CHART_WIDTH}
                y1={y}
                y2={y}
                className="generations-chart-gridline"
              />
              <text x={AXIS_LEFT - 8} y={y} className="generations-chart-tick">
                {Math.round(tick)}
              </text>
            </g>
          );
        })}

        {days.map((day, i) => {
          const x = AXIS_LEFT + i * slot + (slot - barWidth) / 2;
          let cumulative = 0;
          const segments = perDay[i].map((value, seriesIndex) => {
            const height = (value / maxTotal) * plotHeight;
            const y = AXIS_TOP + plotHeight - cumulative - height;
            cumulative += height;
            return { value, height, y, seriesIndex };
          });

          return (
            <g
              key={day}
              onMouseEnter={() => setHovered(i)}
              onMouseLeave={() => setHovered(null)}
              onFocus={() => setHovered(i)}
              onBlur={() => setHovered(null)}
              tabIndex={0}
              className="generations-chart-column"
            >
              <rect
                x={AXIS_LEFT + i * slot}
                y={AXIS_TOP}
                width={slot}
                height={plotHeight}
                fill="transparent"
              />
              {segments
                .filter((seg) => seg.value > 0)
                .map((seg, idx, visible) => (
                  <rect
                    key={seg.seriesIndex}
                    x={x}
                    y={seg.y + (idx < visible.length - 1 ? SEGMENT_GAP / 2 : 0)}
                    width={barWidth}
                    height={Math.max(0, seg.height - SEGMENT_GAP)}
                    fill={SERIES_COLORS[seg.seriesIndex]}
                    rx={idx === visible.length - 1 ? 3 : 0}
                    className={hovered === i ? "is-hovered" : undefined}
                  />
                ))}
            </g>
          );
        })}

        <line
          x1={AXIS_LEFT}
          x2={CHART_WIDTH}
          y1={AXIS_TOP + plotHeight}
          y2={AXIS_TOP + plotHeight}
          className="generations-chart-axis"
        />
      </svg>

      {hovered != null && (
        <div className="generations-chart-tooltip">
          <div className="generations-chart-tooltip-day">{formatDay(days[hovered])}</div>
          {summaries.map((s, i) =>
            perDay[hovered][i] > 0 ? (
              <div className="generations-chart-tooltip-row" key={s.feature}>
                <span
                  className="generations-chart-tooltip-key"
                  style={{ background: SERIES_COLORS[i] }}
                />
                <span className="generations-chart-tooltip-name">
                  {labels[s.feature] ?? s.feature}
                </span>
                <span className="generations-chart-tooltip-value">
                  {perDay[hovered][i]}
                </span>
              </div>
            ) : null
          )}
          {totalsPerDay[hovered] === 0 && (
            <div className="generations-chart-tooltip-empty">No generations that day</div>
          )}
        </div>
      )}
    </div>
  );
};

export default GenerationsChart;
