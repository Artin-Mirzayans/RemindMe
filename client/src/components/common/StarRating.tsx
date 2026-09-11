import React from "react";
import { MdStar, MdStarOutline } from "react-icons/md";

import "./StarRating.css";

const MAX_RATING = 5;

// five-star rating shared by every discovery card
const StarRating: React.FC<{ rating: number }> = ({ rating }) => (
  <span className="star-rating" aria-label={`${rating} out of ${MAX_RATING} stars`}>
    {Array.from({ length: MAX_RATING }, (_, i) =>
      i < rating ? <MdStar key={i} /> : <MdStarOutline key={i} />
    )}
  </span>
);

export default StarRating;
