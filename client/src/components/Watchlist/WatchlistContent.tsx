import React, { useCallback, useEffect, useMemo, useState } from "react";
import apiClient from "../Auth/apiClient";
import Loader from "../Loader/Loader";
import WatchlistCard from "./WatchlistCard";
import SignInPrompt from "../Auth/SignInPrompt";
import RatingFilterBar, { RatingFilter } from "../common/RatingFilterBar";
import RefreshControl from "../common/RefreshControl";
import { useUser } from "../Auth/UserContext";
import { WatchlistEventProps } from "../../props/WatchlistProps";

import "./WatchlistContent.css";

const SAMPLE_EVENTS: WatchlistEventProps[] = [
  {
    title: "NBA Finals Game 7",
    summary: "The championship comes down to one final game.",
    category: "Basketball",
    rating: 5,
    startsAt: null,
    expectedWindow: "Example event",
    source: null,
    recommendedDescription: "NBA Finals Game 7",
    recommendedReminderAt: null,
    howToWatch: "ABC, ESPN+",
  },
  {
    title: "World Chess Championship, Game 6",
    summary: "A must-win game in a tied match for the world title.",
    category: "Chess",
    rating: 4,
    startsAt: null,
    expectedWindow: "Example event",
    source: null,
    recommendedDescription: "Chess Championship G6",
    recommendedReminderAt: null,
    howToWatch: "Chess.com, YouTube",
  },
  {
    title: "Dune: Part Three premiere",
    summary: "The long-anticipated finale hits theaters worldwide.",
    category: "Film",
    rating: 4,
    startsAt: null,
    expectedWindow: "Example event",
    source: null,
    recommendedDescription: "Dune 3 premiere",
    recommendedReminderAt: null,
    howToWatch: "Theaters nationwide",
  },
];

const formatWindowDate = (isoDate: string) =>
  new Date(isoDate).toLocaleDateString(undefined, {
    month: "long",
    day: "numeric",
  });

const WatchlistContent = () => {
  const { user, authLoading } = useUser();
  const [events, setEvents] = useState<WatchlistEventProps[]>([]);
  const [windowRange, setWindowRange] = useState<{ start: string; end: string } | null>(null);
  const [nextRefreshAt, setNextRefreshAt] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [failed, setFailed] = useState(false);
  const [filter, setFilter] = useState<RatingFilter>("all");

  const fetchWatchlist = useCallback(async (force = false) => {
    setFailed(false);
    try {
      const response = await apiClient.get("/watchlist", {
        params: force ? { refresh: true } : {},
      });
      setEvents(response.data?.events ?? []);
      setNextRefreshAt(response.data?.nextRefreshAt ?? null);
      if (response.data?.windowStart && response.data?.windowEnd) {
        setWindowRange({ start: response.data.windowStart, end: response.data.windowEnd });
      }
    } catch (err) {
      console.error("Error fetching watchlist:", err);
      setFailed(true);
    }
  }, []);

  useEffect(() => {
    if (!user) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    (async () => {
      await fetchWatchlist();
      if (!cancelled) setLoading(false);
    })();
    return () => {
      cancelled = true;
    };
  }, [user, fetchWatchlist]);

  const handleRefresh = async () => {
    setRefreshing(true);
    await fetchWatchlist(true);
    setRefreshing(false);
  };

  const visibleEvents = useMemo(
    () => (filter === "all" ? events : events.filter((event) => event.rating === filter)),
    [events, filter]
  );

  if (authLoading || loading) {
    return (
      <div className="watchlist-content">
        <div className="reminders-loader">
          <Loader />
        </div>
        {!authLoading && (
          <p className="watchlist-loading-note">
            We&apos;re rounding up the most interesting things to watch over
            the next two weeks &mdash; hang tight, this takes a moment.
          </p>
        )}
      </div>
    );
  }

  if (!user) {
    return (
      <div className="watchlist-content">
        <div className="content-title">Planning Ahead</div>
        <SignInPrompt
          title="Sign in to unlock Planning Ahead"
          description="Planning Ahead is a rolling look at the biggest things worth setting aside time for over the next two weeks - across sports, film, and more. Sign in with Google and it starts learning what you're into from what you click, so the events that matter most to you rise to the top. Here's a preview:"
        >
          <div className="watchlist-list">
            {SAMPLE_EVENTS.map((event, index) => (
              <WatchlistCard key={`${event.title}-${index}`} event={event} />
            ))}
          </div>
        </SignInPrompt>
      </div>
    );
  }

  return (
    <div className="watchlist-content">
      <div className="content-title">Planning Ahead</div>
      {windowRange && (
        <p className="watchlist-subtitle">
          {formatWindowDate(windowRange.start)} – {formatWindowDate(windowRange.end)}
        </p>
      )}

      {!failed && events.length > 0 && (
        <RefreshControl
          nextRefreshAt={nextRefreshAt}
          refreshing={refreshing}
          onRefresh={handleRefresh}
        />
      )}

      {!failed && events.length > 0 && (
        <RatingFilterBar value={filter} onChange={setFilter} />
      )}

      {failed ? (
        <p className="watchlist-empty">
          The watchlist couldn&apos;t be loaded. Try again shortly.
        </p>
      ) : events.length === 0 ? (
        <p className="watchlist-empty">
          Nothing on the watchlist yet. Check back a little later.
        </p>
      ) : visibleEvents.length === 0 ? (
        <p className="watchlist-empty">Nothing at this rating right now.</p>
      ) : (
        <div className="watchlist-list">
          {visibleEvents.map((event, index) => (
            <WatchlistCard key={`${event.title}-${index}`} event={event} />
          ))}
        </div>
      )}
    </div>
  );
};

export default WatchlistContent;
