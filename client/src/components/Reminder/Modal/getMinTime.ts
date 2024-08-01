const getMinTime = (date: Date | null): Date => {
    if (!date || date.toDateString() === new Date().toDateString()) {
        return new Date(new Date().setSeconds(0, 0));
    }
    return new Date(new Date().setHours(0, 0, 0, 0));
};

export default getMinTime