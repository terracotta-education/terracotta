import dayjs from "@/plugins/dayjs";

// the server's locked-assignment messages (TextConstants.ASSIGNMENT_LOCKED_AT/_UNTIL) carry the
// date as an ISO-8601 instant, shown here in the student's own time zone
const LOCKED_AT = /^Assignment was locked at (\S+)/;
const LOCKED_UNTIL = /^Assignment is locked until (\S+)/;

const formatDate = iso => dayjs(iso).format("MMMM D [at] h:mm A");

/**
 * A readable version of the server's message when an assignment can't be opened because of its
 * dates, or null when the message isn't one of those.
 */
export function lockedAssignmentMessage(message) {
  const text = typeof message === "string" ? message : "";
  const lockedAt = text.match(LOCKED_AT);

  if (lockedAt && dayjs(lockedAt[1]).isValid()) {
    return `This assignment was locked on ${formatDate(lockedAt[1])}.`;
  }

  const lockedUntil = text.match(LOCKED_UNTIL);

  if (lockedUntil && dayjs(lockedUntil[1]).isValid()) {
    return `This assignment is locked until ${formatDate(lockedUntil[1])}.`;
  }

  return null;
}
