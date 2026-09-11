import { format, toZonedTime } from 'date-fns-tz';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';

dayjs.extend(relativeTime);

const UTCToZoned = (date: string) => {
    const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;

    const zonedDate = toZonedTime(date, timeZone);

    const formattedDate = format(zonedDate, 'MMM d');

    const formattedTime = format(zonedDate, 'h:mm a');

    return { formattedDate, formattedTime, relative: dayjs(date).fromNow() };
};

export default UTCToZoned
