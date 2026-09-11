import React, { useEffect, useMemo, useRef, useState } from "react";
import apiClient from "../Auth/apiClient";
import Loader from "../Loader/Loader";
import NearbyCard from "./NearbyCard";
import RatingFilterBar, { RatingFilter } from "../common/RatingFilterBar";
import RefreshControl from "../common/RefreshControl";
import { rememberNearbyCity } from "./nearbyCity";
import { useUser } from "../Auth/UserContext";
import { LocalEventProps } from "../../props/LocalEventsProps";

import "./NearbyContent.css";

type NearbyResult = {
  events: LocalEventProps[];
  location: string | null;
  range: { start: string; end: string } | null;
  nextRefreshAt: string | null;
};

// kept for the life of the tab so switching in and out of Nearby doesn't re-hit the API every
// time - only a full reload clears it, an explicit refresh overwrites it
let cachedResult: NearbyResult | null = null;

const formatDay = (isoDate: string) =>
  new Date(isoDate).toLocaleDateString(undefined, { month: "long", day: "numeric" });

const NearbyContent = () => {
  const { user } = useUser();
  const [events, setEvents] = useState<LocalEventProps[]>([]);
  const [location, setLocation] = useState<string | null>(null);
  const [range, setRange] = useState<{ start: string; end: string } | null>(null);
  const [nextRefreshAt, setNextRefreshAt] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [failed, setFailed] = useState(false);
  const [filter, setFilter] = useState<RatingFilter>("all");
  const inFlight = useRef(false);

  const applyResult = (result: NearbyResult) => {
    setEvents(result.events);
    setLocation(result.location);
    setRange(result.range);
    setNextRefreshAt(result.nextRefreshAt);
    rememberNearbyCity(result.location);
  };

  const fetchNearby = async ({ force = false } = {}) => {
    if (!force && cachedResult) {
      applyResult(cachedResult);
      setLoading(false);
      setFailed(false);
      return;
    }

    if (inFlight.current) return;
    inFlight.current = true;
    if (!force) setLoading(true);
    setFailed(false);
    try {
      const response = await apiClient.get("/local", {
        params: force ? { refresh: true } : {},
      });
      const result: NearbyResult = {
        events: response.data?.events ?? [],
        location: response.data?.location ?? null,
        range:
          response.data?.windowStart && response.data?.windowEnd
            ? { start: response.data.windowStart, end: response.data.windowEnd }
            : null,
        nextRefreshAt: response.data?.nextRefreshAt ?? null,
      };
      cachedResult = result;
      applyResult(result);
    } catch (err) {
      console.error("Error fetching local events:", err);
      setFailed(true);
    } finally {
      inFlight.current = false;
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNearby();
  }, []);

  const visibleEvents = useMemo(
    () => (filter === "all" ? events : events.filter((e) => e.rating === filter)),
    [events, filter]
  );

  const cityLabel = location ? location.split(",")[0].trim() : "Nearby";

  const handleRefresh = async () => {
    setRefreshing(true);
    await fetchNearby({ force: true });
    setRefreshing(false);
  };

  return (
    <div className="nearby-content">
      <div className="content-title">{cityLabel}</div>

      {!user && (
        <p className="nearby-intro">
          The concerts, games and shows worth leaving the house for &mdash; the
          biggest things booked near you over the next couple weeks.
        </p>
      )}

      {!loading && !failed && range && (
        <p className="nearby-subtitle">
          {location ? `Events near ${location}` : "Events near you"}
          {` · ${formatDay(range.start)}–${formatDay(range.end)}`}
        </p>
      )}

      {loading ? (
        <>
          <div className="reminders-loader">
            <Loader />
          </div>
          <p className="nearby-loading-note">
            Pulling together the biggest events near you &mdash; give it a moment.
          </p>
        </>
      ) : failed ? (
        <p className="nearby-empty">Couldn&apos;t load events right now. Try again shortly.</p>
      ) : events.length === 0 ? (
        <p className="nearby-empty">
          Nothing notable found near {location ?? "you"} in the next couple weeks.
        </p>
      ) : (
        <>
          <RefreshControl
            nextRefreshAt={nextRefreshAt}
            refreshing={refreshing}
            onRefresh={handleRefresh}
          />
          <RatingFilterBar value={filter} onChange={setFilter} />
          {visibleEvents.length === 0 ? (
            <p className="nearby-empty">Nothing at this rating right now.</p>
          ) : (
            <div className="nearby-list">
              {visibleEvents.map((event, index) => (
                <NearbyCard key={`${event.title}-${index}`} event={event} />
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default NearbyContent;
