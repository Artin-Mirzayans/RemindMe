import React from "react";
import { useNavigate } from "react-router-dom";
import { MdAlarmAdd, MdOpenInNew } from "react-icons/md";
import { WatchlistEventProps } from "../../props/WatchlistProps";
import UTCToZoned from "../Reminder/List/UTCtoZoned";
import firstFuture from "../Reminder/reminderTime";
import apiClient from "../Auth/apiClient";
import StarRating from "../common/StarRating";

const MAX_DESCRIPTION = 40;

interface WatchlistCardProps {
  event: WatchlistEventProps;
}

const logInterest = (category: string) => {
  apiClient.post("/watchlist/interest", { category }).catch(() => {});
};

const WatchlistCard: React.FC<WatchlistCardProps> = ({ event }) => {
  const navigate = useNavigate();

  const handleRemindMe = () => {
    logInterest(event.category);
    navigate("/reminders", {
      state: {
        prefill: {
          description: (event.recommendedDescription || event.title).slice(0, MAX_DESCRIPTION),
          dateTime: firstFuture(event.recommendedReminderAt, event.startsAt),
        },
      },
    });
  };

  const dateLabel = event.startsAt
    ? (() => {
        const { formattedDate, formattedTime } = UTCToZoned(event.startsAt);
        return `${formattedDate}, ${formattedTime}`;
      })()
    : event.expectedWindow ?? "Date TBD";

  return (
    <article className="watchlist-card">
      <div className="watchlist-card-head">
        <StarRating rating={event.rating} />
        <span className="watchlist-card-category">{event.category}</span>
        {event.source && (
          <a
            className="watchlist-card-source"
            href={event.source}
            target="_blank"
            rel="noopener noreferrer"
            aria-label={`Read more about ${event.title}`}
            onClick={() => logInterest(event.category)}
          >
            <MdOpenInNew />
          </a>
        )}
      </div>

      <h3 className="watchlist-card-title">{event.title}</h3>
      <p className="watchlist-card-date">{dateLabel}</p>
      <p className="watchlist-card-summary">{event.summary}</p>
      {event.howToWatch && (
        <p className="watchlist-card-howto">
          <strong>How to watch:</strong> {event.howToWatch}
        </p>
      )}

      <button
        type="button"
        className="btn btn--compact"
        onClick={handleRemindMe}
      >
        <MdAlarmAdd size={18} />
        Remind me
      </button>
    </article>
  );
};

export default WatchlistCard;
