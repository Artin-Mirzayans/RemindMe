import React, { useEffect, useMemo, useState } from "react";
import { MdCheckCircle, MdWarningAmber, MdHourglassEmpty } from "react-icons/md";
import apiClient from "../Auth/apiClient";
import Loader from "../Loader/Loader";
import { FeedHealth } from "../../props/FeedHealthProps";

import "./AiHealthContent.css";

const FEATURES = [
  { key: "Digest", label: "Today & Tomorrow", cadence: "Refreshed daily" },
  { key: "Watchlist", label: "Planning Ahead", cadence: "Refreshed every few days" },
  { key: "LocalEvents", label: "Nearby", cadence: "Refreshed per area, every few days" },
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
  return s < 10 ? `${s.toFixed(1)}s` : `${Math.round(s)}s`;
};

const STATUS = {
  healthy: { label: "Healthy", Icon: MdCheckCircle },
  degraded: { label: "Degraded", Icon: MdWarningAmber },
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
    healthy: "All AI feeds are healthy",
    degraded: "Some AI feeds are degraded",
    none: "Collecting data",
  }[overall];

  return (
    <div className="ai-health">
      <div className="content-title">AI Health</div>
      <p className="ai-health-intro">
        Today &amp; Tomorrow, Planning Ahead and Nearby are curated by Claude,
        but only refreshed when they go stale. This page shows how those AI
        features are holding up &mdash; how reliably they succeed and how long
        a refresh takes &mdash; straight from what the app records on every run.
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
            <div className="ai-health-hero-sub">based on the last 30 days of runs</div>
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
                      <span>Reliability</span>
                      <strong>{h?.successRate == null ? "—" : pct(h.successRate)}</strong>
                    </div>
                    <div
                      className="ai-health-meter"
                      role="img"
                      aria-label={
                        h?.successRate == null
                          ? "No reliability data yet"
                          : `Reliability ${pct(h.successRate)}`
                      }
                    >
                      <div
                        className="ai-health-meter-fill"
                        style={{ width: `${Math.round((h?.successRate ?? 0) * 100)}%` }}
                      />
                    </div>
                  </div>

                  <div className="ai-health-metric-row">
                    <span>Typical refresh time</span>
                    <strong>{h?.avgLatencyMs == null ? "—" : seconds(h.avgLatencyMs)}</strong>
                  </div>
                </div>
              );
            })}
          </div>

          <ul className="ai-health-tracked">
            <li>
              Every AI call is logged with its outcome, speed, token usage and
              estimated cost.
            </li>
            <li>
              Feeds are generated once and shared, so AI usage follows how
              often content changes &mdash; not how many people visit.
            </li>
            <li>
              A lock stops a burst of requests from becoming a burst of paid
              calls, and extended thinking is off to keep every call fast and
              cheap.
            </li>
          </ul>
        </>
      )}
    </div>
  );
};

export default AiHealthContent;
