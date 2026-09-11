import React from "react";
import { useNavigate } from "react-router-dom";
import { MdAlarmAdd, MdConfirmationNumber } from "react-icons/md";
import { LocalEventProps } from "../../props/LocalEventsProps";
import UTCToZoned from "../Reminder/List/UTCtoZoned";
import firstFuture from "../Reminder/reminderTime";
import StarRating from "../common/StarRating";

const MAX_DESCRIPTION = 40;

interface NearbyCardProps {
  event: LocalEventProps;
}

const NearbyCard: React.FC<NearbyCardProps> = ({ event }) => {
  const navigate = useNavigate();
  const { formattedDate, formattedTime } = UTCToZoned(event.startsAt);

  const handleRemindMe = () => {
    navigate("/reminders", {
      state: {
        prefill: {
          description: (event.recommendedDescription || event.title).slice(0, MAX_DESCRIPTION),
          dateTime: firstFuture(event.recommendedReminderAt, event.startsAt),
        },
      },
    });
  };

  return (
    <article className="nearby-card">
      <div className="nearby-card-head">
        <StarRating rating={event.rating} />
        <span className="nearby-card-category">{event.category}</span>
      </div>

      <h3 className="nearby-card-title">{event.title}</h3>
      <p className="nearby-card-meta">
        {formattedDate}, {formattedTime} &middot; {event.venue}
        {event.priceFrom ? ` · ${event.priceFrom}` : ""}
      </p>
      <p className="nearby-card-summary">{event.summary}</p>

      <div className="nearby-card-actions">
        <button type="button" className="btn btn--compact" onClick={handleRemindMe}>
          <MdAlarmAdd size={18} />
          Remind me
        </button>
        {event.ticketUrl && (
          <a
            className="btn btn--compact btn--ghost"
            href={event.ticketUrl}
            target="_blank"
            rel="noopener noreferrer"
          >
            <MdConfirmationNumber size={18} />
            Tickets
          </a>
        )}
      </div>
    </article>
  );
};

export default NearbyCard;
