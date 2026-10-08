import React, { useEffect, useMemo, useState } from "react";
import apiClient from "../Auth/apiClient";
import Loader from "../Loader/Loader";
import DigestCard from "./DigestCard";
import SignInPrompt from "../Auth/SignInPrompt";
import RatingFilterBar, { RatingFilter } from "../common/RatingFilterBar";
import { useUser } from "../Auth/UserContext";
import { DigestEventProps } from "../../props/DigestProps";

import "./DigestContent.css";

const DigestContent = () => {
  const { user, authLoading } = useUser();
  const [events, setEvents] = useState<DigestEventProps[]>([]);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const [filter, setFilter] = useState<RatingFilter>("all");

  useEffect(() => {
    const fetchDigest = async () => {
      try {
        const response = await apiClient.get("/digest");
        setEvents(response.data?.events ?? []);
      } catch (err) {
        console.error("Error fetching digest:", err);
        setFailed(true);
      } finally {
        setLoading(false);
      }
    };

    fetchDigest();
  }, []);

  const formatDay = (date: Date) =>
    date.toLocaleDateString(undefined, {
      weekday: "long",
      month: "long",
      day: "numeric",
    });

  const today = formatDay(new Date());
  const tomorrow = formatDay(new Date(Date.now() + 24 * 60 * 60 * 1000));

  const visibleEvents = useMemo(() => {
    const byTime = [...events].sort(
      (a, b) => new Date(a.startsAt).getTime() - new Date(b.startsAt).getTime()
    );
    return filter === "all" ? byTime : byTime.filter((event) => event.rating === filter);
  }, [events, filter]);

  return (
    <div className="digest-content">
      <div className="content-title">Today &amp; Tomorrow</div>
      <p className="digest-subtitle">
        {today} &ndash; {tomorrow}
      </p>

      {!authLoading && !user && (
        <>
          <p className="digest-tagline">
            The games, premieres and big events worth watching live &mdash;
            the biggest things happening today and tomorrow.
          </p>

          <SignInPrompt
            compact
            title="Get more from RemindMe."
            description="Sign in with Google to create your own reminders and unlock a personalized Planning Ahead."
          />
        </>
      )}

      {loading ? (
        <>
          <div className="reminders-loader">
            <Loader />
          </div>
          <p className="digest-loading-note">
            Finding what&apos;s worth watching today and tomorrow &mdash; one
            moment.
          </p>
        </>
      ) : failed ? (
        <p className="digest-empty">
          Couldn&apos;t load today&apos;s picks right now. Try again shortly.
        </p>
      ) : events.length === 0 ? (
        <p className="digest-empty">
          Nothing here yet. Check back a little later.
        </p>
      ) : (
        <>
          <RatingFilterBar value={filter} onChange={setFilter} />
          {visibleEvents.length === 0 ? (
            <p className="digest-empty">Nothing at this rating right now.</p>
          ) : (
            <div className="digest-list">
              {visibleEvents.map((event, index) => (
                <DigestCard key={`${event.title}-${index}`} event={event} />
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default DigestContent;
