import React from "react";
import { useNavigate } from "react-router-dom";
import { MdAlarmAdd, MdOpenInNew } from "react-icons/md";
import { DigestEventProps } from "../../props/DigestProps";
import UTCToZoned from "../Reminder/List/UTCtoZoned";
import firstFuture from "../Reminder/reminderTime";
import StarRating from "../common/StarRating";

const MAX_DESCRIPTION = 40;

interface DigestCardProps {
  event: DigestEventProps;
}

const DigestCard: React.FC<DigestCardProps> = ({ event }) => {
  const navigate = useNavigate();

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

  const { formattedDate, formattedTime } = UTCToZoned(event.startsAt);

  return (
    <article className="digest-card">
      <div className="digest-card-head">
        <StarRating rating={event.rating} />
        <span className="digest-card-category">{event.category}</span>
        {event.source && (
          <a
            className="digest-card-source"
            href={event.source}
            target="_blank"
            rel="noopener noreferrer"
            aria-label={`Read more about ${event.title}`}
          >
            <MdOpenInNew />
          </a>
        )}
      </div>

      <h3 className="digest-card-title">{event.title}</h3>
      <p className="digest-card-when">
        {formattedDate}, {formattedTime}
      </p>
      <p className="digest-card-summary">{event.summary}</p>
      {event.howToWatch && (
        <p className="digest-card-howto">
          <strong>Watch:</strong> {event.howToWatch}
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

export default DigestCard;
