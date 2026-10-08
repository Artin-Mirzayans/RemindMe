import React, { useEffect, useMemo, useState } from "react";
import { MdCheckCircle, MdWarningAmber, MdHourglassEmpty } from "react-icons/md";
import apiClient from "../Auth/apiClient";
import Loader from "../Loader/Loader";
import { FeedHealth } from "../../props/FeedHealthProps";

import "./AiHealthContent.css";

const FEATURES = [
  { key: "Digest", label: "Today & Tomorrow", cadence: "Updates every day" },
  { key: "Watchlist", label: "Planning Ahead", cadence: "Updates every few days" },
  { key: "LocalEvents", label: "Nearby", cadence: "Updates every few days for each area" },
];

const HEALTHY_AT = 0.8;

type State = "healthy" | "degraded" | "none";

const stateOf = (h?: FeedHealth): State => {
  if (!h || h.successRate == null) return "none";
  return h.successRate >= HEALTHY_AT ? "healthy" : "degraded";
};

const pct = (value: number) => `${Math.round(value * 100)}%`;

const seconds = (ms: number) => {
  const s = ms / 1000;
  return s < 10 ? `${s.toFixed(1)} seconds` : `${Math.round(s)} seconds`;
};

const STATUS = {
  healthy: { label: "Working well", Icon: MdCheckCircle },
  degraded: { label: "Needs attention", Icon: MdWarningAmber },
  none: { label: "No data yet", Icon: MdHourglassEmpty },
};

const AiHealthContent: React.FC = () => {
  const [health, setHealth] = useState<FeedHealth[] | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    apiClient
      .get("/evals/health", { params: { days: 30 } })
      .then((response) => setHealth(response.data))
      .catch((err) => {
        console.error("Error fetching AI health:", err);
        setFailed(true);
      });
  }, []);

  const rows = useMemo(
    () =>
      FEATURES.map((feature) => ({
        ...feature,
        health: health?.find((h) => h.feature === feature.key),
      })),
    [health]
  );

  const overall = useMemo((): State => {
    const states = rows.map((r) => stateOf(r.health)).filter((s) => s !== "none");
    if (states.length === 0) return "none";
    return states.every((s) => s === "healthy") ? "healthy" : "degraded";
  }, [rows]);

  const headline = {
    healthy: "Everything is running smoothly",
    degraded: "Something needs attention",
    none: "Collecting data",
  }[overall];

  return (
    <div className="ai-health">
      <div className="content-title">AI Health</div>
      <p className="ai-health-intro">
        RemindMe uses AI to pick the events you see on Today &amp; Tomorrow,
        Planning Ahead and Nearby. This page shows how that&apos;s going: how
        often it works, and how long it takes.
      </p>

      {failed && (
        <p className="ai-health-empty">Couldn&apos;t load AI health right now.</p>
      )}

      {!failed && !health && (
        <div className="reminders-loader">
          <Loader />
        </div>
      )}

      {health && (
        <>
          <div className={`ai-health-hero ai-health-hero--${overall}`}>
            {React.createElement(STATUS[overall].Icon, { size: 34 })}
            <div className="ai-health-hero-text">{headline}</div>
            <div className="ai-health-hero-sub">Based on the last 30 days</div>
          </div>

          <div className="ai-health-grid">
            {rows.map(({ key, label, cadence, health: h }) => {
              const state = stateOf(h);
              const { label: statusLabel, Icon } = STATUS[state];
              return (
                <div className="ai-health-card" key={key}>
                  <div className="ai-health-card-head">
                    <div className="ai-health-card-name">{label}</div>
                    <span className={`ai-health-chip ai-health-chip--${state}`}>
                      <Icon size={16} />
                      {statusLabel}
                    </span>
                  </div>
                  <div className="ai-health-cadence">{cadence}</div>

                  <div className="ai-health-metric">
                    <div className="ai-health-metric-row">
                      <span>How often it worked</span>
                      <strong>{h?.successRate == null ? "—" : pct(h.successRate)}</strong>
                    </div>
                    <div
                      className="ai-health-meter"
                      role="img"
                      aria-label={
                        h?.successRate == null
                          ? "No data yet"
                          : `Worked ${pct(h.successRate)} of the time`
                      }
                    >
                      <div
                        className="ai-health-meter-fill"
                        style={{ width: `${Math.round((h?.successRate ?? 0) * 100)}%` }}
                      />
                    </div>
                  </div>

                  <div className="ai-health-metric-row">
                    <span>Time to update</span>
                    <strong>{h?.avgLatencyMs == null ? "—" : seconds(h.avgLatencyMs)}</strong>
                  </div>
                </div>
              );
            })}
          </div>

          <ul className="ai-health-tracked">
            <li>
              Every time the AI runs, we record whether it worked, how long it
              took, and what it used.
            </li>
            <li>
              The picks are made once and shared with everyone, so the AI only
              runs when something new is needed &mdash; not every time someone
              visits.
            </li>
            <li>
              Safeguards stop a rush of visitors from causing a rush of AI runs.
            </li>
          </ul>
        </>
      )}
    </div>
  );
};

export default AiHealthContent;
