export function timeFormat(ms) {
  let seconds = Math.floor(ms / 1000);
  let minutes = Math.floor(seconds / 60);
  let hours = Math.floor(minutes / 60);
  let days = Math.floor(hours / 24);
  seconds = seconds % 60;
  minutes = minutes % 60;
  hours = hours % 24;

  if (!seconds || (seconds && !minutes)) {
    // less than a minute; use only seconds
    return seconds + "s";
  } else if (minutes && !hours) {
    // less than an hour; use minutes and seconds
    return minutes + "m " + seconds + "s";
  } else if (hours && !days) {
    // less than a day; use hours, minutes, and seconds
    return hours + "h " + minutes + "m " + seconds + "s";
  } else {
    // use days, hours, minutes, and seconds
    return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
  }
}

export function round(n) {
  return n % 1 ? Math.round(n * 100) / 100 : n;
}

export function percent(n) {
  return Math.round(n * 100);
}

export function milliToSeconds(ms) {
  return ms / 1000;
}

export function milliToMinutes(ms) {
  return ms / 1000 / 60;
}

export function milliToHours(ms) {
  return ms / 1000 / 60 / 60;
}

export function minutesToMillis(minutes) {
  return minutes * 60 * 1000;
}
