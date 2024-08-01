import { format, toZonedTime } from 'date-fns-tz';

const UTCToZoned = (date) => {
    const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;

    const zonedDate = toZonedTime(date, timeZone);

    const formattedDate = format(zonedDate, 'MM/dd');

    const formattedTime = format(zonedDate, 'h:mm a');

    return { formattedDate, formattedTime };
};

export default UTCToZoned