import dayjs from "@/plugins/dayjs";

/**
 * Milliseconds since the epoch for a date from the API, which arrives as ISO text (Jackson 3's
 * default) or, from older code paths, as a number. NaN when there's no date.
 */
export const toTimestamp = value => {
  if (value == null || value === "") {
    return Number.NaN;
  }

  const date = dayjs(value);

  return date.isValid() ? date.valueOf() : Number.NaN;
};

// oldest first; a submission not yet submitted sorts as the oldest, as before
export const byDateSubmitted = (a, b) => (toTimestamp(a?.dateSubmitted) || 0) - (toTimestamp(b?.dateSubmitted) || 0);
