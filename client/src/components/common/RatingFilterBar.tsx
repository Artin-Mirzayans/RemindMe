import React from "react";

import "./RatingFilterBar.css";

export type RatingFilter = "all" | 5 | 4 | 3;

const FILTERS: { key: RatingFilter; stars: string | null }[] = [
  { key: "all", stars: null },
  { key: 5, stars: "★★★★★" },
  { key: 4, stars: "★★★★" },
  { key: 3, stars: "★★★" },
];

interface Props {
  value: RatingFilter;
  onChange: (value: RatingFilter) => void;
}

// the All / 5 / 4 / 3 star filter used on every discovery tab
const RatingFilterBar: React.FC<Props> = ({ value, onChange }) => (
  <div className="rating-filter-bar">
    {FILTERS.map(({ key, stars }) => (
      <button
        key={key}
        type="button"
        className={`rating-filter-btn${value === key ? " active" : ""}`}
        onClick={() => onChange(key)}
      >
        {stars ? <span className="rating-filter-stars">{stars}</span> : "All"}
      </button>
    ))}
  </div>
);

export default RatingFilterBar;
