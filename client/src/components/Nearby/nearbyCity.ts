import { useEffect, useState } from "react";

// the Nearby tab shows the visitor's actual city once it's resolved. the nav renders before
// NearbyContent has fetched, so the city gets stashed in localStorage and broadcast on a window
// event - the nav picks it up through useNearbyCity() and falls back to "Nearby" until then
export const NEARBY_CITY_KEY = "nearby_city";
export const NEARBY_CITY_EVENT = "nearby:city";
const FALLBACK = "Nearby";

export const readNearbyCity = (): string => {
  try {
    return localStorage.getItem(NEARBY_CITY_KEY) || FALLBACK;
  } catch {
    return FALLBACK;
  }
};

// stores just the city portion of a "City, Region" label and lets the nav know it changed
export const rememberNearbyCity = (label: string | null | undefined) => {
  const city = (label ?? "").split(",")[0].trim();
  if (!city) return;
  try {
    localStorage.setItem(NEARBY_CITY_KEY, city);
  } catch {
    /* ignore */
  }
  window.dispatchEvent(new CustomEvent(NEARBY_CITY_EVENT, { detail: city }));
};

export const useNearbyCity = (): string => {
  const [city, setCity] = useState<string>(readNearbyCity);

  useEffect(() => {
    const update = () => setCity(readNearbyCity());
    window.addEventListener(NEARBY_CITY_EVENT, update);
    window.addEventListener("storage", update);
    return () => {
      window.removeEventListener(NEARBY_CITY_EVENT, update);
      window.removeEventListener("storage", update);
    };
  }, []);

  return city;
};
