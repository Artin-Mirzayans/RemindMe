import React, { useEffect, useMemo, useState } from "react";
import apiClient from "../Auth/apiClient";
import Loader from "../Loader/Loader";
import { EvalsSummary } from "../../props/EvalsProps";
import GenerationsChart from "./GenerationsChart";

import "./SystemContent.css";

const FEATURE_LABELS: Record<string, string> = {
  Digest: "Today & Tomorrow",
  Watchlist: "Planning Ahead",
  LocalEvents: "Nearby",
};

const FEATURE_ORDER = ["Digest", "Watchlist", "LocalEvents"];

const pct = (value: number) => `${Math.round(value * 100)}%`;
const usd = (value: number) =>
  value < 0.01 && value > 0 ? "<$0.01" : `$${value.toFixed(2)}`;

const SystemContent: React.FC = () => {
  const [summaries, setSummaries] = useState<EvalsSummary[] | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    apiClient
      .get("/evals/summary", { params: { days: 30 } })
      .then((response) => setSummaries(response.data))
      .catch((err) => {
        console.error("Error fetching system metrics:", err);
        setFailed(true);
      });
  }, []);

  const ordered = useMemo(() => {
    if (!summaries) return [];
    return FEATURE_ORDER.map((feature) =>
      summaries.find((s) => s.feature === feature)
    ).filter((s): s is EvalsSummary => s != null);
  }, [summaries]);

  const totals = useMemo(() => {
    if (ordered.length === 0) return null;
    const requests = ordered.reduce((sum, s) => sum + s.totalRequests, 0);
    const generations = ordered.reduce((sum, s) => sum + s.totalGenerations, 0);
    const successes = ordered.reduce(
      (sum, s) => sum + s.successRate * s.totalGenerations,
      0
    );
    const cost = ordered.reduce((sum, s) => sum + s.totalCostUsd, 0);
    return {
      cacheHitRate: requests === 0 ? 0 : (requests - generations) / requests,
      successRate: generations === 0 ? 0 : successes / generations,
      requests,
      generations,
      cost,
    };
  }, [ordered]);

  return (
    <div className="system-content">
      <div className="content-title">System Metrics</div>
      <p className="system-intro">
        The discovery feeds above (Today &amp; Tomorrow, Planning Ahead, Nearby)
        are curated by Claude, but only when a shared cache actually goes stale
        &mdash; everything else is served for free. This page tracks how often
        that really happens, what it costs, and how reliably it succeeds, pulled
        straight from what the app itself records.
      </p>

      {failed && (
        <p className="system-empty">Couldn&apos;t load system metrics right now.</p>
      )}

      {!failed && !summaries && (
        <div className="reminders-loader">
          <Loader />
        </div>
      )}

      {totals && (
        <>
          <div className="system-hero">
            <div className="system-hero-value">{pct(totals.cacheHitRate)}</div>
            <div className="system-hero-label">
              of the last 30 days&apos; feed requests were served from cache,
              with no new Claude call
            </div>
          </div>

          <div className="system-stat-row">
            <div className="system-stat">
              <div className="system-stat-value">{totals.requests}</div>
              <div className="system-stat-label">total feed requests</div>
            </div>
            <div className="system-stat">
              <div className="system-stat-value">{totals.generations}</div>
              <div className="system-stat-label">real Claude calls</div>
            </div>
            <div className="system-stat">
              <div className="system-stat-value">{pct(totals.successRate)}</div>
              <div className="system-stat-label">generation success rate</div>
            </div>
            <div className="system-stat">
              <div className="system-stat-value">{usd(totals.cost)}</div>
              <div className="system-stat-label">total spend, 30 days</div>
            </div>
          </div>

          <GenerationsChart summaries={ordered} labels={FEATURE_LABELS} />

          <div className="system-feature-grid">
            {ordered.map((s) => (
              <div className="system-feature-card" key={s.feature}>
                <div className="system-feature-name">
                  {FEATURE_LABELS[s.feature] ?? s.feature}
                </div>
                <dl className="system-feature-stats">
                  <div>
                    <dt>Requests</dt>
                    <dd>{s.totalRequests}</dd>
                  </div>
                  <div>
                    <dt>Real generations</dt>
                    <dd>{s.totalGenerations}</dd>
                  </div>
                  <div>
                    <dt>Cache hit rate</dt>
                    <dd>{pct(s.cacheHitRate)}</dd>
                  </div>
                  <div>
                    <dt>Success rate</dt>
                    <dd>{s.totalGenerations === 0 ? "—" : pct(s.successRate)}</dd>
                  </div>
                  <div>
                    <dt>Cost this window</dt>
                    <dd>{usd(s.totalCostUsd)}</dd>
                  </div>
                  <div>
                    <dt>Avg latency</dt>
                    <dd>
                      {s.totalGenerations === 0
                        ? "—"
                        : `${(s.avgLatencyMs / 1000).toFixed(1)}s`}
                    </dd>
                  </div>
                </dl>
              </div>
            ))}
          </div>

          <p className="system-note">
            A few of the choices behind those numbers: extended thinking is
            disabled on every call (it kept burning the token budget reasoning
            instead of answering), JSON is parsed by hand rather than through a
            structured-output helper that blew past the token limit, a lock
            keeps a burst of requests from fanning out into a burst of paid
            calls, and each feed only refreshes every 1&ndash;3 days depending on
            how time-sensitive it is.
          </p>
        </>
      )}
    </div>
  );
};

export default SystemContent;
