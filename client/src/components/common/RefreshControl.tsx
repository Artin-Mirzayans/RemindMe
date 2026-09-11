import React from "react";
import { MdRefresh } from "react-icons/md";

import "./RefreshControl.css";

interface Props {
  // yyyy-MM-dd the feed becomes eligible to refresh again, or null when nothing is cached yet
  nextRefreshAt: string | null;
  refreshing: boolean;
  onRefresh: () => void;
}

// shows a "Refresh" button once the cooldown has elapsed, otherwise a note on when it'll be back
const RefreshControl: React.FC<Props> = ({ nextRefreshAt, refreshing, onRefresh }) => {
  if (!nextRefreshAt) return null;

  const startOfToday = new Date();
  startOfToday.setHours(0, 0, 0, 0);
  const eligibleOn = new Date(`${nextRefreshAt}T00:00:00`);
  const eligible = startOfToday.getTime() >= eligibleOn.getTime();

  return (
    <div className="refresh-control">
      {eligible ? (
        <button
          type="button"
          className="btn btn--compact btn--ghost"
          onClick={onRefresh}
          disabled={refreshing}
        >
          <MdRefresh size={16} />
          {refreshing ? "Refreshing…" : "Refresh"}
        </button>
      ) : (
        <span className="refresh-control-note">
          Next refresh available{" "}
          {eligibleOn.toLocaleDateString(undefined, { month: "long", day: "numeric" })}
        </span>
      )}
    </div>
  );
};

export default RefreshControl;
